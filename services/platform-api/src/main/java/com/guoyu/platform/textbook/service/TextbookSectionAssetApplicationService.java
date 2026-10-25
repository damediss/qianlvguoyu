package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.textbook.domain.TextbookMarkdownSupport;
import com.guoyu.platform.textbook.entity.TextbookSectionAssetEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentVersionEntity;
import com.guoyu.platform.textbook.mapper.TextbookSectionAssetMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentVersionMapper;
import com.guoyu.platform.textbook.service.TextbookProjectLockService.LockedSection;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 节正文图片（方案 §6.4 A1-A3、§5.5/§5.6）。
 *
 * <p>图片固定写入 file 模块受保护区域 protected/textbook/**；在线读取唯一入口 A2
 * （先经 TextbookAccessService 节读取授权，再内部受控下载）。A1 可先预检并在锁外上传
 * 存储，但写入资产元数据前必须按 §7.1 锁序加锁、重新授权；失败删除刚上传文件。
 * A3 在锁内重新解析当前正文与全部快照的引用，任何快照引用都禁止删除；软删元数据后
 * 物理文件只在事务提交成功后清理。
 */
@Service
public class TextbookSectionAssetApplicationService {

  private static final Logger log =
      LoggerFactory.getLogger(TextbookSectionAssetApplicationService.class);

  private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
  private static final List<String> ALLOWED_EXTENSIONS = List.of("jpeg", "jpg", "png", "webp");

  private final TextbookSectionAssetMapper assetMapper;
  private final TextbookSectionContentMapper contentMapper;
  private final TextbookSectionContentVersionMapper versionMapper;
  private final TextbookProjectLockService lockService;
  private final TextbookAccessService accessService;
  private final TextbookSectionContentApplicationService contentService;
  private final FileApplicationService fileApplicationService;

  public TextbookSectionAssetApplicationService(
      TextbookSectionAssetMapper assetMapper,
      TextbookSectionContentMapper contentMapper,
      TextbookSectionContentVersionMapper versionMapper,
      TextbookProjectLockService lockService,
      TextbookAccessService accessService,
      TextbookSectionContentApplicationService contentService,
      FileApplicationService fileApplicationService) {
    this.assetMapper = assetMapper;
    this.contentMapper = contentMapper;
    this.versionMapper = versionMapper;
    this.lockService = lockService;
    this.accessService = accessService;
    this.contentService = contentService;
    this.fileApplicationService = fileApplicationService;
  }

  // ============================================================
  // A1 上传（允许 untouched/draft/returned；不创建 content、不改变 version）
  // ============================================================

  @Transactional
  public AssetUploadResult upload(Long sectionId, MultipartFile file) {
    // 锁外快速预检 + 存储上传（长文件 IO 不进工程锁，方案 §7.1-6）
    validateImage(file);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    FileVO stored = fileApplicationService.uploadProtectedForTenant(tenantId, file, "textbook");
    try {
      LockedSection locked = lockService.lockBySection(sectionId);
      accessService.requireCurrentWriter(locked.project(), locked.assignment(),
          SecurityContext.currentUser());
      requireContentWritable(locked.content());

      // 锁内重新授权成功后写元数据（归属=本节）
      Integer maxSort = assetMapper.selectList(new LambdaQueryWrapper<TextbookSectionAssetEntity>()
              .eq(TextbookSectionAssetEntity::getSectionId, sectionId)
              .orderByDesc(TextbookSectionAssetEntity::getSortNo)
              .last("LIMIT 1"))
          .stream().findFirst().map(TextbookSectionAssetEntity::getSortNo).orElse(0);
      TextbookSectionAssetEntity asset = new TextbookSectionAssetEntity();
      asset.setProjectId(locked.project().getId());
      asset.setSectionId(sectionId);
      asset.setFileId(stored.id());
      asset.setAssetType("inline_image");
      asset.setSortNo(maxSort == null ? 1 : maxSort + 1);
      assetMapper.insert(asset);
      log.info("[教材] 上传图片：sectionId={} assetId={} fileId={}", sectionId, asset.getId(),
          stored.id());
      return new AssetUploadResult(String.valueOf(asset.getId()),
          TextbookMarkdownSupport.ASSET_URI_PREFIX + asset.getId());
    } catch (RuntimeException ex) {
      // 锁内校验失败：当前事务即将回滚（文件元数据行随之消失），必须立即物理删除刚上传
      // 的受保护对象；deleteAfterCommit 在回滚事务中不会触发，会把文件泄漏成无主对象。
      fileApplicationService.deleteStorageImmediatelyForCleanup(tenantId, stored.id());
      throw ex;
    }
  }

  public record AssetUploadResult(String assetId, String sourceUri) {
  }

  // ============================================================
  // A2 受控在线读取（节读取授权后 inline 下载；no-store）
  // ============================================================

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> downloadContent(Long sectionId, Long assetId) {
    accessService.requireSectionRead(sectionId);
    TextbookSectionAssetEntity asset = assetOfSection(sectionId, assetId);
    ResponseEntity<Resource> response = fileApplicationService.buildDownloadResponse(
        asset.getTenantId(), asset.getFileId(), true);
    // Cache-Control: private, no-store（方案 §6.4 A2）
    return ResponseEntity.status(response.getStatusCode())
        .headers(headers -> headers.putAll(response.getHeaders()))
        .header("Cache-Control", "private, no-store")
        .body(response.getBody());
  }

  // ============================================================
  // A3 删除（锁内重校验；任何快照引用都禁止删除；提交后清理物理文件）
  // ============================================================

  @Transactional
  public void delete(Long sectionId, Long assetId, Integer baseVersion) {
    LockedSection locked = lockService.lockBySection(sectionId);
    accessService.requireCurrentWriter(locked.project(), locked.assignment(),
        SecurityContext.currentUser());
    TextbookSectionAssetEntity asset = assetOfSection(sectionId, assetId);

    // 状态与 baseVersion 重校验（无正文时 baseVersion=0，方案 §6.4 A3）
    TextbookSectionContentEntity content = locked.content();
    if (content == null) {
      if (baseVersion != null && baseVersion != 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "该节无正文记录，baseVersion 应为 0");
      }
    } else {
      String status = content.getContentStatus();
      if (!TextbookSectionContentApplicationService.STATUS_DRAFT.equals(status)
          && !TextbookSectionContentApplicationService.STATUS_RETURNED.equals(status)) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "本节已提交或已通过，不能删除图片（submitted/approved/done 一律 409）");
      }
      if (baseVersion == null || !baseVersion.equals(content.getVersion())) {
        throw contentService.versionConflict(content);
      }
    }
    // 当前正文与全部快照重新解析引用：任何引用都禁止删除（防历史快照裂图）
    String assetUri = TextbookMarkdownSupport.ASSET_URI_PREFIX + assetId;
    if (content != null && content.getContentMd() != null
        && content.getContentMd().contains(assetUri)) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前正文仍引用该图片，先在正文中移除后再删除");
    }
    for (TextbookSectionContentVersionEntity snapshot : versionMapper.selectList(
        new LambdaQueryWrapper<TextbookSectionContentVersionEntity>()
            .eq(TextbookSectionContentVersionEntity::getSectionId, sectionId))) {
      if (snapshot.getContentMd() != null && snapshot.getContentMd().contains(assetUri)) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "历史快照仍引用该图片，禁止删除（避免快照裂图）");
      }
    }
    assetMapper.deleteById(asset.getId());
    Long tenantId = locked.project().getTenantId();
    Long fileId = asset.getFileId();
    // 软删元数据后，物理文件只在事务提交成功后清理（方案 §6.4 A3）
    registerAfterCommitCleanup(tenantId, fileId);
    log.info("[教材] 删除图片：sectionId={} assetId={} fileId={}", sectionId, assetId, fileId);
  }

  private void registerAfterCommitCleanup(Long tenantId, Long fileId) {
    // deleteAfterCommitForTenant 内部完成元数据置 deleted + 事务提交后物理删除
    //（此处不可先手动置 deleted——其 getExistingFile 只认 status='uploaded'）
    fileApplicationService.deleteAfterCommitForTenant(tenantId, fileId);
  }

  // ============================================================
  // 内部
  // ============================================================

  private TextbookSectionAssetEntity assetOfSection(Long sectionId, Long assetId) {
    TextbookSectionAssetEntity asset = assetMapper.selectById(assetId);
    if (asset == null || !sectionId.equals(asset.getSectionId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在或不属于本节");
    }
    return asset;
  }

  private void requireContentWritable(TextbookSectionContentEntity content) {
    if (content == null) {
      return;
    }
    String status = content.getContentStatus();
    if (!TextbookSectionContentApplicationService.STATUS_DRAFT.equals(status)
        && !TextbookSectionContentApplicationService.STATUS_RETURNED.equals(status)) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "本节已提交或已通过，不能上传图片（submitted/approved/done 一律 409）");
    }
  }

  /** 扩展名 / MIME / magic number 三者一致（方案 §5.5）。 */
  private void validateImage(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件为空");
    }
    if (file.getSize() > MAX_IMAGE_BYTES) {
      throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "单张图片不能超过 10MB");
    }
    String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    String lower = original.toLowerCase(Locale.ROOT);
    int dot = lower.lastIndexOf('.');
    String ext = dot >= 0 ? lower.substring(dot + 1) : "";
    if (!ALLOWED_EXTENSIONS.contains(ext)) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE,
          "图片仅支持 jpeg/png/webp，拒绝扩展名：" + (ext.isEmpty() ? "(无)" : ext));
    }
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    String expectedMime = "jpg".equals(ext) || "jpeg".equals(ext) ? "jpeg" : ext;
    if (!("image/" + expectedMime).equals(contentType)) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE,
          "图片 MIME 类型与扩展名不一致：" + contentType);
    }
    byte[] head;
    try {
      head = new byte[16];
      try (var in = file.getInputStream()) {
        int read = in.readNBytes(head, 0, head.length);
        if (read <= 0) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件为空");
        }
      }
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "读取上传文件失败");
    }
    boolean magicOk = switch (ext) {
      case "png" -> head.length >= 4 && (head[0] & 0xFF) == 0x89 && head[1] == 'P'
          && head[2] == 'N' && head[3] == 'G';
      case "jpg", "jpeg" -> head.length >= 2 && (head[0] & 0xFF) == 0xFF
          && (head[1] & 0xFF) == 0xD8;
      case "webp" -> head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F'
          && head[3] == 'F' && head[8] == 'W' && head[9] == 'E' && head[10] == 'B'
          && head[11] == 'P';
      default -> false;
    };
    if (!magicOk) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE,
          "文件内容与图片扩展名不符（magic number 校验失败）");
    }
  }
}
