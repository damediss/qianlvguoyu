package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionContestantWorkTypeConfirmReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.vo.CompetitionContestantWorkTypeReconciliationVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionContestantWorkTypeApplicationService {

  private final CompetitionEventService eventService;
  private final CompetitionContestantWorkTypeDomainService domainService;
  private final CompetitionEventOrgMapper eventOrgMapper;
  private final UserMapper userMapper;

  public CompetitionContestantWorkTypeApplicationService(
      CompetitionEventService eventService,
      CompetitionContestantWorkTypeDomainService domainService,
      CompetitionEventOrgMapper eventOrgMapper,
      UserMapper userMapper) {
    this.eventService = eventService;
    this.domainService = domainService;
    this.eventOrgMapper = eventOrgMapper;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionContestantWorkTypeReconciliationVO> reconcile(Long eventId) {
    PlatformUserPrincipal principal = currentUser();
    EventAccess access = accessibleEvent(principal, eventId);
    return domainService.reconcile(principal.getTenantId(), eventId, access.orgId());
  }

  @Transactional
  public CompetitionContestantWorkTypeReconciliationVO confirm(
      Long eventId,
      Long contestantMemberId,
      CompetitionContestantWorkTypeConfirmReq req) {
    PlatformUserPrincipal principal = currentUser();
    EventAccess access = accessibleEvent(principal, eventId);
    return domainService.confirm(
        principal.getTenantId(),
        eventId,
        contestantMemberId,
        principal.getUserId(),
        req,
        access.orgId());
  }

  private EventAccess accessibleEvent(
      PlatformUserPrincipal principal,
      Long eventId) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (PlatformAdminSupport.isSuperAdmin(principal)) {
      return new EventAccess(event, null);
    }
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId())
        .eq(UserEntity::getDeleted, 0));
    if (user == null || user.getOrgId() == null) {
      log.warn("Contestant work type access outside organization scope, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
    }
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      if (Objects.equals(user.getOrgId(), event.getOwnerOrgId())) {
        return new EventAccess(event, user.getOrgId());
      }
      log.warn("Contestant work type access outside owner organization, eventId={}, userId={}, orgId={}",
          eventId, principal.getUserId(), user.getOrgId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
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
      return new EventAccess(event, user.getOrgId());
    }
    log.warn("Contestant work type access without active event organization, eventId={}, userId={}, orgId={}",
        eventId, principal.getUserId(), user.getOrgId());
    throw new BusinessException(ErrorCode.FORBIDDEN, "当前企业不是有效赛事参与企业");
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition contestant work type request has no authenticated platform user");
      throw ex;
    }
  }

  private record EventAccess(CompetitionEventEntity event, Long orgId) {
  }
}
