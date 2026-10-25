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
import com.guoyu.platform.file.support.ProtectedFileAccessGuard;
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
import java.time.Duration;
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

  /**
   * 受保护上传（教材二期方案 §5.6-1）：对象 key 固定写入
   * {@code protected/{businessArea}/{tenantId}/{yyyy}/{MM}/{uuid}.{ext}}，accessPolicy=protected。
   *
   * <p>file 模块只负责安全生成 objectKey、存文件与元数据，不感知教材权限；业务侧
   * （教材 A1）在上传前后自行做章节归属授权。返回 VO 的 url 恒为 null——受保护文件
   * 的唯一在线读取入口是业务受控接口（教材 A2），走内部 {@link #buildDownloadResponse}。
   */
  @Transactional
  public FileVO uploadProtectedForTenant(Long tenantId, MultipartFile file, String businessArea) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
    }
    if (tenantId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Tenant is required");
    }
    String area = normalizeBusinessArea(businessArea);
    String safeName = sanitizeFileName(file.getOriginalFilename());
    String objectKey = buildProtectedObjectKey(area, tenantId, safeName);
    try {
      String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(file.getBytes()));
      String url;
      try (InputStream in = file.getInputStream()) {
        url = storage.putProtected(objectKey, in, file.getSize(), file.getContentType());
      }
      FileEntity entity = newFileEntity(tenantId, objectKey, safeName,
          file.getContentType(), file.getSize(), sha256, "protected");
      try {
        fileMapper.insert(entity);
      } catch (RuntimeException ex) {
        storage.delete(objectKey);
        throw ex;
      }
      registerRollbackCleanup(objectKey);
      return toVO(entity, url);
    } catch (IOException | NoSuchAlgorithmException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File upload failed");
    } catch (RuntimeException ex) {
      storage.delete(objectKey);
      throw ex;
    }
  }

  private String normalizeBusinessArea(String businessArea) {
    if (!StringUtils.hasText(businessArea)
        || !businessArea.matches("[a-z0-9][a-z0-9-]{0,31}")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid protected business area");
    }
    return businessArea;
  }

  private String buildProtectedObjectKey(String area, Long tenantId, String fileName) {
    String ext = "";
    if (fileName != null) {
      int dot = fileName.lastIndexOf('.');
      if (dot >= 0 && dot < fileName.length() - 1) {
        ext = fileName.substring(dot).replaceAll("[^A-Za-z0-9.]", "").toLowerCase();
      }
    }
    LocalDate now = LocalDate.now();
    return String.format("protected/%s/%d/%d/%02d/%s%s",
        area, tenantId == null ? 0L : tenantId, now.getYear(), now.getMonthValue(),
        UUID.randomUUID(), ext);
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
    ProtectedFileAccessGuard.rejectExternalAccess(entity);
    return toVO(entity, publicUrl(entity));
  }

  @Transactional(readOnly = true)
  public DownloadUrlVO downloadUrl(Long id) {
    FileEntity entity = getExistingFile(SecurityContext.currentUser().getTenantId(), id);
    ProtectedFileAccessGuard.rejectExternalAccess(entity);
    // COS 文件给预签名直链（10 分钟有效）：大文件让客户端直连 COS，绕开服务端中转和 120s 超时墙；
    // local 等无法直链的存储回退鉴权中转 /download
    String url = "/api/files/" + entity.getId() + "/download";
    if ("cos".equalsIgnoreCase(entity.getStorageProvider())) {
      url = storage.presignedDownloadUrl(entity.getObjectKey(), Duration.ofMinutes(10))
          .map(java.net.URL::toString)
          .orElse(url);
    }
    return new DownloadUrlVO(entity.getId(), url, entity.getAccessPolicy());
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
    // 元数据（文件名/大小/类型）可给，受保护文件的公开 url 必须为 null（方案 §5.6-4）
    return toVO(entity, ProtectedFileAccessGuard.externalUrlOrNull(entity, publicUrl(entity)));
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
      // P2-2：底层 IO 消息可能含磁盘路径，只入日志不回显
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取文件失败，请稍后重试");
    }
  }

  /**
   * 导出素材回退地址（无 SecurityContext）：COS 给预签名直链；local 给 /api/exam/files/{objectKey}。
   * 供默认模板导出在本机读盘失败时继续拉取，避免毕业证/等级证静默丢失。
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
  public Optional<String> resolveExportFetchUrl(Long id) {
    FileEntity entity = findUploadedFile(id);
    if (ProtectedFileAccessGuard.isProtectedObjectKey(entity.getObjectKey())) {
      // 受保护文件禁止经通用导出素材回退地址（预签名/公开 URL）外泄（方案 §5.6-2）
      return Optional.empty();
    }
    if ("cos".equalsIgnoreCase(entity.getStorageProvider())) {
      Optional<String> signed = storage.presignedDownloadUrl(entity.getObjectKey(), Duration.ofMinutes(30))
          .map(java.net.URL::toString);
      if (signed.isPresent()) {
        return signed;
      }
    }
    String url = publicUrl(entity);
    return StringUtils.hasText(url) ? Optional.of(url) : Optional.empty();
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
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取文件失败，请稍后重试");
    }
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> buildDownloadResponse(Long id, boolean inline) {
    FileEntity entity = getExistingFile(SecurityContext.currentUser().getTenantId(), id);
    // FileController /files/{id}/download 通用入口：受保护文件默认拒绝（方案 §5.6-2）。
    // 业务受控下载（教材 A2）走 buildDownloadResponse(tenantId, id, inline) 内部变体。
    ProtectedFileAccessGuard.rejectExternalAccess(entity);
    return buildDownloadResponse(entity, loadResource(entity), inline);
  }

  /**
   * 内部受控下载变体：调用方（如教材 A2）已完成业务级授权（TextbookAccessService），
   * 受保护文件允许经此读取；同租户普通文件行为不变。
   */
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
    // 通用 DELETE /files/{id} 外部入口不允许删受保护文件（防绕过教材 A3 快照引用检查）；
    // 业务侧完成自身校验后走内部 deleteAfterCommitForTenant 清理（方案 §5.6-3）。
    ProtectedFileAccessGuard.rejectExternalAccess(entity);
    entity.setStatus("deleted");
    fileMapper.updateById(entity);
    storage.delete(entity.getObjectKey());
  }

  /**
   * 业务受控失败的立即物理清理（教材 A1 锁内授权失败后调用）。
   *
   * <p>与 {@link #deleteAfterCommitForTenant} 不同：调用方所在事务即将回滚，文件元数据行
   * 会随之消失，不能依赖 afterCommit（回滚不会触发），否则受保护对象会变成无主物理文件。
   * 这里只删存储对象、不改元数据——回滚后元数据本就不存在；若事务异常提交，遗留的
   * uploaded 行会因对象缺失在后续读取时暴露为明确错误，而不是静默泄漏文件。
   */
  @Transactional
  public void deleteStorageImmediatelyForCleanup(Long tenantId, Long id) {
    FileEntity entity = getExistingFile(tenantId, id);
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

  /**
   * 取当前登录用户的租户。
   *
   * <p>P0-5：此前在未登录时兜底返回 0，配合 `/files/**` 的 permitAll，任何人都能匿名上传并把文件
   * 落到 tenant_id=0。现在未认证直接抛异常（由全局异常处理器转为 401），匿名上传链路关闭。
   */
  private Long getTenantIdSafe() {
    try {
      return SecurityContext.currentUser().getTenantId();
    } catch (IllegalStateException ex) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后再操作文件");
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
