package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionEventOrgConverter;
import com.guoyu.platform.competition.dto.CompetitionEventOrgCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventOrgQueryReq;
import com.guoyu.platform.competition.dto.CompetitionEventOrgStatusUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.vo.CompetitionEventOrgVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionEventOrgApplicationService {

  private final CompetitionEventService eventService;
  private final CompetitionEventOrgService eventOrgService;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;

  public CompetitionEventOrgApplicationService(
      CompetitionEventService eventService,
      CompetitionEventOrgService eventOrgService,
      OrgMapper orgMapper,
      UserMapper userMapper) {
    this.eventService = eventService;
    this.eventOrgService = eventOrgService;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionEventOrgVO> page(Long eventId, CompetitionEventOrgQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId);
    if (!PlatformAdminSupport.isSuperAdmin(principal)) {
      req.setOrgId(currentUserOrgId(principal));
    }
    var page = eventOrgService.page(principal.getTenantId(), event.getId(), req);
    List<CompetitionEventOrgVO> items = new ArrayList<>();
    Map<Long, OrgEntity> organizations = new HashMap<>();
    if (page.getRecords() != null && !page.getRecords().isEmpty()) {
      List<Long> orgIds = page.getRecords().stream().map(CompetitionEventOrgEntity::getOrgId).distinct().toList();
      List<OrgEntity> orgs = orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
          .eq(OrgEntity::getTenantId, principal.getTenantId())
          .in(OrgEntity::getId, orgIds)
          .eq(OrgEntity::getDeleted, 0));
      if (orgs != null) orgs.forEach(org -> organizations.put(org.getId(), org));
      for (CompetitionEventOrgEntity relation : page.getRecords()) {
        OrgEntity org = organizations.get(relation.getOrgId());
        items.add(CompetitionEventOrgConverter.toVO(
            relation,
            org == null ? null : org.getCode(),
            org == null ? null : org.getName()));
      }
    }
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CompetitionEventOrgVO create(Long eventId, CompetitionEventOrgCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId);
    if (!CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())) {
      log.warn("Attempted to add organization to enterprise-internal event, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "企业内部赛事不能配置其他企业");
    }
    ensureTenantOrganization(principal.getTenantId(), req.orgId());
    return toVO(eventOrgService.create(principal.getTenantId(), event.getId(), req), principal.getTenantId());
  }

  @Transactional
  public CompetitionEventOrgVO updateStatus(
      Long eventId, Long orgId, CompetitionEventOrgStatusUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId);
    CompetitionEventOrgEntity relation = eventOrgService.updateStatus(
        principal.getTenantId(), event.getId(), orgId, req);
    return toVO(relation, principal.getTenantId());
  }

  private CompetitionEventEntity accessibleEvent(PlatformUserPrincipal principal, Long eventId) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())
        && !currentUserOrgId(principal).equals(event.getOwnerOrgId())) {
      log.warn("User attempted to access an event outside organization scope, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
    }
    return event;
  }

  private void ensureTenantOrganization(Long tenantId, Long orgId) {
    OrgEntity organization = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, orgId)
        .eq(OrgEntity::getDeleted, 0));
    if (organization == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "参与企业不存在或不属于当前租户");
    }
  }

  private String findOrgName(Long tenantId, Long orgId) {
    OrgEntity organization = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, orgId)
        .eq(OrgEntity::getDeleted, 0));
    return organization == null ? null : organization.getName();
  }

  private Long currentUserOrgId(PlatformUserPrincipal principal) {
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId()));
    if (user == null || user.getOrgId() == null) {
      log.warn("Competition organization access has no user organization, userId={}, tenantId={}",
          principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无企业数据权限");
    }
    return user.getOrgId();
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition organization request has no authenticated platform user");
      throw ex;
    }
  }

  private CompetitionEventOrgVO toVO(CompetitionEventOrgEntity relation, Long tenantId) {
    OrgEntity org = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, relation.getOrgId())
        .eq(OrgEntity::getDeleted, 0));
    return CompetitionEventOrgConverter.toVO(
        relation,
        org == null ? null : org.getCode(),
        org == null ? null : org.getName());
  }
}
