package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionPlayerCreateReq;
import com.guoyu.platform.competition.dto.CompetitionPlayerQueryReq;
import com.guoyu.platform.competition.dto.CompetitionPlayerUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.enums.CompetitionEventMemberRole;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.mapper.CompetitionPlayerQueryRow;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.competition.vo.CompetitionPlayerVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionPlayerManagementService {

  private final CompetitionEventService eventService;
  private final CompetitionEventOrgMapper eventOrgMapper;
  private final CompetitionEventWorkTypeDomainService eventWorkTypeService;
  private final CompetitionRegistrationMapper registrationMapper;
  private final CompetitionRosterItemMapper rosterItemMapper;
  private final CompetitionContestantProfileMapper profileMapper;
  private final CompetitionEventMemberMapper memberMapper;
  private final CompetitionRosterImportService rosterImportService;
  private final CompetitionRegistrationService registrationService;
  private final CompetitionPlayerIdentityDomainService playerIdentityService;
  private final CompetitionContestantScopeChangeGuard scopeChangeGuard;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;

  public CompetitionPlayerManagementService(
      CompetitionEventService eventService,
      CompetitionEventOrgMapper eventOrgMapper,
      CompetitionEventWorkTypeDomainService eventWorkTypeService,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionRosterItemMapper rosterItemMapper,
      CompetitionContestantProfileMapper profileMapper,
      CompetitionEventMemberMapper memberMapper,
      CompetitionRosterImportService rosterImportService,
      CompetitionRegistrationService registrationService,
      CompetitionPlayerIdentityDomainService playerIdentityService,
      CompetitionContestantScopeChangeGuard scopeChangeGuard,
      OrgMapper orgMapper,
      UserMapper userMapper) {
    this.eventService = eventService;
    this.eventOrgMapper = eventOrgMapper;
    this.eventWorkTypeService = eventWorkTypeService;
    this.registrationMapper = registrationMapper;
    this.rosterItemMapper = rosterItemMapper;
    this.profileMapper = profileMapper;
    this.memberMapper = memberMapper;
    this.rosterImportService = rosterImportService;
    this.registrationService = registrationService;
    this.playerIdentityService = playerIdentityService;
    this.scopeChangeGuard = scopeChangeGuard;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionPlayerVO> page(CompetitionPlayerQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    applyQueryScope(principal, req);
    Page<CompetitionPlayerQueryRow> page = registrationMapper.selectPlayerPage(
        new Page<>(req.getPage(), req.getPageSize()), principal.getTenantId(), req.getEventId(),
        req.getOrgId(), req.getEventWorkTypeId(), req.getKeyword(), req.getEmployeeNo(), req.getName(), req.getStatus());
    if (page == null) {
      log.warn("Competition player mapper returned null page, tenantId={}", principal.getTenantId());
      page = new Page<>(req.getPage(), req.getPageSize());
    }
    List<CompetitionPlayerVO> items = new ArrayList<>();
    if (page.getRecords() != null) {
      for (CompetitionPlayerQueryRow row : page.getRecords()) {
        items.add(toVO(row));
      }
    }
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CompetitionPlayerVO create(CompetitionPlayerCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = assertEventOrgAccess(principal, req.eventId(), req.orgId());
    String workTypeName = requireEventWorkType(principal.getTenantId(), event.getId(), req.eventWorkTypeId());
    CompetitionRosterImportEntity batch = rosterImportService.precheckSingle(
        principal.getTenantId(), event.getId(), req.orgId(), req.eventWorkTypeId(), workTypeName,
        req.employeeNo(), req.name(), req.idCardNo(), req.mobile(), null, req.level());
    if (batch == null || !"pending_confirm".equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          batch == null || batch.getSummary() == null ? "选手信息校验失败" : batch.getSummary());
    }
    registrationService.confirmAndGenerate(
        principal.getTenantId(), batch.getId(), principal.getUserId());
    Long rosterItemId = firstValidRosterItemId(principal.getTenantId(), batch.getId());
    CompetitionRegistrationEntity registration = registrationMapper.selectOne(
        new LambdaQueryWrapper<CompetitionRegistrationEntity>()
            .eq(CompetitionRegistrationEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionRegistrationEntity::getEventId, event.getId())
            .eq(CompetitionRegistrationEntity::getRosterItemId, rosterItemId)
            .eq(CompetitionRegistrationEntity::getDeleted, 0));
    if (registration == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "选手报名资格创建失败");
    }
    return toVO(registrationMapper.selectPlayerByRegistrationId(
        principal.getTenantId(), registration.getId()));
  }

  @Transactional
  public CompetitionPlayerVO update(Long registrationId, CompetitionPlayerUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRegistrationEntity registration = findRegistration(principal.getTenantId(), registrationId);
    CompetitionEventEntity event = assertEventOrgAccess(principal, registration.getEventId(), req.orgId());
    String workTypeName = requireEventWorkType(principal.getTenantId(), event.getId(), req.eventWorkTypeId());
    CompetitionRosterItemEntity item = findRosterItem(principal.getTenantId(), registration.getRosterItemId());
    CompetitionContestantProfileEntity profile = profileMapper.selectForUpdateByTenantAndMember(
        principal.getTenantId(), registration.getMemberId());
    if (profile == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手档案不存在");
    }
    boolean scopeChanged = !Objects.equals(registration.getOrgId(), req.orgId())
        || !Objects.equals(profile.getEventWorkTypeId(), req.eventWorkTypeId());
    if (scopeChanged) {
      scopeChangeGuard.assertChangeAllowed(principal.getTenantId(), event.getId(), profile);
    }
    item.setEmployeeNo(StringUtils.hasText(req.employeeNo()) ? req.employeeNo().trim() : item.getEmployeeNo());
    item.setName(StringUtils.hasText(req.name()) ? req.name().trim() : item.getName());
    item.setLevel(StringUtils.hasText(req.level()) ? req.level().trim() : null);
    item.setWorkType(workTypeName);
    item.setUpdatedBy(principal.getUserId());
    if (rosterItemMapper.updateById(item) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手名单已发生变化，请刷新后重试");
    }
    String idCardNo = StringUtils.hasText(req.idCardNo())
        ? req.idCardNo() : findPlayerSensitiveValue(principal.getTenantId(), registration.getPlayerId(), true);
    String mobile = StringUtils.hasText(req.mobile())
        ? req.mobile() : findPlayerSensitiveValue(principal.getTenantId(), registration.getPlayerId(), false);
    playerIdentityService.updateSensitiveData(
        principal.getTenantId(), registration.getPlayerId(), idCardNo, mobile);
    registration.setOrgId(req.orgId());
    registration.setUpdatedBy(principal.getUserId());
    if (registrationMapper.updateById(registration) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手参赛企业已发生变化，请刷新后重试");
    }
    profile.setEventWorkTypeId(req.eventWorkTypeId());
    profile.setWorkTypeMatchStatus("matched");
    profile.setWorkTypeMatchNote("后台选手管理维护");
    profile.setUpdatedBy(principal.getUserId());
    if (profileMapper.updateById(profile) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手赛事工种已发生变化，请刷新后重试");
    }
    return toVO(registrationMapper.selectPlayerByRegistrationId(
        principal.getTenantId(), registrationId));
  }

  @Transactional
  public void delete(Long registrationId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRegistrationEntity registration = findRegistration(principal.getTenantId(), registrationId);
    CompetitionEventEntity event = assertEventOrgAccess(principal, registration.getEventId(), registration.getOrgId());
    CompetitionContestantProfileEntity profile = profileMapper.selectForUpdateByTenantAndMember(
        principal.getTenantId(), registration.getMemberId());
    if (profile != null) {
      scopeChangeGuard.assertChangeAllowed(principal.getTenantId(), event.getId(), profile);
      profileMapper.deleteById(profile.getId());
    }
    if (registration.getMemberId() != null) {
      memberMapper.deleteById(registration.getMemberId());
    }
    if (registrationMapper.deleteById(registrationId) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手已被其他操作删除，请刷新后重试");
    }
    if (registration.getRosterItemId() != null) {
      rosterItemMapper.deleteById(registration.getRosterItemId());
    }
  }

  private void applyQueryScope(PlatformUserPrincipal principal, CompetitionPlayerQueryReq req) {
    if (PlatformAdminSupport.isSuperAdmin(principal)) {
      if (req.getEventId() != null) {
        eventService.getById(principal.getTenantId(), req.getEventId());
      }
      if (req.getOrgId() != null) {
        ensureOrganization(principal.getTenantId(), req.getOrgId());
      }
      if (req.getEventWorkTypeId() != null && req.getEventId() != null) {
        requireEventWorkType(principal.getTenantId(), req.getEventId(), req.getEventWorkTypeId());
      }
      return;
    }
    Long currentOrgId = currentUserOrgId(principal);
    if (req.getOrgId() != null && !currentOrgId.equals(req.getOrgId())) {
      log.warn("Competition player query crossed organization scope, userId={}, eventId={}, orgId={}",
          principal.getUserId(), req.getEventId(), req.getOrgId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权查询其他企业的选手");
    }
    req.setOrgId(currentOrgId);
    if (req.getEventId() != null) {
      assertEventOrgAccess(principal, req.getEventId(), currentOrgId);
      if (req.getEventWorkTypeId() != null) {
        requireEventWorkType(principal.getTenantId(), req.getEventId(), req.getEventWorkTypeId());
      }
    }
  }

  private CompetitionEventEntity assertEventOrgAccess(
      PlatformUserPrincipal principal, Long eventId, Long orgId) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    ensureOrganization(principal.getTenantId(), orgId);
    if (PlatformAdminSupport.isSuperAdmin(principal)) {
      return event;
    }
    Long currentOrgId = currentUserOrgId(principal);
    if (!currentOrgId.equals(orgId)) {
      log.warn("Competition player write crossed organization scope, userId={}, eventId={}, orgId={}",
          principal.getUserId(), eventId, orgId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅允许维护本企业选手");
    }
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())
        && !orgId.equals(event.getOwnerOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权维护该赛事选手");
    }
    CompetitionEventOrgEntity relation = eventOrgMapper.selectOne(new LambdaQueryWrapper<CompetitionEventOrgEntity>()
        .eq(CompetitionEventOrgEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionEventOrgEntity::getEventId, eventId)
        .eq(CompetitionEventOrgEntity::getOrgId, orgId)
        .ne(CompetitionEventOrgEntity::getStatus, CompetitionEventOrgStatus.CLOSED.getCode())
        .eq(CompetitionEventOrgEntity::getDeleted, 0));
    if (relation == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前企业不是该赛事参与企业");
    }
    return event;
  }

  private String requireEventWorkType(Long tenantId, Long eventId, Long eventWorkTypeId) {
    var workType = eventWorkTypeService.getById(tenantId, eventWorkTypeId);
    if (!Objects.equals(eventId, workType.getEventId()) || !"active".equals(workType.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种无效或不属于当前赛事");
    }
    return workType.getDisplayName();
  }

  private CompetitionRegistrationEntity findRegistration(Long tenantId, Long registrationId) {
    CompetitionRegistrationEntity registration = registrationMapper.selectForUpdate(tenantId, registrationId);
    if (registration == null || registration.getPlayerId() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手参赛记录不存在");
    }
    return registration;
  }

  private CompetitionRosterItemEntity findRosterItem(Long tenantId, Long itemId) {
    CompetitionRosterItemEntity item = rosterItemMapper.selectOne(new LambdaQueryWrapper<CompetitionRosterItemEntity>()
        .eq(CompetitionRosterItemEntity::getTenantId, tenantId)
        .eq(CompetitionRosterItemEntity::getId, itemId)
        .eq(CompetitionRosterItemEntity::getDeleted, 0));
    if (item == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手名单记录不存在");
    }
    return item;
  }

  private Long firstValidRosterItemId(Long tenantId, Long batchId) {
    CompetitionRosterItemEntity item = rosterItemMapper.selectOne(new LambdaQueryWrapper<CompetitionRosterItemEntity>()
        .eq(CompetitionRosterItemEntity::getTenantId, tenantId)
        .eq(CompetitionRosterItemEntity::getBatchId, batchId)
        .eq(CompetitionRosterItemEntity::getValidationStatus, "valid")
        .eq(CompetitionRosterItemEntity::getDeleted, 0)
        .last("LIMIT 1"));
    if (item == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "选手名单记录创建失败");
    }
    return item.getId();
  }

  private String findPlayerSensitiveValue(Long tenantId, Long playerId, boolean idCard) {
    var player = playerIdentityService.resolveExisting(tenantId, playerId);
    return idCard ? player.getIdCardNo() : player.getMobile();
  }

  private void ensureOrganization(Long tenantId, Long orgId) {
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
        .eq(UserEntity::getId, principal.getUserId())
        .eq(UserEntity::getDeleted, 0));
    if (user == null || user.getOrgId() == null) {
      log.warn("Competition player user has no organization, userId={}, tenantId={}",
          principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无企业数据权限");
    }
    return user.getOrgId();
  }

  private CompetitionPlayerVO toVO(CompetitionPlayerQueryRow row) {
    if (row == null) return null;
    CompetitionPlayerVO vo = new CompetitionPlayerVO();
    vo.setRegistrationId(row.getRegistrationId());
    vo.setEventId(row.getEventId());
    vo.setEventName(row.getEventName());
    vo.setOrgId(row.getOrgId());
    vo.setOrgName(row.getOrgName());
    vo.setPlayerId(row.getPlayerId());
    vo.setMemberId(row.getMemberId());
    vo.setRosterItemId(row.getRosterItemId());
    vo.setEventWorkTypeId(row.getEventWorkTypeId());
    vo.setEventWorkTypeName(row.getEventWorkTypeName() == null ? row.getWorkType() : row.getEventWorkTypeName());
    vo.setEmployeeNo(row.getEmployeeNo());
    vo.setName(row.getName());
    vo.setIdCardNo(maskIdCard(row.getIdCardNo()));
    vo.setMobile(maskMobile(row.getMobile()));
    vo.setLevel(row.getLevel());
    vo.setStatus(row.getStatus());
    vo.setPlayerStatus(row.getPlayerStatus());
    vo.setCreatedAt(row.getCreatedAt());
    return vo;
  }

  private String maskIdCard(String idCardNo) {
    if (!StringUtils.hasText(idCardNo)) return "-";
    return idCardNo.length() <= 8 ? "********" : idCardNo.substring(0, 3) + "********" + idCardNo.substring(idCardNo.length() - 4);
  }

  private String maskMobile(String mobile) {
    if (!StringUtils.hasText(mobile)) return "-";
    return mobile.length() < 7 ? "****" : mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition player management request has no authenticated platform user");
      throw ex;
    }
  }

}
