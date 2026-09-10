package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionEventConverter;
import com.guoyu.platform.competition.dto.CompetitionEventCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventQueryReq;
import com.guoyu.platform.competition.dto.CompetitionEventUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.vo.CompetitionEventVO;
import com.guoyu.platform.competition.vo.CompetitionEventPublishResultVO;
import com.guoyu.platform.competition.vo.CompetitionEventPublishReadinessVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionEventApplicationService {

  private final CompetitionEventService eventService;
  private final UserMapper userMapper;
  private final CompetitionEventPublishReadinessService publishReadinessService;

  public CompetitionEventApplicationService(CompetitionEventService eventService, UserMapper userMapper) {
    this(eventService, userMapper, null);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public CompetitionEventApplicationService(
      CompetitionEventService eventService,
      UserMapper userMapper,
      CompetitionEventPublishReadinessService publishReadinessService) {
    this.eventService = eventService;
    this.userMapper = userMapper;
    this.publishReadinessService = publishReadinessService;
  }

  @Transactional
  public CompetitionEventVO create(CompetitionEventCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    Long tenantId = principal.getTenantId();
    String scope = req.scope();
    Long ownerOrgId;
    if (CompetitionEventScope.PLATFORM.getCode().equals(scope)) {
      if (!PlatformAdminSupport.isSuperAdmin(principal)) {
        log.warn("Non-platform admin attempted to create platform competition event, userId={}, tenantId={}",
            principal.getUserId(), tenantId);
        throw new BusinessException(ErrorCode.FORBIDDEN, "仅平台管理员可创建平台赛事");
      }
      ownerOrgId = null;
    } else if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(scope)) {
      ownerOrgId = currentUserOrgId(principal);
    } else {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事范围不合法");
    }

    return toVO(eventService.create(tenantId, req, ownerOrgId));
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionEventVO> page(CompetitionEventQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    Long tenantId = principal.getTenantId();
    var page = PlatformAdminSupport.isSuperAdmin(principal)
        ? eventService.page(tenantId, req)
        : eventService.page(tenantId, req, currentUserOrgIdOrNull(principal), true);
    var items = page.getRecords() == null
        ? java.util.List.<CompetitionEventVO>of()
        : page.getRecords().stream().map(CompetitionEventConverter::toVO).toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public CompetitionEventVO getById(Long eventId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    assertEventScope(principal, event);
    return toVO(event);
  }

  @Transactional
  public CompetitionEventVO update(Long eventId, CompetitionEventUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    assertEventScope(principal, eventService.getById(principal.getTenantId(), eventId));
    return toVO(eventService.updateDraft(principal.getTenantId(), eventId, req));
  }

  @Transactional
  public CompetitionEventPublishResultVO publish(Long eventId) {
    PlatformUserPrincipal principal = currentUser();
    assertEventScope(principal, eventService.getById(principal.getTenantId(), eventId));
    return eventService.publish(principal.getTenantId(), principal.getUserId(), eventId, principal);
  }

  @Transactional(readOnly = true)
  public CompetitionEventPublishReadinessVO publishReadiness(Long eventId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    assertEventScope(principal, event);
    if (publishReadinessService == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "赛事发布校验器未启用");
    }
    return publishReadinessService.evaluate(
        principal.getTenantId(), principal.getUserId(), event, principal);
  }

  private void assertEventScope(PlatformUserPrincipal principal, CompetitionEventEntity event) {
    if (PlatformAdminSupport.isSuperAdmin(principal)
        || !CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      return;
    }
    Long orgId = currentUserOrgIdOrNull(principal);
    if (orgId == null || !orgId.equals(event.getOwnerOrgId())) {
      log.warn("User attempted to access competition event outside organization scope, userId={}, tenantId={}, eventId={}",
          principal.getUserId(), principal.getTenantId(), event.getId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
    }
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition event request has no authenticated platform user");
      throw ex;
    }
  }

  private Long currentUserOrgId(PlatformUserPrincipal principal) {
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId()));
    if (user == null || user.getOrgId() == null) {
      log.warn("Competition event creator has no enterprise organization, userId={}, tenantId={}",
          principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前用户未绑定企业组织，无法创建企业内部赛事");
    }
    return user.getOrgId();
  }

  private Long currentUserOrgIdOrNull(PlatformUserPrincipal principal) {
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId())
        .eq(UserEntity::getDeleted, 0));
    return user == null ? null : user.getOrgId();
  }

  private CompetitionEventVO toVO(CompetitionEventEntity entity) {
    return CompetitionEventConverter.toVO(entity);
  }
}
