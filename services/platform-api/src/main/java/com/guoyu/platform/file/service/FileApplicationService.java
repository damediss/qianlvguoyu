package com.guoyu.platform.file.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.file.vo.DownloadUrlVO;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class FileApplicationService {

  private final FileMapper fileMapper;
  private final FileStorageProperties properties;

  public FileApplicationService(FileMapper fileMapper, FileStorageProperties properties) {
    this.fileMapper = fileMapper;
    this.properties = properties;
  }

  @Transactional
  public FileVO upload(MultipartFile file, String accessPolicy) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
    }
    if (!"local".equalsIgnoreCase(storageProvider())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Only local file storage is available in current MVP");
    }

    Long tenantId = SecurityContext.currentUser().getTenantId();
    String safeName = sanitizeFileName(file.getOriginalFilename());
    String objectKey = buildObjectKey(tenantId, safeName);
    Path target = resolveLocalPath(objectKey);
    try {
      Files.createDirectories(target.getParent());
      String sha256 = copyAndHash(file, target);

      FileEntity entity = new FileEntity();
      entity.setTenantId(tenantId);
      entity.setStorageProvider(storageProvider());
      entity.setObjectKey(objectKey);
      entity.setOriginalName(safeName);
      entity.setContentType(file.getContentType());
      entity.setSizeBytes(file.getSize());
      entity.setSha256(sha256);
      entity.setAccessPolicy(StringUtils.hasText(accessPolicy) ? accessPolicy : "private");
      entity.setStatus("uploaded");
      entity.setDeleted(0);
      fileMapper.insert(entity);
      return toVO(entity);
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File upload failed");
    }
  }

  @Transactional(readOnly = true)
  public FileVO get(Long id) {
    return toVO(getExistingFile(SecurityContext.currentUser().getTenantId(), id));
  }

  @Transactional(readOnly = true)
  public DownloadUrlVO downloadUrl(Long id) {
    FileEntity entity = getExistingFile(SecurityContext.currentUser().getTenantId(), id);
    return new DownloadUrlVO(entity.getId(), "/api/files/" + entity.getId() + "/download", entity.getAccessPolicy());
  }

  @Transactional(readOnly = true)
  public Resource downloadResource(Long id) {
    FileEntity entity = getExistingFile(SecurityContext.currentUser().getTenantId(), id);
    return loadResource(entity);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> buildDownloadResponse(Long id, boolean inline) {
    FileEntity entity = getExistingFile(SecurityContext.currentUser().getTenantId(), id);
    Resource resource = loadResource(entity);
    return buildDownloadResponse(entity, resource, inline);
  }

  private Resource loadResource(FileEntity entity) {
    Path path = localPath(entity)
        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "File is not available from local storage"));
    try {
      Resource resource = new UrlResource(path.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        log.warn("File content missing on disk: id={}, objectKey={}, path={}", entity.getId(), entity.getObjectKey(), path);
        throw new BusinessException(ErrorCode.NOT_FOUND, "File content not found");
      }
      return resource;
    } catch (BusinessException ex) {
      throw ex;
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File download failed");
    }
  }

  private ResponseEntity<Resource> buildDownloadResponse(FileEntity entity, Resource resource, boolean inline) {
    String contentType = StringUtils.hasText(entity.getContentType())
        ? entity.getContentType()
        : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    String dispositionType = inline ? "inline" : "attachment";
    String encodedName = URLEncoder.encode(entity.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename*=UTF-8''" + encodedName)
        .contentType(MediaType.parseMediaType(contentType))
        .body(resource);
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    FileEntity entity = getExistingFile(tenantId, id);
    entity.setStatus("deleted");
    fileMapper.updateById(entity);
  }

  @Transactional(readOnly = true)
  public FileEntity getExistingFile(Long tenantId, Long id) {
    FileEntity entity = fileMapper.selectOne(new LambdaQueryWrapper<FileEntity>()
        .eq(FileEntity::getTenantId, tenantId)
        .eq(FileEntity::getId, id)
        .eq(FileEntity::getDeleted, 0)
        .eq(FileEntity::getStatus, "uploaded"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "File not found");
    }
    return entity;
  }

  public Optional<Path> localPath(FileEntity entity) {
    if (!"local".equalsIgnoreCase(entity.getStorageProvider())) {
      return Optional.empty();
    }
    return Optional.of(resolveLocalPath(entity.getObjectKey()));
  }

  private String storageProvider() {
    return StringUtils.hasText(properties.storageProvider()) ? properties.storageProvider() : "local";
  }

  private Path resolveLocalPath(String objectKey) {
    Path root = Paths.get(properties.localRoot()).toAbsolutePath().normalize();
    Path resolved = root.resolve(objectKey).normalize();
    if (!resolved.startsWith(root)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid file object key");
    }
    return resolved;
  }

  private String buildObjectKey(Long tenantId, String fileName) {
    LocalDate today = LocalDate.now();
    return tenantId + "/" + today.getYear() + "/" + "%02d".formatted(today.getMonthValue())
        + "/" + UUID.randomUUID() + "-" + fileName;
  }

  private String sanitizeFileName(String originalName) {
    String name = StringUtils.hasText(originalName) ? originalName : "upload.bin";
    return name.replace("\\", "_").replace("/", "_").replace(":", "_");
  }

  private String copyAndHash(MultipartFile file, Path target) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream input = file.getInputStream();
          DigestInputStream digestInput = new DigestInputStream(input, digest)) {
        Files.copy(digestInput, target);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 digest unavailable", ex);
    }
  }

  private FileVO toVO(FileEntity entity) {
    return new FileVO(
        entity.getId(),
        entity.getOriginalName(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getAccessPolicy(),
        entity.getStatus(),
        entity.getCreatedAt()
    );
  }
}
