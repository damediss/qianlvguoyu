package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionScoreRuleConverter;
import com.guoyu.platform.competition.dto.CompetitionScoreRuleCreateReq;
import com.guoyu.platform.competition.dto.CompetitionScoreRuleUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.vo.CompetitionScoreRuleVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionScoreRuleApplicationService {

  private final CompetitionScoreRuleDomainService scoreRuleDomainService;
  private final CompetitionEventService eventService;
  private final CompetitionEventOrgMapper eventOrgMapper;
  private final UserMapper userMapper;

  public CompetitionScoreRuleApplicationService(
      CompetitionScoreRuleDomainService scoreRuleDomainService,
      CompetitionEventService eventService,
      CompetitionEventOrgMapper eventOrgMapper,
      UserMapper userMapper) {
    this.scoreRuleDomainService = scoreRuleDomainService;
    this.eventService = eventService;
    this.eventOrgMapper = eventOrgMapper;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionScoreRuleVO> list(
      Long eventId, Long stageId, Long eventWorkTypeId) {
    PlatformUserPrincipal principal = currentUser();
    accessibleEvent(principal, eventId);
    List<CompetitionScoreRuleVO> result = new ArrayList<>();
    scoreRuleDomainService.list(
            principal.getTenantId(), eventId, stageId, eventWorkTypeId)
        .forEach(rule -> result.add(CompetitionScoreRuleConverter.toVO(rule)));
    return result;
  }

  @Transactional
  public CompetitionScoreRuleVO create(Long eventId, CompetitionScoreRuleCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    manageableEvent(principal, eventId);
    return CompetitionScoreRuleConverter.toVO(scoreRuleDomainService.create(
        principal.getTenantId(), principal.getUserId(), eventId, req));
  }

  @Transactional
  public CompetitionScoreRuleVO update(Long ruleId, CompetitionScoreRuleUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionScoreRuleEntity rule =
        scoreRuleDomainService.getById(principal.getTenantId(), ruleId);
    manageableEvent(principal, rule.getEventId());
    return CompetitionScoreRuleConverter.toVO(scoreRuleDomainService.update(
        principal.getTenantId(), principal.getUserId(), ruleId, req));
  }

  @Transactional
  public CompetitionScoreRuleVO publish(Long ruleId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionScoreRuleEntity rule =
        scoreRuleDomainService.getById(principal.getTenantId(), ruleId);
    manageableEvent(principal, rule.getEventId());
    return CompetitionScoreRuleConverter.toVO(scoreRuleDomainService.publish(
        principal.getTenantId(), principal.getUserId(), ruleId));
  }

  CompetitionEventEntity accessibleEvent(PlatformUserPrincipal principal, Long eventId) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (PlatformAdminSupport.isSuperAdmin(principal)) {
      return event;
    }
    Long orgId = currentUserOrgId(principal);
    boolean allowed;
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      allowed = orgId.equals(event.getOwnerOrgId());
    } else if (CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())) {
      CompetitionEventOrgEntity relation = eventOrgMapper.selectOne(
          new LambdaQueryWrapper<CompetitionEventOrgEntity>()
              .eq(CompetitionEventOrgEntity::getTenantId, principal.getTenantId())
              .eq(CompetitionEventOrgEntity::getEventId, eventId)
              .eq(CompetitionEventOrgEntity::getOrgId, orgId)
              .eq(CompetitionEventOrgEntity::getStatus, CompetitionEventOrgStatus.ACTIVE.getCode())
              .eq(CompetitionEventOrgEntity::getDeleted, 0));
      allowed = relation != null;
    } else {
      allowed = false;
    }
    if (!allowed) {
      log.warn("User attempted score rule access outside event data scope, tenantId={}, userId={}, eventId={}",
          principal.getTenantId(), principal.getUserId(), eventId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事评分规则");
    }
    return event;
  }

  CompetitionEventEntity manageableEvent(
      PlatformUserPrincipal principal, Long eventId) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (PlatformAdminSupport.isSuperAdmin(principal)) {
      return event;
    }
    if (CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())) {
      log.warn("Non-platform admin attempted to manage platform event score rule, tenantId={}, "
              + "userId={}, eventId={}",
          principal.getTenantId(), principal.getUserId(), eventId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅平台管理员可管理平台赛事评分规则");
    }
    Long orgId = currentUserOrgId(principal);
    if (!CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())
        || !orgId.equals(event.getOwnerOrgId())) {
      log.warn("User attempted to manage score rule outside event data scope, tenantId={}, "
              + "userId={}, eventId={}",
          principal.getTenantId(), principal.getUserId(), eventId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理该赛事评分规则");
    }
    return event;
  }

  private Long currentUserOrgId(PlatformUserPrincipal principal) {
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId())
        .eq(UserEntity::getDeleted, 0));
    if (user == null || user.getOrgId() == null) {
      log.warn("Competition score rule access has no organization, tenantId={}, userId={}",
          principal.getTenantId(), principal.getUserId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无赛事数据范围");
    }
    return user.getOrgId();
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition score rule request has no authenticated platform user");
      throw ex;
    }
  }
}
