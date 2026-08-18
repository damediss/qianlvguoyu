package com.guoyu.platform.file.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.FileStorage;
import com.guoyu.platform.file.TencentCosFileStorage;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.qcloud.cos.model.COSObject;
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
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    return storeFromBytesForTenant(getTenantIdSafe(), content, originalName, contentType, accessPolicy);
  }

  /**
   * Stores generated/imported content for an explicit tenant. Callers running in worker threads
   * must pass the tenant captured from the request thread instead of using SecurityContext.
   */
  @Transactional
  public FileVO storeFromBytesForTenant(
      Long tenantId, byte[] content, String originalName, String contentType, String accessPolicy) {
    if (content == null || content.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
    }

    if (tenantId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Tenant is required");
    }
    String safeName = sanitizeFileName(originalName);
    StoredFileFormatValidator.assertSupportedOfficeFile(safeName, content);
    StoredFileFormatValidator.assertSupportedImageFile(safeName, contentType, content);
    String objectKey = buildObjectKey(tenantId, safeName);
    try {
      String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
      String url;
      try (InputStream in = new java.io.ByteArrayInputStream(content)) {
        url = storage.put(objectKey, in, content.length, contentType);
      }

      FileEntity entity = newFileEntity(tenantId, objectKey, safeName, contentType, content.length, sha256, accessPolicy);
      try {
        fileMapper.insert(entity);
      } catch (RuntimeException ex) {
        storage.delete(objectKey);
        throw ex;
      }
      registerRollbackCleanup(objectKey);
      return toVO(entity, url);
    } catch (IOException | NoSuchAlgorithmException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File store failed");
    }
  }

  @Transactional
  public FileVO upload(MultipartFile file, String accessPolicy) {
    return uploadForTenant(getTenantIdSafe(), file, accessPolicy);
  }

  @Transactional
  public FileVO uploadForTenant(Long tenantId, MultipartFile file, String accessPolicy) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
    }

    if (tenantId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Tenant is required");
    }
    String safeName = sanitizeFileName(file.getOriginalFilename());
    String objectKey = buildObjectKey(tenantId, safeName);
    UploadResult uploadResult = null;
    try {
      uploadResult = copyAndUpload(file, objectKey);

      FileEntity entity = newFileEntity(tenantId, objectKey, safeName,
          file.getContentType(), file.getSize(), uploadResult.sha256(), accessPolicy);
      fileMapper.insert(entity);
      registerRollbackCleanup(objectKey);
      return toVO(entity, uploadResult.url());
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File upload failed");
    } catch (RuntimeException ex) {
      if (uploadResult != null) {
        storage.delete(objectKey);
      }
      throw ex;
    }
  }

  private void registerRollbackCleanup(String objectKey) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
          storage.delete(objectKey);
        }
      }
    });
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
  public byte[] readBytes(Long id) {
    FileEntity entity = getExistingFile(SecurityContext.currentUser().getTenantId(), id);
    return readBytes(entity);
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

  /**
   * Opens an uploaded file for an internal worker without relying on SecurityContext.
   * The caller owns and must close the returned stream.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
  public InputStream openFileInputStreamById(Long id) {
    FileEntity entity = findUploadedFile(id);
    try {
      return openInputStream(entity);
    } catch (IOException ex) {
      log.warn("打开文件流失败: id={}, objectKey={}, provider={}", id, entity.getObjectKey(),
          entity.getStorageProvider(), ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取文件失败: " + ex.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> buildDownloadResponse(Long id, boolean inline) {
    FileEntity entity = getExistingFile(SecurityContext.currentUser().getTenantId(), id);
    return buildDownloadResponse(entity, loadResource(entity), inline);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> buildDownloadResponse(
      Long tenantId, Long id, boolean inline) {
    FileEntity entity = getExistingFile(tenantId, id);
    return buildDownloadResponse(entity, loadResource(entity), inline);
  }

  private ResponseEntity<Resource> buildDownloadResponse(FileEntity entity, Resource resource, boolean inline) {
    String contentType = StringUtils.hasText(entity.getContentType())
        ? entity.getContentType()
        : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    String dispositionType = inline ? "inline" : "attachment";
    String encodedName = URLEncoder.encode(entity.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename*=UTF-8''" + encodedName)
        .contentType(MediaType.parseMediaType(contentType));
    if (entity.getSizeBytes() != null && entity.getSizeBytes() >= 0) {
      builder.contentLength(entity.getSizeBytes());
    }
    return builder.body(resource);
  }

  private byte[] readBytes(FileEntity entity) {
    if ("cos".equalsIgnoreCase(entity.getStorageProvider())) {
      if (!(storage instanceof TencentCosFileStorage cosStorage)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "COS storage is not configured");
      }
      try (COSObject object = cosStorage.getObject(entity.getObjectKey());
          InputStream input = object.getObjectContent()) {
        return input.readAllBytes();
      } catch (IOException ex) {
        log.warn("Failed to read COS file: id={}, objectKey={}", entity.getId(), entity.getObjectKey(), ex);
        throw new BusinessException(ErrorCode.BAD_REQUEST, "File read failed");
      }
    }
    Path path = localPath(entity)
        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "File is not available from local storage"));
    try {
      return Files.readAllBytes(path);
    } catch (IOException ex) {
      log.warn("Failed to read local file: id={}, objectKey={}, path={}", entity.getId(), entity.getObjectKey(), path, ex);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File read failed");
    }
  }

  private Resource loadResource(FileEntity entity) {
    if ("cos".equalsIgnoreCase(entity.getStorageProvider())) {
      try {
        InputStream cosIn = tryOpenCosInputStream(entity);
        if (cosIn != null) {
          return new InputStreamResource(cosIn);
        }
      } catch (BusinessException ex) {
        log.warn("COS 下载失败，尝试本地回退: id={}, objectKey={}, msg={}",
            entity.getId(), entity.getObjectKey(), ex.getMessage());
      }
      Optional<Path> local = localPathResolver.resolveExisting(entity.getObjectKey());
      if (local.isPresent()) {
        try {
          Resource resource = new UrlResource(local.get().toUri());
          if (resource.exists() && resource.isReadable()) {
            log.info("COS 不可用，已从本地回退下载: id={}, path={}", entity.getId(), local.get());
            return resource;
          }
        } catch (IOException ex) {
          log.warn("本地回退失败: id={}", entity.getId(), ex);
        }
      }
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "File download failed: COS 与本地均不可用（常见于生成材料只写本地却标记为 cos，请重新生成）");
    }
    Path path = localPathResolver.resolveExisting(entity.getObjectKey())
        .orElseGet(() -> localPathResolver.resolveConfigured(entity.getObjectKey()));
    try {
      Resource resource = new UrlResource(path.toUri());
      if (resource.exists() && resource.isReadable()) {
        return resource;
      }
    } catch (IOException ex) {
      log.warn("本地文件读取异常: id={}, path={}", entity.getId(), path, ex);
    }
    InputStream cosFallback = tryOpenCosInputStream(entity);
    if (cosFallback != null) {
      log.info("本地文件缺失，已从 COS 回退下载: id={}, objectKey={}", entity.getId(), entity.getObjectKey());
      return new InputStreamResource(cosFallback);
    }
    log.warn("File content missing on disk: id={}, objectKey={}, path={}", entity.getId(), entity.getObjectKey(), path);
    throw new BusinessException(ErrorCode.NOT_FOUND, "File content not found");
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    deleteForTenant(tenantId, id);
  }

  @Transactional
  public void deleteForTenant(Long tenantId, Long id) {
    FileEntity entity = getExistingFile(tenantId, id);
    entity.setStatus("deleted");
    fileMapper.updateById(entity);
    storage.delete(entity.getObjectKey());
  }

  /**
   * Marks metadata deleted in the current transaction and removes the storage object only after
   * commit. A database rollback therefore never restores a row whose object has already gone.
   */
  @Transactional
  public void deleteAfterCommitForTenant(Long tenantId, Long id) {
    FileEntity entity = getExistingFile(tenantId, id);
    entity.setStatus("deleted");
    fileMapper.updateById(entity);
    String objectKey = entity.getObjectKey();
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          storage.delete(objectKey);
        }
      });
    } else {
      storage.delete(objectKey);
    }
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
    // objectKey 仅用 ASCII（UUID + 扩展名），避免中文文件名导致 COS 上传/下载异常
    String ext = "";
    if (fileName != null) {
      int dot = fileName.lastIndexOf('.');
      if (dot >= 0 && dot < fileName.length() - 1) {
        ext = fileName.substring(dot).replaceAll("[^A-Za-z0-9.]", "").toLowerCase();
      }
    }
    LocalDate now = LocalDate.now();
    return normalizeObjectKeyPrefix(properties.objectKeyPrefix()) + String.format("%d/%d/%02d/%s%s",
        tenantId == null ? 0L : tenantId,
        now.getYear(),
        now.getMonthValue(),
        UUID.randomUUID(),
        ext);
  }

  private String normalizeObjectKeyPrefix(String configuredPrefix) {
    if (!StringUtils.hasText(configuredPrefix)) {
      return "";
    }
    String prefix = configuredPrefix.trim();
    if (prefix.startsWith("/")
        || prefix.endsWith("/")
        || prefix.contains("\\")
        || !prefix.matches("[A-Za-z0-9._-]+(?:/[A-Za-z0-9._-]+)*")) {
      throw new IllegalStateException("Invalid file object key prefix");
    }
    for (String segment : prefix.split("/")) {
      if (segment.equals(".") || segment.equals("..")) {
        throw new IllegalStateException("Invalid file object key prefix");
      }
    }
    return prefix + "/";
  }

  private String sanitizeFileName(String originalName) {
    if (!StringUtils.hasText(originalName)) {
      return "file.bin";
    }
    String name = Paths.get(originalName).getFileName().toString();
    name = name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_").trim();
    return StringUtils.hasText(name) ? name : "file.bin";
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
