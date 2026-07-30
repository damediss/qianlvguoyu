package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionRegistrationConverter;
import com.guoyu.platform.competition.dto.CompetitionRegistrationAuditRequest;
import com.guoyu.platform.competition.dto.CompetitionRegistrationQueryReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationQueryRow;
import com.guoyu.platform.competition.vo.CompetitionRegistrationVO;
import com.guoyu.platform.competition.vo.CompetitionRegistrationAuditResultVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionRegistrationApplicationService {

  private final CompetitionRegistrationService registrationService;
  private final CompetitionEventService eventService;
  private final CompetitionEventOrgMapper eventOrgMapper;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;

  public CompetitionRegistrationApplicationService(
      CompetitionRegistrationService registrationService,
      CompetitionEventService eventService,
      CompetitionEventOrgMapper eventOrgMapper,
      OrgMapper orgMapper,
      UserMapper userMapper) {
    this.registrationService = registrationService;
    this.eventService = eventService;
    this.eventOrgMapper = eventOrgMapper;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionRegistrationVO> page(CompetitionRegistrationQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    applyScope(principal, req);
    var page = registrationService.page(principal.getTenantId(), req);
    List<CompetitionRegistrationVO> items = new ArrayList<>();
    if (page.getRecords() != null) {
      for (CompetitionRegistrationQueryRow row : page.getRecords()) {
        items.add(CompetitionRegistrationConverter.toVO(row));
      }
    }
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CompetitionRegistrationAuditResultVO audit(
      Long registrationId, CompetitionRegistrationAuditRequest req) {
    PlatformUserPrincipal principal = currentUser();
    return registrationService.audit(
        principal.getTenantId(), registrationId, req, principal);
  }

  private void applyScope(PlatformUserPrincipal principal, CompetitionRegistrationQueryReq req) {
    if (PlatformAdminSupport.isSuperAdmin(principal)) {
      if (req.getOrgId() != null) {
        ensureTenantOrganization(principal.getTenantId(), req.getOrgId());
      }
      if (req.getEventId() != null) {
        eventService.getById(principal.getTenantId(), req.getEventId());
      }
      return;
    }

    Long currentOrgId = currentUserOrgId(principal);
    if (req.getOrgId() != null && !currentOrgId.equals(req.getOrgId())) {
      log.warn("User attempted to query registrations for another organization, userId={}, tenantId={}, orgId={}",
          principal.getUserId(), principal.getTenantId(), req.getOrgId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权查询其他企业的参赛资格");
    }
    ensureTenantOrganization(principal.getTenantId(), currentOrgId);
    req.setOrgId(currentOrgId);
    if (req.getEventId() != null) {
      assertEventAccess(principal, req.getEventId(), currentOrgId);
    }
  }

  private void assertEventAccess(PlatformUserPrincipal principal, Long eventId, Long orgId) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      if (!orgId.equals(event.getOwnerOrgId())) {
        log.warn("Registration event access denied for enterprise-internal event, userId={}, tenantId={}, eventId={}, orgId={}",
            principal.getUserId(), principal.getTenantId(), eventId, orgId);
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权查询该赛事的参赛资格");
      }
      return;
    }
    CompetitionEventOrgEntity relation = eventOrgMapper.selectOne(new LambdaQueryWrapper<CompetitionEventOrgEntity>()
        .eq(CompetitionEventOrgEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionEventOrgEntity::getEventId, eventId)
        .eq(CompetitionEventOrgEntity::getOrgId, orgId)
        .eq(CompetitionEventOrgEntity::getStatus, CompetitionEventOrgStatus.ACTIVE.getCode())
        .eq(CompetitionEventOrgEntity::getDeleted, 0));
    if (relation == null) {
      log.warn("Registration event access denied because active organization relation is missing, userId={}, tenantId={}, eventId={}, orgId={}",
          principal.getUserId(), principal.getTenantId(), eventId, orgId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前企业不是有效赛事参与企业");
    }
  }

  private void ensureTenantOrganization(Long tenantId, Long orgId) {
    OrgEntity org = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, orgId)
        .eq(OrgEntity::getDeleted, 0));
    if (org == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "企业不存在或不属于当前租户");
    }
  }

  private Long currentUserOrgId(PlatformUserPrincipal principal) {
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId()));
    if (user == null || user.getOrgId() == null) {
      log.warn("Registration query user has no organization, userId={}, tenantId={}",
          principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无企业数据权限");
    }
    return user.getOrgId();
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition registration query has no authenticated platform user");
      throw ex;
    }
  }
}
