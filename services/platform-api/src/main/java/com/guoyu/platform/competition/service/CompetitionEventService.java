package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionEventConverter;
import com.guoyu.platform.competition.dto.CompetitionEventCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventQueryReq;
import com.guoyu.platform.competition.dto.CompetitionEventUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.vo.CompetitionEventPublishResultVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionEventService {

  private static final SecureRandom ENTRY_CODE_RANDOM = new SecureRandom();

  private final CompetitionEventMapper mapper;
  private final CompetitionEventOrgMapper eventOrgMapper;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;

  public CompetitionEventService(
      CompetitionEventMapper mapper,
      CompetitionEventOrgMapper eventOrgMapper,
      OrgMapper orgMapper,
      UserMapper userMapper) {
    this.mapper = mapper;
    this.eventOrgMapper = eventOrgMapper;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
  }

  @Transactional
  public CompetitionEventEntity create(Long tenantId, CompetitionEventCreateReq req, Long ownerOrgId) {
    validateDateRange(req.startAt(), req.endAt());
    validateRegistrationAuditRequired(req.registrationAuditRequired());
    String scope = validateScope(req.scope());
    if (existsByCode(tenantId, req.code())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛事编码已存在");
    }

    CompetitionEventEntity entity = CompetitionEventConverter.toEntity(req);
    entity.setTenantId(tenantId);
    entity.setScope(scope);
    entity.setOwnerOrgId(ownerOrgId);
    entity.setStatus(CompetitionEventStatus.DRAFT.getCode());
    entity.setRegistrationAuditRequired(
        req.registrationAuditRequired() == null ? 0 : req.registrationAuditRequired());
    entity.setDeleted(0);
    mapper.insert(entity);
    return entity;
  }

  @Transactional(readOnly = true)
  public Page<CompetitionEventEntity> page(Long tenantId, CompetitionEventQueryReq req) {
    return page(tenantId, req, null, false);
  }

  @Transactional(readOnly = true)
  public Page<CompetitionEventEntity> page(
      Long tenantId, CompetitionEventQueryReq req, Long ownerOrgId, boolean restrictInternalScope) {
    LambdaQueryWrapper<CompetitionEventEntity> wrapper = new LambdaQueryWrapper<CompetitionEventEntity>()
        .eq(CompetitionEventEntity::getTenantId, tenantId)
        .orderByDesc(CompetitionEventEntity::getCreatedAt)
        .orderByDesc(CompetitionEventEntity::getId);

    if (restrictInternalScope) {
      if (ownerOrgId == null) {
        wrapper.eq(CompetitionEventEntity::getScope, CompetitionEventScope.PLATFORM.getCode());
      } else {
        wrapper.and(condition -> condition
            .eq(CompetitionEventEntity::getScope, CompetitionEventScope.PLATFORM.getCode())
            .or(internal -> internal
                .eq(CompetitionEventEntity::getScope, CompetitionEventScope.ENTERPRISE_INTERNAL.getCode())
                .eq(CompetitionEventEntity::getOwnerOrgId, ownerOrgId)));
      }
    }

    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(condition -> condition
          .like(CompetitionEventEntity::getName, req.getKeyword())
          .or()
          .like(CompetitionEventEntity::getCode, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getScope())) {
      wrapper.eq(CompetitionEventEntity::getScope, validateScope(req.getScope()));
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CompetitionEventEntity::getStatus, validateStatus(req.getStatus()));
    }

    Page<CompetitionEventEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    if (page == null) {
      log.warn("Competition event mapper returned null page, tenantId={}", tenantId);
      return new Page<>(req.getPage(), req.getPageSize());
    }
    return page;
  }

  @Transactional(readOnly = true)
  public CompetitionEventEntity getById(Long tenantId, Long eventId) {
    return findExisting(tenantId, eventId);
  }

  @Transactional
  public CompetitionEventEntity updateDraft(Long tenantId, Long eventId, CompetitionEventUpdateReq req) {
    CompetitionEventEntity entity = mapper.selectForUpdate(tenantId, eventId);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }
    if (!CompetitionEventStatus.DRAFT.getCode().equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅草稿赛事可修改");
    }
    validateDateRange(req.startAt(), req.endAt());
    validateRegistrationAuditRequired(req.registrationAuditRequired());
    CompetitionEventConverter.applyUpdate(entity, req);
    mapper.updateById(entity);
    return entity;
  }

  @Transactional
  public CompetitionEventPublishResultVO publish(Long tenantId, Long userId, Long eventId) {
    return publish(tenantId, userId, eventId, null);
  }

  @Transactional
  public CompetitionEventPublishResultVO publish(
      Long tenantId, Long userId, Long eventId, PlatformUserPrincipal principal) {
    CompetitionEventEntity event = mapper.selectForUpdate(tenantId, eventId);
    if (event == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }
    if (!CompetitionEventStatus.DRAFT.getCode().equals(event.getStatus())) {
      log.warn("Competition event publish status conflict, tenantId={}, eventId={}, status={}",
          tenantId, eventId, event.getStatus());
      throw new BusinessException(ErrorCode.CONFLICT, "仅草稿赛事可发布");
    }

    validatePublishEvent(event);
    validateOrganizations(tenantId, userId, event, principal);

    String previousStatus = event.getStatus();
    if (event.getPlayerEntryCode() == null || event.getPlayerEntryCode().isBlank()) {
      event.setPlayerEntryCode(newPlayerEntryCode());
    }
    event.setStatus(CompetitionEventStatus.PUBLISHED.getCode());
    event.setUpdatedBy(userId);
    event.setUpdatedAt(LocalDateTime.now());
    if (mapper.updateById(event) != 1) {
      log.warn("Competition event publish update conflict, tenantId={}, eventId={}", tenantId, eventId);
      throw new BusinessException(ErrorCode.CONFLICT, "赛事状态已发生变化，请刷新后重试");
    }

    CompetitionEventPublishResultVO result = new CompetitionEventPublishResultVO();
    result.setEventId(event.getId());
    result.setEventCode(event.getCode());
    result.setPlayerEntryCode(event.getPlayerEntryCode());
    result.setEventScope(event.getScope());
    result.setOwnerOrgId(event.getOwnerOrgId());
    result.setPreviousStatus(previousStatus);
    result.setStatus(event.getStatus());
    return result;
  }

  private boolean existsByCode(Long tenantId, String code) {
    CompetitionEventEntity existing = mapper.selectOne(new LambdaQueryWrapper<CompetitionEventEntity>()
        .eq(CompetitionEventEntity::getTenantId, tenantId)
        .eq(CompetitionEventEntity::getCode, code));
    return existing != null;
  }

  private String newPlayerEntryCode() {
    for (int attempt = 0; attempt < 5; attempt++) {
      byte[] randomBytes = new byte[24];
      ENTRY_CODE_RANDOM.nextBytes(randomBytes);
      String entryCode = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
      if (!mapper.existsByPlayerEntryCode(entryCode)) {
        return entryCode;
      }
    }
    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "选手入口创建失败");
  }

  private CompetitionEventEntity findExisting(Long tenantId, Long eventId) {
    CompetitionEventEntity entity = mapper.selectOne(new LambdaQueryWrapper<CompetitionEventEntity>()
        .eq(CompetitionEventEntity::getTenantId, tenantId)
        .eq(CompetitionEventEntity::getId, eventId));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }
    return entity;
  }

  private String validateScope(String scope) {
    for (CompetitionEventScope value : CompetitionEventScope.values()) {
      if (value.getCode().equals(scope)) {
        return scope;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事范围不合法");
  }

  private String validateStatus(String status) {
    for (CompetitionEventStatus value : CompetitionEventStatus.values()) {
      if (value.getCode().equals(status)) {
        return status;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事状态不合法");
  }

  private void validateRegistrationAuditRequired(Integer value) {
    if (value != null && value != 0 && value != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "报名审核开关必须为0或1");
    }
  }

  private void validateDateRange(java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
    if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事结束时间不能早于开始时间");
    }
  }

  private void validatePublishEvent(CompetitionEventEntity event) {
    if (!StringUtils.hasText(event.getName()) || event.getName().length() > 128
        || !StringUtils.hasText(event.getCode()) || event.getCode().length() > 64) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事名称和编码不能为空");
    }
    if (event.getRegistrationAuditRequired() == null
        || (event.getRegistrationAuditRequired() != 0 && event.getRegistrationAuditRequired() != 1)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事报名审核配置不合法");
    }
    if (event.getDescription() != null && event.getDescription().length() > 2000) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事描述长度不合法");
    }
    validateScopeForPublish(event.getScope());
    if (!isValidDateRange(event.getStartAt(), event.getEndAt())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事结束时间不能早于开始时间");
    }
    if (CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())
        && event.getOwnerOrgId() != null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "平台赛事不能绑定企业内部赛事所属组织");
    }
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())
        && event.getOwnerOrgId() == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "企业内部赛事必须绑定所属组织");
    }
  }

  private void validateOrganizations(
      Long tenantId, Long userId, CompetitionEventEntity event, PlatformUserPrincipal principal) {
    if (CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())) {
      List<CompetitionEventOrgEntity> relations = eventOrgMapper.selectList(
          new LambdaQueryWrapper<CompetitionEventOrgEntity>()
              .eq(CompetitionEventOrgEntity::getTenantId, tenantId)
              .eq(CompetitionEventOrgEntity::getEventId, event.getId())
              .eq(CompetitionEventOrgEntity::getStatus, CompetitionEventOrgStatus.ACTIVE.getCode())
              .eq(CompetitionEventOrgEntity::getDeleted, 0));
      if (relations == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "平台赛事至少需要一个有效参与企业");
      }
      if (principal != null && !PlatformAdminSupport.isSuperAdmin(principal)) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getTenantId, tenantId)
            .eq(UserEntity::getId, userId)
            .eq(UserEntity::getDeleted, 0));
        if (user == null || user.getOrgId() == null) {
          log.warn("Platform event publish user has no data scope, userId={}, tenantId={}, eventId={}",
              userId, tenantId, event.getId());
          throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无赛事数据范围");
        }
        boolean inScope = relations.stream().anyMatch(relation ->
            Objects.equals(relation.getOrgId(), user.getOrgId())
                && isTenantOrganization(tenantId, relation.getOrgId()));
        if (!inScope) {
          log.warn("User attempted to publish platform event outside organization scope, userId={}, tenantId={}, eventId={}",
              userId, tenantId, event.getId());
          throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无赛事数据范围");
        }
      }
      if (relations.stream().anyMatch(relation ->
          relation != null
              && Objects.equals(tenantId, relation.getTenantId())
              && Objects.equals(event.getId(), relation.getEventId())
              && Objects.equals(relation.getDeleted(), 0)
              && CompetitionEventOrgStatus.ACTIVE.getCode().equals(relation.getStatus())
              && isTenantOrganization(tenantId, relation.getOrgId()))) {
        return;
      }
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "平台赛事至少需要一个有效参与企业");
    }

    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, tenantId)
        .eq(UserEntity::getId, userId)
        .eq(UserEntity::getDeleted, 0));
    if (user == null || user.getOrgId() == null
        || !Objects.equals(user.getOrgId(), event.getOwnerOrgId())) {
      log.warn("User attempted to publish enterprise event outside organization scope, userId={}, tenantId={}, eventId={}",
          userId, tenantId, event.getId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权发布该企业内部赛事");
    }
    if (!isTenantOrganization(tenantId, event.getOwnerOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "赛事所属组织不存在或不属于当前租户");
    }
    CompetitionEventOrgEntity relation = eventOrgMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventOrgEntity>()
            .eq(CompetitionEventOrgEntity::getTenantId, tenantId)
            .eq(CompetitionEventOrgEntity::getEventId, event.getId())
            .eq(CompetitionEventOrgEntity::getOrgId, event.getOwnerOrgId())
            .eq(CompetitionEventOrgEntity::getStatus, CompetitionEventOrgStatus.ACTIVE.getCode())
            .eq(CompetitionEventOrgEntity::getDeleted, 0));
    if (relation == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "企业内部赛事所属组织没有有效参与关系");
    }
    if (!Objects.equals(tenantId, relation.getTenantId())
        || !Objects.equals(event.getId(), relation.getEventId())
        || !Objects.equals(event.getOwnerOrgId(), relation.getOrgId())
        || !Objects.equals(relation.getDeleted(), 0)
        || !CompetitionEventOrgStatus.ACTIVE.getCode().equals(relation.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "企业内部赛事所属组织没有有效参与关系");
    }
  }

  private boolean isTenantOrganization(Long tenantId, Long orgId) {
    if (orgId == null || orgMapper == null) {
      return false;
    }
    OrgEntity organization = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, orgId)
        .eq(OrgEntity::getDeleted, 0));
    return organization != null
        && Objects.equals(tenantId, organization.getTenantId())
        && Objects.equals(orgId, organization.getId())
        && Objects.equals(organization.getDeleted(), 0);
  }

  private boolean isValidDateRange(java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
    return startAt == null || endAt == null || !endAt.isBefore(startAt);
  }

  private void validateScopeForPublish(String scope) {
    for (CompetitionEventScope value : CompetitionEventScope.values()) {
      if (value.getCode().equals(scope)) {
        return;
      }
    }
    throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事范围不合法");
  }

}
