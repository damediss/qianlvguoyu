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
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionNoticeStatus;
import com.guoyu.platform.competition.mapper.CompetitionNoticeMapper;
import com.guoyu.platform.competition.vo.CompetitionNoticePublishResultVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionNoticeService {

  private static final int MAX_TITLE_LENGTH = 256;
  private static final int MAX_CONTENT_LENGTH = 16000;

  private final CompetitionNoticeMapper mapper;
  private final CompetitionEventService eventService;
  private final UserMapper userMapper;
  private final OrgMapper orgMapper;

  public CompetitionNoticeService(
      CompetitionNoticeMapper mapper,
      CompetitionEventService eventService,
      UserMapper userMapper,
      OrgMapper orgMapper) {
    this.mapper = mapper;
    this.eventService = eventService;
    this.userMapper = userMapper;
    this.orgMapper = orgMapper;
  }

  @Transactional
  public CompetitionNoticeEntity create(
      PlatformUserPrincipal principal, Long eventId, CompetitionNoticeCreateReq req) {
    requireAccessibleEvent(principal, eventId);
    validateContent(req.title(), req.content(), req.version());

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
    validateContent(req.title(), req.content(), req.version());
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

  private void validateContent(String title, String content, Integer version) {
    String normalizedTitle = title == null ? null : title.trim();
    if (!StringUtils.hasText(normalizedTitle) || normalizedTitle.length() > MAX_TITLE_LENGTH) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "通知标题不能为空且长度不能超过256个字符");
    }
    if (!StringUtils.hasText(content) || content.length() > MAX_CONTENT_LENGTH) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "通知内容不能为空且长度不能超过16000个字符");
    }
    if (version != null && version < 1) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "通知版本必须大于0");
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
