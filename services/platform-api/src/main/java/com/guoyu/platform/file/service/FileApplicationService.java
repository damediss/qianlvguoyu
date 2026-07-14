package com.guoyu.platform.file.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.FileStorage;
import com.guoyu.platform.file.TencentCosFileStorage;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.file.support.FileLocalPathResolver;
import com.guoyu.platform.file.support.StoredFileFormatValidator;
import com.guoyu.platform.file.vo.DownloadUrlVO;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class FileApplicationService {

  /** upload 的 hash + url 一起返回,避免在调用方拼接字符串。 */
  private record UploadResult(String sha256, String url) {}

  private final FileMapper fileMapper;
  private final FileStorageProperties properties;
  private final FileStorage storage;
  private final FileLocalPathResolver localPathResolver;

  public FileApplicationService(
      FileMapper fileMapper,
      FileStorageProperties properties,
      FileStorage storage,
      FileLocalPathResolver localPathResolver) {
    this.fileMapper = fileMapper;
    this.properties = properties;
    this.storage = storage;
    this.localPathResolver = localPathResolver;
  }

  @Transactional
  public FileVO storeFromBytes(byte[] content, String originalName, String contentType, String accessPolicy) {
    if (content == null || content.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
    }

    Long tenantId = getTenantIdSafe();
    String safeName = sanitizeFileName(originalName);
    StoredFileFormatValidator.assertSupportedOfficeFile(safeName, content);
    String objectKey = buildObjectKey(tenantId, safeName);
    try {
      String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
      String url;
      try (InputStream in = new java.io.ByteArrayInputStream(content)) {
        url = storage.put(objectKey, in, content.length, contentType);
      }

      FileEntity entity = newFileEntity(tenantId, objectKey, safeName, contentType, content.length, sha256, accessPolicy);
      fileMapper.insert(entity);
      return toVO(entity, url);
    } catch (IOException | NoSuchAlgorithmException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File store failed");
    }
  }

  @Transactional
  public FileVO upload(MultipartFile file, String accessPolicy) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
    }

    Long tenantId = getTenantIdSafe();
    String safeName = sanitizeFileName(file.getOriginalFilename());
    String objectKey = buildObjectKey(tenantId, safeName);
    try {
      UploadResult r = copyAndUpload(file, objectKey);

      FileEntity entity = newFileEntity(tenantId, objectKey, safeName,
          file.getContentType(), file.getSize(), r.sha256(), accessPolicy);
      fileMapper.insert(entity);
      return toVO(entity, r.url());
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File upload failed");
    }
  }

  /**
   * MultipartFile -> FileStorage,带 SHA-256 摘要。
   * 临时落本地小文件算 hash 再上传(纯流式可改用 COS TransferManager,简化起见用临时文件)。
   */
  private UploadResult copyAndUpload(MultipartFile file, String objectKey) throws IOException {
    Path tmp = Files.createTempFile("guoyu-upload-", ".bin");
    try {
      MessageDigest digest;
      try {
        digest = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException ex) {
        throw new IllegalStateException("SHA-256 digest unavailable", ex);
      }
      try (InputStream input = file.getInputStream();
          DigestInputStream digestInput = new DigestInputStream(input, digest)) {
        Files.copy(digestInput, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      StoredFileFormatValidator.assertSupportedOfficeFile(sanitizeFileName(file.getOriginalFilename()), tmp);
      String sha256 = HexFormat.of().formatHex(digest.digest());
      String url;
      try (InputStream up = Files.newInputStream(tmp)) {
        url = storage.put(objectKey, up, Files.size(tmp), file.getContentType());
      }
      return new UploadResult(sha256, url);
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  private FileEntity newFileEntity(Long tenantId, String objectKey, String safeName,
                                    String contentType, long size, String sha256, String accessPolicy) {
    FileEntity entity = new FileEntity();
    entity.setTenantId(tenantId);
    entity.setStorageProvider(storage.providerName());
    entity.setObjectKey(objectKey);
    entity.setOriginalName(safeName);
    entity.setContentType(StringUtils.hasText(contentType) ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
    entity.setSizeBytes(size);
    entity.setSha256(sha256);
    entity.setAccessPolicy(StringUtils.hasText(accessPolicy) ? accessPolicy : "private");
    entity.setStatus("uploaded");
    entity.setDeleted(0);
    return entity;
  }

  @Transactional(readOnly = true)
  public FileVO get(Long id) {
    FileEntity entity = getExistingFile(SecurityContext.currentUser().getTenantId(), id);
    return toVO(entity, publicUrl(entity));
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

  /**
   * 按文件 ID 读取元数据（不做当前用户租户校验）。
   * 供已通过业务权限校验的内部服务调用（如三年规划附件下载）。
   * NOT_SUPPORTED：避免调用方 catch 异常后回退逻辑触发 UnexpectedRollbackException。
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
  public FileVO getById(Long id) {
    FileEntity entity = findUploadedFile(id);
    return toVO(entity, publicUrl(entity));
  }

  /**
   * 按文件 ID 读取字节（不做当前用户租户校验，支持 local / COS）。
   * NOT_SUPPORTED：避免调用方 catch 异常后回退逻辑触发 UnexpectedRollbackException。
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
  public byte[] readFileBytesById(Long id) {
    FileEntity entity = findUploadedFile(id);
    try (InputStream in = openInputStream(entity)) {
      return in.readAllBytes();
    } catch (IOException ex) {
      log.warn("读取文件失败: id={}, objectKey={}, provider={}", id, entity.getObjectKey(),
          entity.getStorageProvider(), ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取文件失败: " + ex.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> buildDownloadResponse(Long id, boolean inline) {
    FileEntity entity = getExistingFile(SecurityContext.currentUser().getTenantId(), id);
    Resource resource = loadResource(entity);
    return buildDownloadResponse(entity, resource, inline);
  }

  private Resource loadResource(FileEntity entity) {
    if ("cos".equalsIgnoreCase(entity.getStorageProvider())) {
      // COS 直连:前端用 FileVO.url 跳 CDN,后端不再流式转发(避免流量走两次)。
      // 真要后端转发的话,FileController 调 TencentCosFileStorage.getObject → InputStreamResource。
      throw new BusinessException(ErrorCode.NOT_IMPLEMENTED,
          "COS 文件下载请使用前端 FileVO.url 直连 CDN");
    }
    Path path = localPathResolver.resolveExisting(entity.getObjectKey())
        .orElseGet(() -> localPathResolver.resolveConfigured(entity.getObjectKey()));
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
    storage.delete(entity.getObjectKey());
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

  private FileEntity findUploadedFile(Long id) {
    FileEntity entity = fileMapper.selectOne(new LambdaQueryWrapper<FileEntity>()
        .eq(FileEntity::getId, id)
        .eq(FileEntity::getDeleted, 0)
        .eq(FileEntity::getStatus, "uploaded"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "File not found");
    }
    return entity;
  }

  private InputStream openInputStream(FileEntity entity) throws IOException {
    if ("cos".equalsIgnoreCase(entity.getStorageProvider())) {
      return openCosInputStream(entity);
    }
    Optional<Path> existing = localPathResolver.resolveExisting(entity.getObjectKey());
    if (existing.isPresent()) {
      return Files.newInputStream(existing.get());
    }
    InputStream cosFallback = tryOpenCosInputStream(entity);
    if (cosFallback != null) {
      log.info("本地文件缺失，已从 COS 回退读取: id={}, objectKey={}", entity.getId(), entity.getObjectKey());
      return cosFallback;
    }
    Path configured = localPathResolver.resolveConfigured(entity.getObjectKey());
    log.warn("File content missing on disk: id={}, objectKey={}, configuredPath={}",
        entity.getId(), entity.getObjectKey(), configured);
    throw new BusinessException(ErrorCode.NOT_FOUND, "File content not found");
  }

  private InputStream openCosInputStream(FileEntity entity) {
    InputStream in = tryOpenCosInputStream(entity);
    if (in != null) {
      return in;
    }
    throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "COS 存储未配置，无法读取文件");
  }

  private InputStream tryOpenCosInputStream(FileEntity entity) {
    if (!(storage instanceof TencentCosFileStorage cosStorage)) {
      return null;
    }
    try {
      return cosStorage.getObject(entity.getObjectKey()).getObjectContent();
    } catch (Exception ex) {
      log.warn("COS 读取失败: id={}, objectKey={}", entity.getId(), entity.getObjectKey(), ex);
      if ("cos".equalsIgnoreCase(entity.getStorageProvider())) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "COS 读取失败: " + ex.getMessage());
      }
      return null;
    }
  }

  public Optional<Path> localPath(FileEntity entity) {
    if (!"local".equalsIgnoreCase(entity.getStorageProvider())) {
      return Optional.empty();
    }
    return localPathResolver.resolveExisting(entity.getObjectKey());
  }

  private String publicUrl(FileEntity entity) {
    if ("cos".equalsIgnoreCase(entity.getStorageProvider())) {
      return cosPublicUrl(entity.getObjectKey());
    }
    return localPublicUrl(entity.getObjectKey());
  }

  private String localPublicUrl(String objectKey) {
    return "/api/exam/files/" + objectKey;
  }

  private String cosPublicUrl(String objectKey) {
    FileStorageProperties.Cos c = properties.cos();
    if (c == null) return "";
    String prefix = StringUtils.hasText(c.urlPrefix())
        ? c.urlPrefix()
        : String.format("https://%s.cos.%s.myqcloud.com", c.bucket(), c.region());
    if (!StringUtils.hasText(prefix)) return "";
    String p = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    return p + "/" + objectKey;
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

  private Long getTenantIdSafe() {
    try {
      return SecurityContext.currentUser().getTenantId();
    } catch (IllegalStateException ex) {
      return 0L;
    }
  }

  private FileVO toVO(FileEntity entity, String url) {
    return new FileVO(
        entity.getId(),
        entity.getOriginalName(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getAccessPolicy(),
        entity.getStatus(),
        entity.getCreatedAt(),
        url
    );
  }
}
