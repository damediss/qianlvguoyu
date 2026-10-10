package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.util.SensitiveMaskUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionPlayerCreateReq;
import com.guoyu.platform.competition.dto.CompetitionPlayerQueryReq;
import com.guoyu.platform.competition.dto.CompetitionPlayerUpdateReq;
import com.guoyu.platform.competition.dto.CompetitionPlayerWorkTypeReq;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
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
import com.guoyu.platform.competition.vo.CompetitionPlayerWorkTypeVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final CompetitionEventWorkTypeLevelService eventWorkTypeLevelService;

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
    this(eventService, eventOrgMapper, eventWorkTypeService, registrationMapper, rosterItemMapper,
        profileMapper, memberMapper, rosterImportService, registrationService, playerIdentityService,
        scopeChangeGuard, orgMapper, userMapper, null);
  }

  @Autowired
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
      UserMapper userMapper,
      CompetitionEventWorkTypeLevelService eventWorkTypeLevelService) {
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
    this.eventWorkTypeLevelService = eventWorkTypeLevelService;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionPlayerVO> page(CompetitionPlayerQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    applyQueryScope(principal, req);
    Page<CompetitionPlayerQueryRow> page = registrationMapper.selectPlayerPage(
        new Page<>(req.getPage(), req.getPageSize()), principal.getTenantId(), req.getEventId(),
        req.getOrgId(), req.getEventWorkTypeId(), req.getKeyword(), req.getName(), req.getStatus());
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

  @Transactional(readOnly = true)
  public CompetitionPlayerVO getForEdit(Long registrationId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionPlayerQueryRow row = registrationMapper.selectPlayerByRegistrationId(
        principal.getTenantId(), registrationId);
    if (row == null || row.getPlayerId() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手参赛记录不存在");
    }
    assertEventOrgAccess(principal, row.getEventId(), row.getOrgId());
    return toVO(row, true);
  }

  @Transactional
  public CompetitionPlayerVO create(CompetitionPlayerCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = assertEventOrgAccess(principal, req.eventId(), req.orgId());
    CompetitionRosterImportEntity batch;
    if (req.workTypes() == null) {
      // Keep the original payload working for older clients during rollout.
      String workTypeName = requireEventWorkType(
          principal.getTenantId(), event.getId(), req.eventWorkTypeId());
      batch = rosterImportService.precheckSingle(
          principal.getTenantId(), event.getId(), req.orgId(), req.eventWorkTypeId(), workTypeName,
          req.name(), req.idCardNo(), req.mobile(), null, req.level());
    } else {
      List<ResolvedWorkType> selections = resolveWorkTypes(
          principal, event, req.workTypes(), req.eventWorkTypeId(), req.level());
      batch = rosterImportService.precheckSingleMulti(
          principal.getTenantId(), event.getId(), req.orgId(), req.name(),
          req.idCardNo(), req.mobile(), selections.stream()
              .map(selection -> new CompetitionRosterImportService.SingleWorkTypeSelection(
                  selection.eventWorkTypeId(), selection.workTypeName(), selection.levelId(),
                  selection.levelCode(), selection.levelName()))
              .toList());
    }
    if (batch == null || !"pending_confirm".equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          batch == null
              ? "选手信息校验失败"
              : validationFailureMessage(principal.getTenantId(), batch.getId(), batch.getSummary()));
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
    if (req.workTypes() != null) {
      return updateMultipleWorkTypes(principal, registration, event, req);
    }
    String workTypeName = requireEventWorkType(principal.getTenantId(), event.getId(), req.eventWorkTypeId());
    CompetitionRosterItemEntity item = findRosterItem(principal.getTenantId(), registration.getRosterItemId());
    CompetitionContestantProfileEntity profile = profileMapper.selectForUpdateByRegistrationId(
        principal.getTenantId(), registrationId);
    if (profile == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手档案不存在");
    }
    boolean scopeChanged = !Objects.equals(registration.getOrgId(), req.orgId())
        || !Objects.equals(profile.getEventWorkTypeId(), req.eventWorkTypeId());
    if (scopeChanged) {
      scopeChangeGuard.assertChangeAllowed(principal.getTenantId(), event.getId(), profile);
    }
    if (!Objects.equals(profile.getEventWorkTypeId(), req.eventWorkTypeId())
        && registrationMapper.existsEffectiveByPlayerAndWorkType(
            principal.getTenantId(), event.getId(), registration.getPlayerId(), req.eventWorkTypeId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "该选手已报名当前赛项");
    }
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
    registration.setEventWorkTypeId(req.eventWorkTypeId());
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

  private CompetitionPlayerVO updateMultipleWorkTypes(
      PlatformUserPrincipal principal,
      CompetitionRegistrationEntity primaryRegistration,
      CompetitionEventEntity event,
      CompetitionPlayerUpdateReq req) {
    List<ResolvedWorkType> desired = resolveWorkTypes(
        principal, event, req.workTypes(), req.eventWorkTypeId(), req.level());
    Set<Long> desiredIds = new HashSet<>();
    for (ResolvedWorkType selection : desired) {
      if (!desiredIds.add(selection.eventWorkTypeId())) {
        throw new BusinessException(ErrorCode.CONFLICT, "同一选手不能重复选择同一赛事工种");
      }
    }

    List<CompetitionRegistrationEntity> existing = registrationMapper.selectList(
        new LambdaQueryWrapper<CompetitionRegistrationEntity>()
            .eq(CompetitionRegistrationEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionRegistrationEntity::getEventId, event.getId())
            .eq(CompetitionRegistrationEntity::getPlayerId, primaryRegistration.getPlayerId())
            .eq(CompetitionRegistrationEntity::getDeleted, 0)
            .orderByAsc(CompetitionRegistrationEntity::getId));
    if (existing == null) existing = new ArrayList<>();

    Map<Long, CompetitionRegistrationEntity> registrationByWorkType = new LinkedHashMap<>();
    Map<Long, CompetitionContestantProfileEntity> profileByRegistration = new HashMap<>();
    for (CompetitionRegistrationEntity registration : existing) {
      if (registration == null) continue;
      CompetitionContestantProfileEntity profile = profileMapper.selectForUpdateByRegistrationId(
          principal.getTenantId(), registration.getId());
      profileByRegistration.put(registration.getId(), profile);
      Long workTypeId = effectiveWorkTypeId(registration, profile);
      if (workTypeId != null) registrationByWorkType.put(workTypeId, registration);
    }
    if (!registrationByWorkType.containsValue(primaryRegistration)) {
      Long primaryWorkTypeId = effectiveWorkTypeId(primaryRegistration,
          profileByRegistration.get(primaryRegistration.getId()));
      if (primaryWorkTypeId != null) registrationByWorkType.put(primaryWorkTypeId, primaryRegistration);
    }
    if (registrationByWorkType.isEmpty() && !existing.isEmpty()) {
      registrationByWorkType.put(desired.get(0).eventWorkTypeId(), primaryRegistration);
    }

    String idCardNo = StringUtils.hasText(req.idCardNo())
        ? req.idCardNo() : findPlayerSensitiveValue(
            principal.getTenantId(), primaryRegistration.getPlayerId(), true);
    String mobile = StringUtils.hasText(req.mobile())
        ? req.mobile() : findPlayerSensitiveValue(
            principal.getTenantId(), primaryRegistration.getPlayerId(), false);
    CompetitionRosterItemEntity primaryItem = findRosterItem(
        principal.getTenantId(), primaryRegistration.getRosterItemId());
    String name = StringUtils.hasText(req.name())
        ? req.name().trim() : primaryItem.getName();

    for (CompetitionRegistrationEntity registration : existing) {
      if (registration == null) continue;
      CompetitionContestantProfileEntity profile = profileByRegistration.get(registration.getId());
      Long currentWorkTypeId = effectiveWorkTypeId(registration, profile);
      if (currentWorkTypeId == null && registration.getId().equals(primaryRegistration.getId())
          && desired.size() == 1) {
        currentWorkTypeId = desired.get(0).eventWorkTypeId();
      }
      if (currentWorkTypeId == null || desiredIds.contains(currentWorkTypeId)) continue;
      deleteExistingRegistration(principal, event, registration, profile);
    }

    for (ResolvedWorkType selection : desired) {
      CompetitionRegistrationEntity registration = registrationByWorkType.get(selection.eventWorkTypeId());
      if (registration == null) continue;
      CompetitionContestantProfileEntity profile = profileByRegistration.get(registration.getId());
      if (profile == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "选手档案不存在");
      }
      updateExistingWorkType(principal, registration, profile, req, selection);
    }

    List<ResolvedWorkType> additions = desired.stream()
        .filter(selection -> !registrationByWorkType.containsKey(selection.eventWorkTypeId()))
        .toList();
    if (!additions.isEmpty()) {
      CompetitionRosterImportEntity batch = rosterImportService.precheckSingleMulti(
          principal.getTenantId(), event.getId(), req.orgId(),
          name, idCardNo, mobile,
          additions.stream().map(selection -> new CompetitionRosterImportService.SingleWorkTypeSelection(
              selection.eventWorkTypeId(), selection.workTypeName(), selection.levelId(),
              selection.levelCode(), selection.levelName())).toList());
      if (batch == null || !"pending_confirm".equals(batch.getStatus())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            batch == null ? "选手信息校验失败"
                : validationFailureMessage(principal.getTenantId(), batch.getId(), batch.getSummary()));
      }
      registrationService.confirmAndGenerate(
          principal.getTenantId(), batch.getId(), principal.getUserId());
    }

    playerIdentityService.updateSensitiveData(
        principal.getTenantId(), primaryRegistration.getPlayerId(), idCardNo, mobile);
    CompetitionRegistrationEntity target = registrationMapper.selectOne(
        new LambdaQueryWrapper<CompetitionRegistrationEntity>()
            .eq(CompetitionRegistrationEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionRegistrationEntity::getEventId, event.getId())
            .eq(CompetitionRegistrationEntity::getPlayerId, primaryRegistration.getPlayerId())
            .eq(CompetitionRegistrationEntity::getEventWorkTypeId, desired.get(0).eventWorkTypeId())
            .eq(CompetitionRegistrationEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (target == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "选手参赛资格更新失败");
    }
    return toVO(registrationMapper.selectPlayerByRegistrationId(
        principal.getTenantId(), target.getId()));
  }

  private void updateExistingWorkType(
      PlatformUserPrincipal principal,
      CompetitionRegistrationEntity registration,
      CompetitionContestantProfileEntity profile,
      CompetitionPlayerUpdateReq req,
      ResolvedWorkType selection) {
    boolean scopeChanged = !Objects.equals(registration.getOrgId(), req.orgId())
        || !Objects.equals(effectiveWorkTypeId(registration, profile), selection.eventWorkTypeId())
        || !Objects.equals(profile.getEventWorkTypeLevelId(), selection.levelId());
    if (scopeChanged) {
      scopeChangeGuard.assertChangeAllowed(principal.getTenantId(), registration.getEventId(), profile);
    }
    CompetitionRosterItemEntity item = findRosterItem(
        principal.getTenantId(), registration.getRosterItemId());
    if (StringUtils.hasText(req.name())) item.setName(req.name().trim());
    item.setWorkType(selection.workTypeName());
    item.setLevel(selection.levelName());
    item.setEventWorkTypeId(selection.eventWorkTypeId());
    item.setEventWorkTypeLevelId(selection.levelId());
    item.setOrgId(req.orgId());
    item.setUpdatedBy(principal.getUserId());
    if (rosterItemMapper.updateById(item) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手名单已发生变化，请刷新后重试");
    }
    registration.setOrgId(req.orgId());
    registration.setEventWorkTypeId(selection.eventWorkTypeId());
    registration.setUpdatedBy(principal.getUserId());
    if (registrationMapper.updateById(registration) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手参赛企业已发生变化，请刷新后重试");
    }
    profile.setEventWorkTypeId(selection.eventWorkTypeId());
    profile.setEventWorkTypeLevelId(selection.levelId());
    profile.setWorkTypeMatchStatus("matched");
    profile.setWorkTypeMatchNote("后台选手管理维护");
    profile.setUpdatedBy(principal.getUserId());
    if (profileMapper.updateById(profile) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手赛事工种等级已发生变化，请刷新后重试");
    }
  }

  private void deleteExistingRegistration(
      PlatformUserPrincipal principal,
      CompetitionEventEntity event,
      CompetitionRegistrationEntity registration,
      CompetitionContestantProfileEntity profile) {
    if (profile != null) {
      scopeChangeGuard.assertChangeAllowed(principal.getTenantId(), event.getId(), profile);
      if (profileMapper.deleteById(profile.getId()) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "选手赛事工种删除失败，请刷新后重试");
      }
    }
    if (registrationMapper.deleteById(registration.getId()) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手参赛资格删除失败，请刷新后重试");
    }
    if (registration.getRosterItemId() != null && rosterItemMapper.deleteById(registration.getRosterItemId()) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手名单记录删除失败，请刷新后重试");
    }
  }

  @Transactional
  public void delete(Long registrationId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRegistrationEntity registration = findRegistration(principal.getTenantId(), registrationId);
    CompetitionEventEntity event = assertEventOrgAccess(principal, registration.getEventId(), registration.getOrgId());
    CompetitionContestantProfileEntity profile = profileMapper.selectForUpdateByRegistrationId(
        principal.getTenantId(), registrationId);
    if (profile != null) {
      scopeChangeGuard.assertChangeAllowed(principal.getTenantId(), event.getId(), profile);
      profileMapper.deleteById(profile.getId());
    }
    if (registration.getMemberId() != null) {
      Long remaining = registrationMapper.selectCount(
          new LambdaQueryWrapper<CompetitionRegistrationEntity>()
              .eq(CompetitionRegistrationEntity::getTenantId, principal.getTenantId())
              .eq(CompetitionRegistrationEntity::getMemberId, registration.getMemberId())
              .ne(CompetitionRegistrationEntity::getId, registrationId)
              .eq(CompetitionRegistrationEntity::getDeleted, 0));
      if (remaining == null || remaining == 0) {
        memberMapper.deleteById(registration.getMemberId());
      }
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
    if (workType == null || !Objects.equals(eventId, workType.getEventId())
        || !"active".equals(workType.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种无效或不属于当前赛事");
    }
    return workType.getDisplayName();
  }

  private List<ResolvedWorkType> resolveWorkTypes(
      PlatformUserPrincipal principal,
      CompetitionEventEntity event,
      List<CompetitionPlayerWorkTypeReq> requested,
      Long legacyEventWorkTypeId,
      String legacyLevel) {
    if (requested == null) {
      if (legacyEventWorkTypeId == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一个赛事工种");
      }
      requested = List.of(new CompetitionPlayerWorkTypeReq(
          legacyEventWorkTypeId, null, null, legacyLevel));
    } else if (requested.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一个赛事工种");
    }
    List<ResolvedWorkType> result = new ArrayList<>();
    Set<Long> ids = new HashSet<>();
    for (CompetitionPlayerWorkTypeReq item : requested) {
      if (item == null || item.eventWorkTypeId() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事工种不能为空");
      }
      if (!ids.add(item.eventWorkTypeId())) {
        throw new BusinessException(ErrorCode.CONFLICT, "同一选手不能重复选择同一赛事工种");
      }
      String workTypeName = requireEventWorkType(
          principal.getTenantId(), event.getId(), item.eventWorkTypeId());
      LevelBinding level = resolveLevel(
          principal.getTenantId(), event.getId(), item.eventWorkTypeId(), item);
      result.add(new ResolvedWorkType(
          item.eventWorkTypeId(), workTypeName, level.id(), level.code(), level.name()));
    }
    return result;
  }

  private LevelBinding resolveLevel(
      Long tenantId,
      Long eventId,
      Long eventWorkTypeId,
      CompetitionPlayerWorkTypeReq requested) {
    if (eventWorkTypeLevelService == null) {
      if (!StringUtils.hasText(requested.level())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "每个赛事工种都必须选择等级");
      }
      return new LevelBinding(null, requested.levelCode(), requested.level().trim());
    }
    List<CompetitionEventWorkTypeLevelEntity> levels = eventWorkTypeLevelService.list(
        tenantId, eventId, eventWorkTypeId);
    if (levels == null || levels.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "该赛事工种尚未配置可报名等级");
    }
    CompetitionEventWorkTypeLevelEntity selected = null;
    if (requested.eventWorkTypeLevelId() != null) {
      for (CompetitionEventWorkTypeLevelEntity level : levels) {
        if (level != null && Objects.equals(level.getId(), requested.eventWorkTypeLevelId())) {
          selected = level;
          break;
        }
      }
      if (selected == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种等级无效或不属于当前工种");
      }
    } else if (StringUtils.hasText(requested.levelCode()) || StringUtils.hasText(requested.level())) {
      String code = requested.levelCode();
      String name = requested.level();
      for (CompetitionEventWorkTypeLevelEntity level : levels) {
        if (level == null) continue;
        boolean matches = (StringUtils.hasText(code) && code.trim().equals(level.getLevelCode()))
            || (StringUtils.hasText(name) && (name.trim().equals(level.getLevelName())
                || name.trim().equals(level.getSourceLevelText())));
        if (matches) {
          if (selected != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "赛事工种等级匹配结果不唯一");
          }
          selected = level;
        }
      }
      if (selected == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种等级无效或不属于当前工种");
      }
    } else {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "每个赛事工种都必须选择等级");
    }
    if (StringUtils.hasText(requested.levelCode())
        && !requested.levelCode().trim().equals(selected.getLevelCode())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种等级编码与等级不一致");
    }
    if (StringUtils.hasText(requested.level())
        && !requested.level().trim().equals(selected.getLevelName())
        && !requested.level().trim().equals(selected.getSourceLevelText())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种等级名称与等级不一致");
    }
    return new LevelBinding(selected.getId(), selected.getLevelCode(), selected.getLevelName());
  }

  private Long effectiveWorkTypeId(
      CompetitionRegistrationEntity registration,
      CompetitionContestantProfileEntity profile) {
    if (profile != null && profile.getEventWorkTypeId() != null) {
      return profile.getEventWorkTypeId();
    }
    return registration == null ? null : registration.getEventWorkTypeId();
  }

  private record LevelBinding(Long id, String code, String name) {
  }

  private record ResolvedWorkType(
      Long eventWorkTypeId,
      String workTypeName,
      Long levelId,
      String levelCode,
      String levelName) {
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

  private String validationFailureMessage(Long tenantId, Long batchId, String fallback) {
    if (batchId == null) {
      return StringUtils.hasText(fallback) ? fallback : "选手信息校验失败";
    }
    CompetitionRosterItemEntity item = rosterItemMapper.selectOne(new LambdaQueryWrapper<CompetitionRosterItemEntity>()
        .eq(CompetitionRosterItemEntity::getTenantId, tenantId)
        .eq(CompetitionRosterItemEntity::getBatchId, batchId)
        .eq(CompetitionRosterItemEntity::getDeleted, 0)
        .last("LIMIT 1"));
    return item != null && StringUtils.hasText(item.getErrorMessage())
        ? item.getErrorMessage()
        : (StringUtils.hasText(fallback) ? fallback : "选手信息校验失败");
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
    return toVO(row, false);
  }

  private CompetitionPlayerVO toVO(CompetitionPlayerQueryRow row, boolean includeSensitiveData) {
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
    vo.setEventWorkTypeLevelId(row.getEventWorkTypeLevelId());
    vo.setEventWorkTypeLevelCode(row.getEventWorkTypeLevelCode());
    vo.setEventWorkTypeLevelName(row.getEventWorkTypeLevelName());
    vo.setName(row.getName());
    vo.setIdCardNo(includeSensitiveData ? row.getIdCardNo() : SensitiveMaskUtils.maskIdCard(row.getIdCardNo()));
    vo.setMobile(includeSensitiveData ? row.getMobile() : SensitiveMaskUtils.maskPhone(row.getMobile()));
    vo.setLevel(row.getLevel());
    vo.setStatus(row.getStatus());
    vo.setPlayerStatus(row.getPlayerStatus());
    vo.setCreatedAt(row.getCreatedAt());
    vo.setWorkTypes(loadWorkTypes(row));
    return vo;
  }

  private List<CompetitionPlayerWorkTypeVO> loadWorkTypes(CompetitionPlayerQueryRow row) {
    if (row.getEventId() == null || row.getPlayerId() == null) {
      return row.getEventWorkTypeId() == null ? List.of() : List.of(toWorkTypeVO(row));
    }
    List<CompetitionPlayerQueryRow> rows = registrationMapper.selectPlayerRowsByEventAndPlayer(
        row.getEventId() == null ? null : currentTenantId(), row.getEventId(), row.getPlayerId(), row.getOrgId());
    if (rows == null || rows.isEmpty()) {
      return row.getEventWorkTypeId() == null ? List.of() : List.of(toWorkTypeVO(row));
    }
    Map<Long, CompetitionPlayerWorkTypeVO> result = new LinkedHashMap<>();
    for (CompetitionPlayerQueryRow item : rows) {
      if (item == null || item.getEventWorkTypeId() == null) continue;
      result.putIfAbsent(item.getEventWorkTypeId(), toWorkTypeVO(item));
    }
    return new ArrayList<>(result.values());
  }

  private CompetitionPlayerWorkTypeVO toWorkTypeVO(CompetitionPlayerQueryRow row) {
    CompetitionPlayerWorkTypeVO vo = new CompetitionPlayerWorkTypeVO();
    vo.setRegistrationId(row.getRegistrationId());
    vo.setProfileId(row.getProfileId());
    vo.setEventWorkTypeId(row.getEventWorkTypeId());
    vo.setEventWorkTypeName(row.getEventWorkTypeName() == null ? row.getWorkType() : row.getEventWorkTypeName());
    vo.setEventWorkTypeLevelId(row.getEventWorkTypeLevelId());
    vo.setLevelCode(row.getEventWorkTypeLevelCode());
    vo.setLevelName(row.getEventWorkTypeLevelName() == null ? row.getLevel() : row.getEventWorkTypeLevelName());
    vo.setLevel(row.getLevel());
    vo.setStatus(row.getStatus());
    return vo;
  }

  private Long currentTenantId() {
    return currentUser().getTenantId();
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
