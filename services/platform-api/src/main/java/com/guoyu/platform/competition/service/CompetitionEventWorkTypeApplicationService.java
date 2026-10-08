package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionEventWorkTypeConverter;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.vo.CompetitionEventWorkTypeVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionEventWorkTypeApplicationService {

  private final CompetitionEventService eventService;
  private final CompetitionEventWorkTypeDomainService domainService;
  private final CompetitionEventOrgMapper eventOrgMapper;
  private final UserMapper userMapper;

  public CompetitionEventWorkTypeApplicationService(
      CompetitionEventService eventService,
      CompetitionEventWorkTypeDomainService domainService,
      CompetitionEventOrgMapper eventOrgMapper,
      UserMapper userMapper) {
    this.eventService = eventService;
    this.domainService = domainService;
    this.eventOrgMapper = eventOrgMapper;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionEventWorkTypeVO> list(Long eventId) {
    PlatformUserPrincipal principal = currentUser();
    accessibleEvent(principal, eventId, false);
    List<CompetitionEventWorkTypeVO> result = new ArrayList<>();
    for (CompetitionEventWorkTypeEntity entity :
        domainService.list(principal.getTenantId(), eventId)) {
      result.add(toVO(principal.getTenantId(), entity));
    }
    return result;
  }

  @Transactional(readOnly = true)
  public CompetitionEventWorkTypeVO get(Long eventId, Long eventWorkTypeId) {
    PlatformUserPrincipal principal = currentUser();
    accessibleEvent(principal, eventId, false);
    CompetitionEventWorkTypeEntity entity = domainService.getById(principal.getTenantId(), eventWorkTypeId);
    if (entity == null || !Objects.equals(eventId, entity.getEventId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    }
    return toVO(principal.getTenantId(), entity);
  }

  public void assertAccessible(Long eventId, boolean manage) {
    accessibleEvent(currentUser(), eventId, manage);
  }

  @Transactional
  public CompetitionEventWorkTypeVO create(
      Long eventId,
      CompetitionEventWorkTypeCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, true);
    requireDraftEvent(event);
    event = lockedDraftOrFallback(principal, eventId, event);
    return toVO(principal.getTenantId(),
        domainService.create(principal.getTenantId(), eventId, req));
  }

  @Transactional
  public CompetitionEventWorkTypeVO update(
      Long eventId,
    Long eventWorkTypeId,
      CompetitionEventWorkTypeUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, true);
    requireDraftEvent(event);
    event = lockedDraftOrFallback(principal, eventId, event);
    return toVO(principal.getTenantId(), domainService.update(
        principal.getTenantId(), eventId, eventWorkTypeId, req));
  }

  @Transactional
  public void delete(Long eventId, Long eventWorkTypeId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, true);
    requireDraftEvent(event);
    lockedDraftOrFallback(principal, eventId, event);
    domainService.remove(principal.getTenantId(), eventId, eventWorkTypeId);
  }

  private CompetitionEventEntity lockedDraftOrFallback(
      PlatformUserPrincipal principal, Long eventId, CompetitionEventEntity fallback) {
    CompetitionEventEntity locked = eventService.lockForUpdate(principal.getTenantId(), eventId);
    if (locked == null) {
      return fallback;
    }
    requireDraftEvent(locked);
    return locked;
  }

  private void requireDraftEvent(CompetitionEventEntity event) {
    com.guoyu.platform.competition.domain.CompetitionEventArrangementGuard.requireOpen(
        event, "已关闭或已归档赛事不允许修改参赛工种");
  }

  private CompetitionEventWorkTypeVO toVO(Long tenantId, CompetitionEventWorkTypeEntity entity) {
    String sourceName = domainService.findActiveSourceName(tenantId, entity.getWorkTypeId());
    return CompetitionEventWorkTypeConverter.toVO(entity, sourceName);
  }

  private CompetitionEventEntity accessibleEvent(
      PlatformUserPrincipal principal,
      Long eventId,
      boolean manage) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (PlatformAdminSupport.isSuperAdmin(principal)) {
      return event;
    }
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId())
        .eq(UserEntity::getDeleted, 0));
    if (user == null || user.getOrgId() == null) {
      log.warn("Event work type access outside organization scope, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
    }
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      if (Objects.equals(user.getOrgId(), event.getOwnerOrgId())) {
        return event;
      }
      log.warn("Event work type access outside owner organization, eventId={}, userId={}, orgId={}",
          eventId, principal.getUserId(), user.getOrgId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
    }
    if (manage) {
      log.warn("Enterprise participant attempted to manage platform event work types, eventId={}, userId={}, orgId={}",
          eventId, principal.getUserId(), user.getOrgId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "平台赛事工种仅平台管理员可管理");
    }
    CompetitionEventOrgEntity relation = eventOrgMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventOrgEntity>()
            .eq(CompetitionEventOrgEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionEventOrgEntity::getEventId, eventId)
            .eq(CompetitionEventOrgEntity::getOrgId, user.getOrgId())
            .eq(CompetitionEventOrgEntity::getStatus,
                CompetitionEventOrgStatus.ACTIVE.getCode())
            .eq(CompetitionEventOrgEntity::getDeleted, 0));
    if (relation != null) {
      return event;
    }
    log.warn("Event work type access without active event organization, eventId={}, userId={}, orgId={}",
        eventId, principal.getUserId(), user.getOrgId());
    throw new BusinessException(ErrorCode.FORBIDDEN, "当前企业不是有效赛事参与企业");
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition event work type request has no authenticated platform user");
      throw ex;
    }
  }
}
