package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionNoticeConverter;
import com.guoyu.platform.competition.dto.CompetitionNoticeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeQueryReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionNoticeAssetEntity;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionNoticeAssetType;
import com.guoyu.platform.competition.enums.CompetitionNoticeContentType;
import com.guoyu.platform.competition.enums.CompetitionNoticeStatus;
import com.guoyu.platform.competition.mapper.CompetitionNoticeAssetMapper;
import com.guoyu.platform.competition.mapper.CompetitionNoticeMapper;
import com.guoyu.platform.competition.messaging.service.CompetitionOutboxApplicationService;
import com.guoyu.platform.competition.vo.CompetitionNoticePublishResultVO;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionNoticeService {

  private static final int MAX_TITLE_LENGTH = 256;
  private static final int MAX_MARKDOWN_LENGTH = 100000;
  private static final int MAX_PDF_DESCRIPTION_LENGTH = 2000;
  private static final Pattern EXACT_MARKDOWN_IMAGE_PATTERN =
      Pattern.compile("!\\[[^\\]\\r\\n]*]\\(notice-asset://(\\d+)\\)");
  private static final Pattern RAW_HTML_PATTERN =
      Pattern.compile("(?is)<(?:!--|!DOCTYPE|\\?|/?\\s*[A-Za-z])[^>]*>");

  private final CompetitionNoticeMapper mapper;
  private final CompetitionEventService eventService;
  private final UserMapper userMapper;
  private final OrgMapper orgMapper;
  private final CompetitionOutboxApplicationService outboxApplicationService;
  private final CompetitionNoticeAssetMapper assetMapper;
  private final FileApplicationService fileService;

  @Autowired
  public CompetitionNoticeService(
      CompetitionNoticeMapper mapper,
      CompetitionEventService eventService,
      UserMapper userMapper,
      OrgMapper orgMapper,
      CompetitionOutboxApplicationService outboxApplicationService,
      CompetitionNoticeAssetMapper assetMapper,
      FileApplicationService fileService) {
    this.mapper = mapper;
    this.eventService = eventService;
    this.userMapper = userMapper;
    this.orgMapper = orgMapper;
    this.outboxApplicationService = outboxApplicationService;
    this.assetMapper = assetMapper;
    this.fileService = fileService;
  }

  CompetitionNoticeService(
      CompetitionNoticeMapper mapper,
      CompetitionEventService eventService,
      UserMapper userMapper,
      OrgMapper orgMapper,
      CompetitionOutboxApplicationService outboxApplicationService) {
    this(mapper, eventService, userMapper, orgMapper, outboxApplicationService, null, null);
  }

  @Transactional
  public CompetitionNoticeEntity create(
      PlatformUserPrincipal principal, Long eventId, CompetitionNoticeCreateReq req) {
    requireAccessibleEvent(principal, eventId);
    validateDraft(req.title(), req.content(), req.contentType(), req.version());

    CompetitionNoticeEntity entity = CompetitionNoticeConverter.toEntity(req);
    entity.setTenantId(principal.getTenantId());
    entity.setEventId(eventId);
    entity.setVersion(req.version() == null ? 1 : req.version());
    entity.setStatus(CompetitionNoticeStatus.DRAFT.getCode());
    entity.setDeleted(0);
    mapper.insert(entity);
    return entity;
  }

  @Transactional(readOnly = true)
  public Page<CompetitionNoticeEntity> page(
      PlatformUserPrincipal principal, Long eventId, CompetitionNoticeQueryReq req) {
    requireAccessibleEvent(principal, eventId);
    LambdaQueryWrapper<CompetitionNoticeEntity> wrapper = new LambdaQueryWrapper<CompetitionNoticeEntity>()
        .eq(CompetitionNoticeEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionNoticeEntity::getEventId, eventId)
        .eq(CompetitionNoticeEntity::getDeleted, 0)
        .orderByDesc(CompetitionNoticeEntity::getCreatedAt)
        .orderByDesc(CompetitionNoticeEntity::getId);
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CompetitionNoticeEntity::getStatus, validateStatus(req.getStatus()));
    }
    if (StringUtils.hasText(req.getContentType())) {
      wrapper.eq(CompetitionNoticeEntity::getContentType,
          CompetitionNoticeContentType.normalize(req.getContentType()));
    }
    Page<CompetitionNoticeEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    if (page == null) {
      log.warn("Competition notice mapper returned null page, tenantId={}, eventId={}",
          principal.getTenantId(), eventId);
      return new Page<>(req.getPage(), req.getPageSize());
    }
    return page;
  }

  @Transactional(readOnly = true)
  public CompetitionNoticeEntity getById(PlatformUserPrincipal principal, Long noticeId) {
    CompetitionNoticeEntity notice = findExisting(principal.getTenantId(), noticeId);
    requireAccessibleEvent(principal, notice.getEventId());
    return notice;
  }

  /**
   * Serializes every asset mutation with publish by locking the owning notice row first.
   */
  @Transactional
  public CompetitionNoticeEntity lockDraftForAssetMutation(
      PlatformUserPrincipal principal, Long noticeId) {
    CompetitionNoticeEntity notice = mapper.selectForUpdate(principal.getTenantId(), noticeId);
    if (notice == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
    }
    requireAccessibleEvent(principal, notice.getEventId());
    if (!CompetitionNoticeStatus.DRAFT.getCode().equals(notice.getStatus())) {
      log.warn("Competition notice asset mutation status conflict, tenantId={}, noticeId={}, status={}",
          principal.getTenantId(), noticeId, notice.getStatus());
      throw new BusinessException(ErrorCode.CONFLICT, "已发布公告的文件不可修改");
    }
    return notice;
  }

  @Transactional
  public CompetitionNoticeEntity updateDraft(
      PlatformUserPrincipal principal, Long noticeId, CompetitionNoticeUpdateReq req) {
    CompetitionNoticeEntity notice = mapper.selectForUpdate(principal.getTenantId(), noticeId);
    if (notice == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
    }
    requireAccessibleEvent(principal, notice.getEventId());
    if (!CompetitionNoticeStatus.DRAFT.getCode().equals(notice.getStatus())) {
      log.warn("Competition notice update status conflict, tenantId={}, noticeId={}, status={}",
          principal.getTenantId(), noticeId, notice.getStatus());
      throw new BusinessException(ErrorCode.CONFLICT, "仅草稿通知允许修改");
    }
    validateDraft(req.title(), req.content(), req.contentType(), req.version());
    CompetitionNoticeConverter.applyUpdate(notice, req);
    notice.setUpdatedBy(principal.getUserId());
    notice.setUpdatedAt(LocalDateTime.now());
    if (mapper.updateById(notice) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "通知状态已发生变化，请刷新后重试");
    }
    return notice;
  }

  @Transactional
  public CompetitionNoticePublishResultVO publish(
      PlatformUserPrincipal principal, Long noticeId) {
    CompetitionNoticeEntity notice = mapper.selectForUpdate(principal.getTenantId(), noticeId);
    if (notice == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
    }
    requireAccessibleEvent(principal, notice.getEventId());
    if (!CompetitionNoticeStatus.DRAFT.getCode().equals(notice.getStatus())) {
      log.warn("Competition notice publish status conflict, tenantId={}, noticeId={}, status={}",
          principal.getTenantId(), noticeId, notice.getStatus());
      throw new BusinessException(ErrorCode.CONFLICT, "仅草稿通知允许发布");
    }
    validateForPublish(notice);

    String beforeStatus = notice.getStatus();
    notice.setStatus(CompetitionNoticeStatus.PUBLISHED.getCode());
    notice.setPublishedBy(principal.getUserId());
    notice.setPublishedAt(LocalDateTime.now());
    notice.setUpdatedBy(principal.getUserId());
    notice.setUpdatedAt(LocalDateTime.now());
    if (mapper.updateById(notice) != 1) {
      log.warn("Competition notice publish update conflict, tenantId={}, noticeId={}",
          principal.getTenantId(), noticeId);
      throw new BusinessException(ErrorCode.CONFLICT, "通知状态已发生变化，请刷新后重试");
    }
    outboxApplicationService.enqueueNoticePublished(notice, principal.getUserId());

    CompetitionNoticePublishResultVO result = new CompetitionNoticePublishResultVO();
    result.setNoticeId(notice.getId());
    result.setEventId(notice.getEventId());
    result.setBeforeStatus(beforeStatus);
    result.setAfterStatus(notice.getStatus());
    return result;
  }

  private CompetitionNoticeEntity findExisting(Long tenantId, Long noticeId) {
    CompetitionNoticeEntity notice = mapper.selectOne(new LambdaQueryWrapper<CompetitionNoticeEntity>()
        .eq(CompetitionNoticeEntity::getTenantId, tenantId)
        .eq(CompetitionNoticeEntity::getId, noticeId)
        .eq(CompetitionNoticeEntity::getDeleted, 0));
    if (notice == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
    }
    return notice;
  }

  private CompetitionEventEntity requireAccessibleEvent(
      PlatformUserPrincipal principal, Long eventId) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
          .eq(UserEntity::getTenantId, principal.getTenantId())
          .eq(UserEntity::getId, principal.getUserId())
          .eq(UserEntity::getDeleted, 0));
      if (event.getOwnerOrgId() == null || user == null || user.getOrgId() == null
          || !Objects.equals(user.getOrgId(), event.getOwnerOrgId())
          || !isTenantOrganization(principal.getTenantId(), event.getOwnerOrgId())) {
        log.warn("User attempted notice access outside enterprise event scope, userId={}, tenantId={}, eventId={}",
            principal.getUserId(), principal.getTenantId(), eventId);
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事通知");
      }
    }
    return event;
  }

  private boolean isTenantOrganization(Long tenantId, Long orgId) {
    OrgEntity organization = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, orgId)
        .eq(OrgEntity::getDeleted, 0));
    return organization != null
        && Objects.equals(tenantId, organization.getTenantId())
        && Objects.equals(orgId, organization.getId())
        && Objects.equals(organization.getDeleted(), 0);
  }

  private void validateDraft(String title, String content, String contentType, Integer version) {
    String normalizedTitle = title == null ? null : title.trim();
    if (!StringUtils.hasText(normalizedTitle) || normalizedTitle.length() > MAX_TITLE_LENGTH) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "通知标题不能为空且长度不能超过256个字符");
    }
    String normalizedType = CompetitionNoticeContentType.normalize(contentType);
    int maxLength = CompetitionNoticeContentType.PDF.getCode().equals(normalizedType)
        ? MAX_PDF_DESCRIPTION_LENGTH : MAX_MARKDOWN_LENGTH;
    if (content != null && content.length() > maxLength) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          CompetitionNoticeContentType.PDF.getCode().equals(normalizedType)
              ? "PDF公告简介不能超过2000个字符" : "Markdown公告正文不能超过100000个字符");
    }
    if (version != null && version < 1) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "通知版本必须大于0");
    }
  }

  private void validateForPublish(CompetitionNoticeEntity notice) {
    String type = CompetitionNoticeContentType.normalize(notice.getContentType());
    List<CompetitionNoticeAssetEntity> assets = assetMapper == null
        ? List.of()
        : assetMapper.selectList(new LambdaQueryWrapper<CompetitionNoticeAssetEntity>()
            .eq(CompetitionNoticeAssetEntity::getTenantId, notice.getTenantId())
            .eq(CompetitionNoticeAssetEntity::getNoticeId, notice.getId())
            .eq(CompetitionNoticeAssetEntity::getDeleted, 0));
    if (assets == null) {
      assets = List.of();
    }
    for (CompetitionNoticeAssetEntity asset : assets) {
      if (fileService != null) {
        fileService.getExistingFile(notice.getTenantId(), asset.getFileId());
      }
    }
    if (CompetitionNoticeContentType.PDF.getCode().equals(type)) {
      if (notice.getContent() != null
          && notice.getContent().length() > MAX_PDF_DESCRIPTION_LENGTH) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "PDF公告简介不能超过2000个字符");
      }
      long pdfCount = assets.stream()
          .filter(asset -> CompetitionNoticeAssetType.PDF.getCode().equals(asset.getAssetType()))
          .count();
      if (pdfCount != 1 || assets.size() != 1) {
        throw new BusinessException(
            ErrorCode.UNPROCESSABLE_ENTITY, "PDF公告发布前必须且只能上传一个PDF文件");
      }
      return;
    }
    if (!StringUtils.hasText(notice.getContent())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "Markdown公告正文不能为空");
    }
    if (notice.getContent().length() > MAX_MARKDOWN_LENGTH) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "Markdown公告正文不能超过100000个字符");
    }
    if (assets.stream().anyMatch(
        asset -> !CompetitionNoticeAssetType.INLINE_IMAGE.getCode().equals(asset.getAssetType()))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "Markdown公告不能绑定PDF文件");
    }
    Set<Long> ownedAssetIds = assets.stream()
        .map(CompetitionNoticeAssetEntity::getId)
        .collect(Collectors.toSet());
    if (RAW_HTML_PATTERN.matcher(notice.getContent()).find()) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, "Markdown公告不允许包含原始HTML");
    }
    int searchFrom = 0;
    while (true) {
      int imageStart = notice.getContent().indexOf("![", searchFrom);
      if (imageStart < 0) {
        break;
      }
      Matcher exactMatcher = EXACT_MARKDOWN_IMAGE_PATTERN.matcher(notice.getContent());
      exactMatcher.region(imageStart, notice.getContent().length());
      if (!exactMatcher.lookingAt()
          || !ownedAssetIds.contains(Long.valueOf(exactMatcher.group(1)))) {
        throw new BusinessException(
            ErrorCode.UNPROCESSABLE_ENTITY,
            "Markdown图片必须使用当前公告资产的精确语法");
      }
      searchFrom = exactMatcher.end();
    }
  }

  private String validateStatus(String status) {
    for (CompetitionNoticeStatus value : CompetitionNoticeStatus.values()) {
      if (value.getCode().equals(status)) {
        return status;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "通知状态不合法");
  }
}
