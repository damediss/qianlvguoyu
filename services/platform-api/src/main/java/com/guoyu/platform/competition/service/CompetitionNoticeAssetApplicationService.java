package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionNoticeAssetEntity;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.enums.CompetitionNoticeAssetType;
import com.guoyu.platform.competition.enums.CompetitionNoticeContentType;
import com.guoyu.platform.competition.enums.CompetitionNoticeStatus;
import com.guoyu.platform.competition.mapper.CompetitionNoticeAssetMapper;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.player.security.CompetitionPlayerSecurityContext;
import com.guoyu.platform.competition.vo.CompetitionNoticeAssetVO;
import com.guoyu.platform.competition.vo.CompetitionNoticeAssetRow;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class CompetitionNoticeAssetApplicationService {

  private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
  private static final long MAX_PDF_BYTES = 50L * 1024 * 1024;

  private final CompetitionNoticeService noticeService;
  private final CompetitionNoticeAssetMapper assetMapper;
  private final FileApplicationService fileService;

  public CompetitionNoticeAssetApplicationService(
      CompetitionNoticeService noticeService,
      CompetitionNoticeAssetMapper assetMapper,
      FileApplicationService fileService) {
    this.noticeService = noticeService;
    this.assetMapper = assetMapper;
    this.fileService = fileService;
  }

  @Transactional
  public CompetitionNoticeAssetVO upload(
      Long noticeId, String requestedAssetType, MultipartFile file) {
    PlatformUserPrincipal user = currentUser();
    CompetitionNoticeEntity notice = noticeService.lockDraftForAssetMutation(user, noticeId);
    String expectedAssetType = CompetitionNoticeContentType.PDF.getCode().equals(notice.getContentType())
        ? CompetitionNoticeAssetType.PDF.getCode()
        : CompetitionNoticeAssetType.INLINE_IMAGE.getCode();
    String assetType = normalizeAssetType(requestedAssetType);
    if (!expectedAssetType.equals(assetType)) {
      throw new BusinessException(
          ErrorCode.BAD_REQUEST, "上传文件类型与公告内容类型不匹配");
    }
    validateUpload(file, assetType);

    FileVO stored = fileService.uploadForTenant(user.getTenantId(), file, "private");
    try {
      CompetitionNoticeAssetEntity asset = new CompetitionNoticeAssetEntity();
      asset.setTenantId(user.getTenantId());
      asset.setNoticeId(noticeId);
      asset.setFileId(stored.id());
      asset.setAssetType(assetType);
      asset.setSortOrder(nextSortOrder(user.getTenantId(), noticeId));
      asset.setDeleted(0);
      assetMapper.insert(asset);

      if (CompetitionNoticeAssetType.PDF.getCode().equals(assetType)) {
        for (CompetitionNoticeAssetEntity previous : listEntities(user.getTenantId(), noticeId)) {
          if (!previous.getId().equals(asset.getId())
              && CompetitionNoticeAssetType.PDF.getCode().equals(previous.getAssetType())) {
            assetMapper.deleteById(previous.getId());
            fileService.deleteAfterCommitForTenant(user.getTenantId(), previous.getFileId());
          }
        }
      }
      return toVO(asset, stored);
    } catch (RuntimeException ex) {
      fileService.deleteForTenant(user.getTenantId(), stored.id());
      throw ex;
    }
  }

  @Transactional
  public void delete(Long noticeId, Long assetId) {
    PlatformUserPrincipal user = currentUser();
    noticeService.lockDraftForAssetMutation(user, noticeId);
    CompetitionNoticeAssetEntity asset = findOwned(user.getTenantId(), noticeId, assetId);
    assetMapper.deleteById(asset.getId());
    fileService.deleteAfterCommitForTenant(user.getTenantId(), asset.getFileId());
  }

  @Transactional
  public void removeIncompatibleAssets(
      PlatformUserPrincipal user, Long noticeId, String newContentType) {
    noticeService.lockDraftForAssetMutation(user, noticeId);
    String keepType = CompetitionNoticeContentType.PDF.getCode().equals(newContentType)
        ? CompetitionNoticeAssetType.PDF.getCode()
        : CompetitionNoticeAssetType.INLINE_IMAGE.getCode();
    for (CompetitionNoticeAssetEntity asset : listEntities(user.getTenantId(), noticeId)) {
      if (!keepType.equals(asset.getAssetType())) {
        assetMapper.deleteById(asset.getId());
        fileService.deleteAfterCommitForTenant(user.getTenantId(), asset.getFileId());
      }
    }
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> managerDownload(Long noticeId, Long assetId) {
    PlatformUserPrincipal user = currentUser();
    noticeService.getById(user, noticeId);
    CompetitionNoticeAssetEntity asset = findOwned(user.getTenantId(), noticeId, assetId);
    return fileService.buildDownloadResponse(user.getTenantId(), asset.getFileId(), true);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> playerDownload(Long messageId, Long assetId) {
    CompetitionPlayerPrincipal player = currentPlayer();
    CompetitionNoticeAssetEntity asset = assetMapper.selectPlayerAccessible(
        player.getTenantId(), player.getPlayerId(), messageId, assetId);
    if (asset == null) {
      log.warn("Player attempted inaccessible notice asset, tenantId={}, playerId={}, messageId={}, assetId={}",
          player.getTenantId(), player.getPlayerId(), messageId, assetId);
      throw new BusinessException(ErrorCode.NOT_FOUND, "公告文件不存在或当前赛事资格已失效");
    }
    return fileService.buildDownloadResponse(player.getTenantId(), asset.getFileId(), true);
  }

  @Transactional(readOnly = true)
  public List<CompetitionNoticeAssetVO> list(Long tenantId, Long noticeId) {
    return listByNoticeIds(tenantId, List.of(noticeId))
        .getOrDefault(noticeId, List.of());
  }

  @Transactional(readOnly = true)
  public Map<Long, List<CompetitionNoticeAssetVO>> listByNoticeIds(
      Long tenantId, List<Long> noticeIds) {
    Map<Long, List<CompetitionNoticeAssetVO>> grouped = new LinkedHashMap<>();
    if (noticeIds == null || noticeIds.isEmpty()) {
      return grouped;
    }
    List<Long> distinctIds = noticeIds.stream()
        .filter(java.util.Objects::nonNull)
        .distinct()
        .toList();
    if (distinctIds.isEmpty()) {
      return grouped;
    }
    List<CompetitionNoticeAssetRow> rows =
        assetMapper.selectRowsByNoticeIds(tenantId, distinctIds);
    if (rows == null) {
      log.warn("Notice asset batch mapper returned null, tenantId={}, noticeCount={}",
          tenantId, distinctIds.size());
      return grouped;
    }
    for (CompetitionNoticeAssetRow row : rows) {
      CompetitionNoticeAssetVO vo = new CompetitionNoticeAssetVO();
      vo.setAssetId(row.getAssetId());
      vo.setAssetType(row.getAssetType());
      vo.setOriginalName(row.getOriginalName());
      vo.setContentType(row.getContentType());
      vo.setSizeBytes(row.getSizeBytes());
      vo.setSourceUri("notice-asset://" + row.getAssetId());
      grouped.computeIfAbsent(row.getNoticeId(), ignored -> new ArrayList<>()).add(vo);
    }
    return grouped;
  }

  private List<CompetitionNoticeAssetEntity> listEntities(Long tenantId, Long noticeId) {
    List<CompetitionNoticeAssetEntity> assets = assetMapper.selectList(
        new LambdaQueryWrapper<CompetitionNoticeAssetEntity>()
            .eq(CompetitionNoticeAssetEntity::getTenantId, tenantId)
            .eq(CompetitionNoticeAssetEntity::getNoticeId, noticeId)
            .eq(CompetitionNoticeAssetEntity::getDeleted, 0)
            .orderByAsc(CompetitionNoticeAssetEntity::getSortOrder)
            .orderByAsc(CompetitionNoticeAssetEntity::getId));
    return assets == null ? new ArrayList<>() : new ArrayList<>(assets);
  }

  private CompetitionNoticeAssetEntity findOwned(Long tenantId, Long noticeId, Long assetId) {
    CompetitionNoticeAssetEntity asset = assetMapper.selectOne(
        new LambdaQueryWrapper<CompetitionNoticeAssetEntity>()
            .eq(CompetitionNoticeAssetEntity::getTenantId, tenantId)
            .eq(CompetitionNoticeAssetEntity::getNoticeId, noticeId)
            .eq(CompetitionNoticeAssetEntity::getId, assetId)
            .eq(CompetitionNoticeAssetEntity::getDeleted, 0));
    if (asset == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "公告文件不存在");
    }
    return asset;
  }

  private int nextSortOrder(Long tenantId, Long noticeId) {
    return listEntities(tenantId, noticeId).stream()
        .map(CompetitionNoticeAssetEntity::getSortOrder)
        .filter(java.util.Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(-1) + 1;
  }

  private void validateUpload(MultipartFile file, String assetType) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要上传的文件");
    }
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    byte[] head = new byte[16];
    int read;
    try (InputStream input = file.getInputStream()) {
      read = input.read(head);
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "读取上传文件失败");
    }
    if (CompetitionNoticeAssetType.PDF.getCode().equals(assetType)) {
      if (file.getSize() > MAX_PDF_BYTES || !name.endsWith(".pdf")
          || !"application/pdf".equals(mime) || read < 5
          || head[0] != '%' || head[1] != 'P' || head[2] != 'D' || head[3] != 'F' || head[4] != '-') {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持不超过50MB的有效PDF文件");
      }
      return;
    }
    String extensionType = name.endsWith(".jpg") || name.endsWith(".jpeg") ? "jpeg"
        : name.endsWith(".png") ? "png" : name.endsWith(".webp") ? "webp" : null;
    String mimeType = "image/jpeg".equals(mime) ? "jpeg"
        : "image/png".equals(mime) ? "png" : "image/webp".equals(mime) ? "webp" : null;
    String magicType = detectImageMagic(head, read);
    if (file.getSize() > MAX_IMAGE_BYTES || extensionType == null || mimeType == null
        || magicType == null || !extensionType.equals(mimeType)
        || !extensionType.equals(magicType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持不超过10MB的JPG、PNG或WebP图片");
    }
  }

  private String normalizeAssetType(String assetType) {
    if (CompetitionNoticeAssetType.PDF.getCode().equals(assetType)
        || CompetitionNoticeAssetType.INLINE_IMAGE.getCode().equals(assetType)) {
      return assetType;
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "公告文件类型不合法");
  }

  private String detectImageMagic(byte[] head, int length) {
    boolean jpeg = length >= 3 && (head[0] & 0xff) == 0xff
        && (head[1] & 0xff) == 0xd8 && (head[2] & 0xff) == 0xff;
    boolean png = length >= 8 && (head[0] & 0xff) == 0x89
        && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
    boolean webp = length >= 12 && head[0] == 'R' && head[1] == 'I'
        && head[2] == 'F' && head[3] == 'F' && head[8] == 'W'
        && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
    return jpeg ? "jpeg" : png ? "png" : webp ? "webp" : null;
  }

  private CompetitionNoticeAssetVO toVO(CompetitionNoticeAssetEntity asset, FileVO file) {
    CompetitionNoticeAssetVO vo = new CompetitionNoticeAssetVO();
    vo.setAssetId(asset.getId());
    vo.setAssetType(asset.getAssetType());
    vo.setOriginalName(file.originalName());
    vo.setContentType(file.contentType());
    vo.setSizeBytes(file.sizeBytes());
    vo.setSourceUri("notice-asset://" + asset.getId());
    return vo;
  }

  private void requireDraft(CompetitionNoticeEntity notice) {
    if (!CompetitionNoticeStatus.DRAFT.getCode().equals(notice.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已发布公告的文件不可修改");
    }
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition notice asset request has no authenticated platform user");
      throw ex;
    }
  }

  private CompetitionPlayerPrincipal currentPlayer() {
    try {
      return CompetitionPlayerSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Competition player asset request has no authenticated player");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "选手登录已失效");
    }
  }
}
