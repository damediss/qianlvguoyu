package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionRegistrationAuditRequest;
import com.guoyu.platform.competition.dto.CompetitionRegistrationQueryReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.enums.CompetitionRegistrationAuditAction;
import com.guoyu.platform.competition.enums.CompetitionRegistrationSource;
import com.guoyu.platform.competition.enums.CompetitionRegistrationStatus;
import com.guoyu.platform.competition.enums.CompetitionRosterImportStatus;
import com.guoyu.platform.competition.enums.CompetitionRosterValidationStatus;
import com.guoyu.platform.competition.enums.CompetitionPlayerStatus;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationQueryRow;
import com.guoyu.platform.competition.mapper.CompetitionRosterImportMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.vo.CompetitionRegistrationAuditResultVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionRegistrationService {

  private final CompetitionEventMapper eventMapper;
  private final CompetitionEventOrgMapper eventOrgMapper;
  private final CompetitionRosterImportMapper importMapper;
  private final CompetitionRosterItemMapper itemMapper;
  private final CompetitionRegistrationMapper registrationMapper;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;
  private final CompetitionPlayerIdentityMapper playerMapper;
  private final CompetitionEventMemberService eventMemberService;
  private final CompetitionContestantParticipationDomainService participationService;

  public CompetitionRegistrationService(
      CompetitionEventMapper eventMapper,
      CompetitionEventOrgMapper eventOrgMapper,
      CompetitionRosterImportMapper importMapper,
      CompetitionRosterItemMapper itemMapper,
      CompetitionRegistrationMapper registrationMapper) {
    this(eventMapper, eventOrgMapper, importMapper, itemMapper, registrationMapper,
        null, null, null, null, null);
  }

  public CompetitionRegistrationService(
      CompetitionEventMapper eventMapper,
      CompetitionEventOrgMapper eventOrgMapper,
      CompetitionRosterImportMapper importMapper,
      CompetitionRosterItemMapper itemMapper,
      CompetitionRegistrationMapper registrationMapper,
      OrgMapper orgMapper,
      UserMapper userMapper) {
    this(eventMapper, eventOrgMapper, importMapper, itemMapper, registrationMapper,
        orgMapper, userMapper, null, null, null);
  }

  public CompetitionRegistrationService(
      CompetitionEventMapper eventMapper,
      CompetitionEventOrgMapper eventOrgMapper,
      CompetitionRosterImportMapper importMapper,
      CompetitionRosterItemMapper itemMapper,
      CompetitionRegistrationMapper registrationMapper,
      OrgMapper orgMapper,
      UserMapper userMapper,
      CompetitionPlayerIdentityMapper playerMapper,
      CompetitionEventMemberService eventMemberService) {
    this(eventMapper, eventOrgMapper, importMapper, itemMapper, registrationMapper,
        orgMapper, userMapper, playerMapper, eventMemberService, null);
  }

  @Autowired
  public CompetitionRegistrationService(
      CompetitionEventMapper eventMapper,
      CompetitionEventOrgMapper eventOrgMapper,
      CompetitionRosterImportMapper importMapper,
      CompetitionRosterItemMapper itemMapper,
      CompetitionRegistrationMapper registrationMapper,
      OrgMapper orgMapper,
      UserMapper userMapper,
      CompetitionPlayerIdentityMapper playerMapper,
      CompetitionEventMemberService eventMemberService,
      CompetitionContestantParticipationDomainService participationService) {
    this.eventMapper = eventMapper;
    this.eventOrgMapper = eventOrgMapper;
    this.importMapper = importMapper;
    this.itemMapper = itemMapper;
    this.registrationMapper = registrationMapper;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.playerMapper = playerMapper;
    this.eventMemberService = eventMemberService;
    this.participationService = participationService;
  }

  @Transactional
  public CompetitionRosterImportEntity confirmAndGenerate(
      Long tenantId, Long batchId, Long confirmedBy) {
    CompetitionRosterImportEntity batch = importMapper.selectForUpdate(tenantId, batchId);
    if (batch == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "名单导入批次不存在");
    }
    if (CompetitionRosterImportStatus.CONFIRMED.getCode().equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "名单导入批次已确认，不能重复确认");
    }
    if (!CompetitionRosterImportStatus.PENDING_CONFIRM.getCode().equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前批次状态不允许确认");
    }

    CompetitionEventEntity event = findEvent(tenantId, batch.getEventId());
    Set<Long> validatedOrganizationIds = new HashSet<>();
    Map<Long, CompetitionEventOrgEntity> participationByOrganization = new HashMap<>();
    if (batch.getOrgId() != null) {
      CompetitionEventOrgEntity participation = validateParticipation(
          tenantId, event, batch.getOrgId());
      validatedOrganizationIds.add(batch.getOrgId());
      participationByOrganization.put(batch.getOrgId(), participation);
    }
    List<CompetitionRosterItemEntity> validItems = itemMapper.selectList(
        new LambdaQueryWrapper<CompetitionRosterItemEntity>()
            .eq(CompetitionRosterItemEntity::getTenantId, tenantId)
            .eq(CompetitionRosterItemEntity::getBatchId, batchId)
            .eq(CompetitionRosterItemEntity::getValidationStatus,
                CompetitionRosterValidationStatus.VALID.getCode())
            .eq(CompetitionRosterItemEntity::getDeleted, 0));
    if (validItems == null || validItems.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前批次没有有效名单行，不能确认");
    }

    for (CompetitionRosterItemEntity item : validItems) {
      Long orgId = resolveItemOrganization(batch, item);
      if (orgId == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "名单行未匹配参赛企业");
      }
      if (validatedOrganizationIds.add(orgId)) {
        participationByOrganization.put(orgId, validateParticipation(tenantId, event, orgId));
      }
    }

    validateNoDuplicateRegistrations(tenantId, event, batch, validItems);
    validateQuota(tenantId, event, batch, participationByOrganization, validItems);

    String registrationStatus = event.getRegistrationAuditRequired() != null
        && event.getRegistrationAuditRequired() == 1
        ? CompetitionRegistrationStatus.PENDING_AUDIT.getCode()
        : CompetitionRegistrationStatus.ELIGIBLE_UNACTIVATED.getCode();
    for (CompetitionRosterItemEntity item : validItems) {
      if (item.getPlayerId() == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "名单缺少选手身份，请按最新模板重新导入");
      }
      LocalDateTime joinedAt = LocalDateTime.now();
      CompetitionEventMemberEntity member = requireEventMemberService().createContestantMember(
          tenantId, event.getId(), item.getPlayerId(), confirmedBy, joinedAt);
      CompetitionRegistrationEntity registration = new CompetitionRegistrationEntity();
      registration.setTenantId(tenantId);
      registration.setEventId(event.getId());
      registration.setOrgId(resolveItemOrganization(batch, item));
      registration.setRosterItemId(item.getId());
      registration.setPlayerId(item.getPlayerId());
      registration.setMemberId(member.getId());
      registration.setEventWorkTypeId(resolveItemWorkType(batch, item));
      registration.setSource(CompetitionRegistrationSource.ENTERPRISE_IMPORT.getCode());
      registration.setStatus(resolveRegistrationStatus(registrationStatus, item.getPlayerId()));
      registration.setDeleted(0);
      registrationMapper.insert(registration);
      if (registration.getId() == null) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建参赛资格失败");
      }
      requireEventMemberService().createContestantProfile(
          tenantId, member.getId(), registration.getId(), confirmedBy,
          registration.getEventWorkTypeId(), item.getEventWorkTypeLevelId());
    }

    batch.setStatus(CompetitionRosterImportStatus.CONFIRMED.getCode());
    batch.setConfirmedBy(confirmedBy);
    batch.setConfirmedAt(LocalDateTime.now());
    importMapper.updateById(batch);
    log.info("Competition roster confirmed: tenantId={}, eventId={}, orgId={}, batchId={}, generatedCount={}, status={}",
        tenantId, event.getId(), batch.getOrgId(), batchId, validItems.size(), registrationStatus);
    return batch;
  }

  private CompetitionEventMemberService requireEventMemberService() {
    if (eventMemberService == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "赛事成员服务不可用");
    }
    return eventMemberService;
  }

  private String resolveRegistrationStatus(String defaultStatus, Long playerId) {
    if (!CompetitionRegistrationStatus.ELIGIBLE_UNACTIVATED.getCode().equals(defaultStatus)
        || playerMapper == null) {
      return defaultStatus;
    }
    CompetitionPlayerIdentityEntity player = playerMapper.selectById(playerId);
    if (player == null) {
      log.warn("Competition registration player identity not found, playerId={}", playerId);
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "名单选手身份不存在");
    }
    return CompetitionPlayerStatus.ACTIVE.getCode().equals(player.getStatus())
        ? CompetitionRegistrationStatus.ELIGIBLE_ACTIVATED.getCode()
        : defaultStatus;
  }

  @Transactional(readOnly = true)
  public Page<CompetitionRegistrationQueryRow> page(
      Long tenantId, CompetitionRegistrationQueryReq req) {
    Page<CompetitionRegistrationQueryRow> page = registrationMapper.selectPageWithDetails(
        new Page<>(req.getPage(), req.getPageSize()), tenantId, req.getEventId(), req.getOrgId(),
        req.getStatus(), req.getName());
    if (page == null) {
      log.warn("Competition registration mapper returned null page, tenantId={}", tenantId);
      return new Page<>(req.getPage(), req.getPageSize());
    }
    return page;
  }

  @Transactional
  public CompetitionRegistrationAuditResultVO audit(
      Long tenantId,
      Long registrationId,
      CompetitionRegistrationAuditRequest request,
      PlatformUserPrincipal principal) {
    CompetitionRegistrationAuditAction action = validateRequest(request);
    CompetitionRegistrationEntity registration = registrationMapper.selectForUpdate(
        tenantId, registrationId);
    if (registration == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "参赛资格不存在");
    }

    CompetitionEventEntity event = eventMapper.selectForUpdate(tenantId, registration.getEventId());
    if (event == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }
    CompetitionRosterItemEntity rosterItem = findRosterItem(tenantId, registration.getRosterItemId());
    CompetitionRosterImportEntity batch = findRosterBatch(tenantId, rosterItem.getBatchId());
    validateRosterOwnership(registration, event, batch, rosterItem);
    validateAuditScope(tenantId, registration, event, principal);

    String beforeStatus = registration.getStatus();
    if (!CompetitionRegistrationStatus.PENDING_AUDIT.getCode().equals(beforeStatus)) {
      log.warn("Competition registration audit status conflict, tenantId={}, registrationId={}, status={}",
          tenantId, registrationId, beforeStatus);
      throw new BusinessException(ErrorCode.CONFLICT, "当前参赛资格状态不允许审核");
    }
    validateEventStatus(event);

    LocalDateTime auditedAt = LocalDateTime.now();
    registration.setStatus(action.getTargetStatus().getCode());
    registration.setAuditReason(normalizeReason(request.getReason()));
    registration.setAuditedBy(principal.getUserId());
    registration.setAuditedAt(auditedAt);
    registration.setUpdatedBy(principal.getUserId());
    registration.setUpdatedAt(auditedAt);
    registrationMapper.updateById(registration);

    CompetitionRegistrationAuditResultVO result = new CompetitionRegistrationAuditResultVO();
    result.setRegistrationId(registration.getId());
    result.setEventId(registration.getEventId());
    result.setOrgId(registration.getOrgId());
    result.setRosterItemId(registration.getRosterItemId());
    result.setAction(action.getCode());
    result.setBeforeStatus(beforeStatus);
    result.setStatus(registration.getStatus());
    result.setAuditedBy(registration.getAuditedBy());
    result.setAuditedAt(registration.getAuditedAt());
    return result;
  }

  private CompetitionRegistrationAuditAction validateRequest(
      CompetitionRegistrationAuditRequest request) {
    if (request == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "审核请求不能为空");
    }
    CompetitionRegistrationAuditAction action =
        CompetitionRegistrationAuditAction.fromCode(request.getAction());
    if (action == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "审核动作不合法");
    }
    String reason = request.getReason();
    if (reason != null && reason.trim().length() > 1000) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "审核原因长度不能超过1000个字符");
    }
    boolean reasonRequired = action == CompetitionRegistrationAuditAction.SUPPLEMENT
        || action == CompetitionRegistrationAuditAction.REJECT;
    if (reasonRequired && (reason == null || reason.trim().isEmpty())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "该审核动作必须填写原因");
    }
    if (!reasonRequired && reason != null && reason.trim().isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "审核原因不能只包含空白字符");
    }
    return action;
  }

  private String normalizeReason(String reason) {
    return reason == null ? null : reason.trim();
  }

  private CompetitionRosterItemEntity findRosterItem(Long tenantId, Long rosterItemId) {
    CompetitionRosterItemEntity item = itemMapper.selectOne(new LambdaQueryWrapper<CompetitionRosterItemEntity>()
        .eq(CompetitionRosterItemEntity::getTenantId, tenantId)
        .eq(CompetitionRosterItemEntity::getId, rosterItemId)
        .eq(CompetitionRosterItemEntity::getDeleted, 0));
    if (item == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "关联名单行不存在");
    }
    return item;
  }

  private CompetitionRosterImportEntity findRosterBatch(Long tenantId, Long batchId) {
    CompetitionRosterImportEntity batch = importMapper.selectOne(new LambdaQueryWrapper<CompetitionRosterImportEntity>()
        .eq(CompetitionRosterImportEntity::getTenantId, tenantId)
        .eq(CompetitionRosterImportEntity::getId, batchId)
        .eq(CompetitionRosterImportEntity::getDeleted, 0));
    if (batch == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "关联名单批次不存在");
    }
    return batch;
  }

  private void validateRosterOwnership(
      CompetitionRegistrationEntity registration,
      CompetitionEventEntity event,
      CompetitionRosterImportEntity batch,
      CompetitionRosterItemEntity item) {
    Long itemOrgId = resolveItemOrganization(batch, item);
    Long itemWorkTypeId = resolveItemWorkType(batch, item);
    if (!java.util.Objects.equals(registration.getEventId(), batch.getEventId())
        || !java.util.Objects.equals(registration.getOrgId(), itemOrgId)
        || !java.util.Objects.equals(registration.getEventWorkTypeId(), itemWorkTypeId)
        || !java.util.Objects.equals(registration.getEventId(), event.getId())) {
      log.error("Competition registration association is invalid, registrationId={}, eventId={}, orgId={}, batchId={}",
          registration.getId(), registration.getEventId(), registration.getOrgId(), batch.getId());
      throw new BusinessException(ErrorCode.NOT_FOUND, "参赛资格关联数据不存在");
    }
  }

  private void validateAuditScope(
      Long tenantId,
      CompetitionRegistrationEntity registration,
      CompetitionEventEntity event,
      PlatformUserPrincipal principal) {
    boolean superAdmin = PlatformAdminSupport.isSuperAdmin(principal);
    if (orgMapper == null && !superAdmin) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无赛事数据范围");
    }
    if (orgMapper != null) {
      ensureTenantOrganization(tenantId, registration.getOrgId());
    }
    if (!superAdmin) {
      if (userMapper == null) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无赛事数据范围");
      }
      Long currentOrgId = currentUserOrgId(tenantId, principal);
      if (!java.util.Objects.equals(currentOrgId, registration.getOrgId())) {
        log.warn("User attempted to audit another organization registration, userId={}, tenantId={}, registrationId={}",
            principal.getUserId(), tenantId, registration.getId());
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权审核其他企业的参赛资格");
      }
    }

    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      if (!java.util.Objects.equals(registration.getOrgId(), event.getOwnerOrgId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权审核该赛事的参赛资格");
      }
      return;
    }
    if (!CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事范围不合法");
    }
    CompetitionEventOrgEntity relation = eventOrgMapper.selectForUpdate(
        tenantId, event.getId(), registration.getOrgId());
    if (relation == null
        || !CompetitionEventOrgStatus.ACTIVE.getCode().equals(relation.getStatus())) {
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

  private Long currentUserOrgId(Long tenantId, PlatformUserPrincipal principal) {
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, tenantId)
        .eq(UserEntity::getId, principal.getUserId()));
    if (user == null || user.getOrgId() == null) {
      log.warn("Registration audit user has no organization, userId={}, tenantId={}",
          principal.getUserId(), tenantId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无企业数据权限");
    }
    return user.getOrgId();
  }

  private void validateEventStatus(CompetitionEventEntity event) {
    if (CompetitionEventStatus.CLOSED.getCode().equals(event.getStatus())
        || CompetitionEventStatus.ARCHIVED.getCode().equals(event.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事已关闭或归档，不允许审核");
    }
    if (!CompetitionEventStatus.DRAFT.getCode().equals(event.getStatus())
        && !CompetitionEventStatus.PUBLISHED.getCode().equals(event.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "当前赛事状态不允许审核");
    }
  }

  private CompetitionEventEntity findEvent(Long tenantId, Long eventId) {
    CompetitionEventEntity event = eventMapper.selectForUpdate(tenantId, eventId);
    if (event == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }
    return event;
  }

  private CompetitionEventOrgEntity validateParticipation(
      Long tenantId, CompetitionEventEntity event, Long orgId) {
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      if (!java.util.Objects.equals(orgId, event.getOwnerOrgId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "企业内部赛事仅所属企业可确认名单");
      }
      return null;
    }
    if (!CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事范围不合法");
    }
    CompetitionEventOrgEntity participation = eventOrgMapper.selectForUpdate(
        tenantId, event.getId(), orgId);
    if (participation == null
        || !CompetitionEventOrgStatus.ACTIVE.getCode().equals(participation.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前企业不是有效赛事参与企业");
    }
    if (participation.getImportPermission() == null || participation.getImportPermission() != 1) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前企业未获得名单导入权限");
    }
    return participation;
  }

  private void validateNoDuplicateRegistrations(
      Long tenantId,
      CompetitionEventEntity event,
      CompetitionRosterImportEntity batch,
      List<CompetitionRosterItemEntity> validItems) {
    Set<String> itemKeys = new HashSet<>();
    Set<String> playerWorkTypeKeys = new HashSet<>();
    for (CompetitionRosterItemEntity item : validItems) {
      CompetitionRegistrationEntity existing = registrationMapper.selectOne(
          new LambdaQueryWrapper<CompetitionRegistrationEntity>()
              .eq(CompetitionRegistrationEntity::getTenantId, tenantId)
              .eq(CompetitionRegistrationEntity::getRosterItemId, item.getId())
              .eq(CompetitionRegistrationEntity::getDeleted, 0));
      if (existing != null) {
        throw new BusinessException(ErrorCode.CONFLICT, "rosterItem 已存在参赛资格");
      }
      Long orgId = resolveItemOrganization(batch, item);
      Long eventWorkTypeId = resolveItemWorkType(batch, item);
      if (item.getPlayerId() == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "名单缺少选手身份，请按最新模板重新导入");
      }
      String itemKey = orgId + "::" + item.getPlayerId() + "::"
          + (eventWorkTypeId == null ? "legacy" : eventWorkTypeId);
      if (!itemKeys.add(itemKey)) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "同一企业的同一选手在当前批次中重复报名同一赛事工种");
      }
      if (!playerWorkTypeKeys.add(item.getPlayerId() + "::"
          + (eventWorkTypeId == null ? "legacy" : eventWorkTypeId))) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "该选手在当前批次中重复报名同一赛事工种");
      }
      if (participationService != null) {
        participationService.assertCanEnroll(
            tenantId, event, item.getPlayerId(), eventWorkTypeId);
      } else if (registrationMapper.existsEffectiveByPlayerAndWorkType(
          tenantId, event.getId(), item.getPlayerId(), eventWorkTypeId)) {
        throw new BusinessException(ErrorCode.CONFLICT, "该选手已报名当前赛项");
      }
    }
  }

  private void validateQuota(
      Long tenantId,
      CompetitionEventEntity event,
      CompetitionRosterImportEntity batch,
      Map<Long, CompetitionEventOrgEntity> participationByOrganization,
      List<CompetitionRosterItemEntity> validItems) {
    if (participationByOrganization == null || participationByOrganization.isEmpty()) {
      return;
    }
    for (Map.Entry<Long, CompetitionEventOrgEntity> entry : participationByOrganization.entrySet()) {
      CompetitionEventOrgEntity participation = entry.getValue();
      if (participation == null || participation.getQuota() == null) {
        continue;
      }
      Long orgId = entry.getKey();
      long existingCount = registrationMapper.countDistinctEffectivePlayersByEventOrg(
          tenantId, event.getId(), orgId);
      Set<Long> newPlayerIds = new HashSet<>();
      for (CompetitionRosterItemEntity item : validItems) {
        if (!Objects.equals(orgId, resolveItemOrganization(batch, item)) || item.getPlayerId() == null) {
          continue;
        }
        if (registrationMapper.existsEffectiveByPlayer(
            tenantId, event.getId(), orgId, item.getPlayerId())) {
          continue;
        }
        newPlayerIds.add(item.getPlayerId());
      }
      if (existingCount + newPlayerIds.size() > participation.getQuota()) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "赛事参与企业名额不足，当前名额=" + participation.getQuota());
      }
    }
  }

  private Long resolveItemOrganization(
      CompetitionRosterImportEntity batch, CompetitionRosterItemEntity item) {
    return item != null && item.getOrgId() != null ? item.getOrgId() : batch.getOrgId();
  }

  private Long resolveItemWorkType(
      CompetitionRosterImportEntity batch, CompetitionRosterItemEntity item) {
    return item != null && item.getEventWorkTypeId() != null
        ? item.getEventWorkTypeId() : batch.getEventWorkTypeId();
  }
}
