package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionEventConverter;
import com.guoyu.platform.competition.converter.CompetitionStageConverter;
import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageDraftSaveReq;
import com.guoyu.platform.competition.dto.CompetitionStageQueryReq;
import com.guoyu.platform.competition.dto.CompetitionStageUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionStagePlanStatus;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.vo.CompetitionEventVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Slf4j
public class CompetitionStageApplicationService {

  private final CompetitionEventService eventService;
  private final CompetitionStageService stageService;
  private final CompetitionEventOrgMapper eventOrgMapper;
  private final CompetitionEventWorkTypeMapper eventWorkTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper eventWorkTypeLevelMapper;
  private final UserMapper userMapper;

  public CompetitionStageApplicationService(
      CompetitionEventService eventService,
      CompetitionStageService stageService,
      CompetitionEventOrgMapper eventOrgMapper,
      UserMapper userMapper) {
    this(eventService, stageService, eventOrgMapper, null, null, userMapper);
  }

  public CompetitionStageApplicationService(
      CompetitionEventService eventService,
      CompetitionStageService stageService,
      CompetitionEventOrgMapper eventOrgMapper,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      UserMapper userMapper) {
    this(eventService, stageService, eventOrgMapper, eventWorkTypeMapper, null, userMapper);
  }

  @Autowired
  public CompetitionStageApplicationService(
      CompetitionEventService eventService,
      CompetitionStageService stageService,
      CompetitionEventOrgMapper eventOrgMapper,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      CompetitionEventWorkTypeLevelMapper eventWorkTypeLevelMapper,
      UserMapper userMapper) {
    this.eventService = eventService;
    this.stageService = stageService;
    this.eventOrgMapper = eventOrgMapper;
    this.eventWorkTypeMapper = eventWorkTypeMapper;
    this.eventWorkTypeLevelMapper = eventWorkTypeLevelMapper;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionStageVO> page(Long eventId, CompetitionStageQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, false);
    var page = req.getEventWorkTypeId() == null
        ? stageService.page(principal.getTenantId(), event.getId(), req)
        : stageService.page(
            principal.getTenantId(), event.getId(), req.getEventWorkTypeId(),
            req.getEventWorkTypeLevelCode(), req);
    List<CompetitionStageVO> items = new ArrayList<>();
    if (page.getRecords() != null) {
      page.getRecords().forEach(stage -> items.add(CompetitionStageConverter.toVO(stage)));
    }
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public CompetitionStageVO get(Long stageId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    accessibleEvent(principal, stage.getEventId(), false);
    return CompetitionStageConverter.toVO(stage);
  }

  @Transactional(readOnly = true)
  public CompetitionStageVO assertCanManage(Long stageId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    accessibleEvent(principal, stage.getEventId(), true);
    return CompetitionStageConverter.toVO(stage);
  }

  /**
   * Checks write access to the stage arrangement itself. Downstream lifecycle and execution-plan
   * operations intentionally continue to use {@link #assertCanManage(Long)} after arrangement
   * confirmation, so the arrangement-specific lock must be explicit at this boundary.
   */
  @Transactional
  public CompetitionStageVO assertCanManageStagePlan(Long stageId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    CompetitionEventEntity event = accessibleEvent(principal, stage.getEventId(), true);
    CompetitionEventEntity locked = eventService.lockForUpdate(principal.getTenantId(), stage.getEventId());
    if (locked != null) {
      event = locked;
    }
    requireStagePlanDraft(event);
    assertStagePlanScopeDraft(principal, stage);
    return CompetitionStageConverter.toVO(stage);
  }

  @Transactional
  public CompetitionStageVO create(Long eventId, CompetitionStageCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, true);
    requireStagePlanDraft(event);
    return CompetitionStageConverter.toVO(stageService.create(principal.getTenantId(), event.getId(), req));
  }

  @Transactional
  public List<CompetitionStageVO> saveDraft(Long eventId, CompetitionStageDraftSaveReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, true);
    requireStagePlanDraft(event);
    List<CompetitionStageEntity> saved = stageService.replaceDraft(
        principal.getTenantId(), event.getId(), principal.getUserId(), req);
    List<CompetitionStageVO> result = new ArrayList<>();
    if (saved != null) {
      saved.forEach(stage -> result.add(CompetitionStageConverter.toVO(stage)));
    }
    return result;
  }

  @Transactional
  public List<CompetitionStageVO> saveDraftForWorkType(
      Long eventId, Long eventWorkTypeId, CompetitionStageDraftSaveReq req) {
    return saveDraftForWorkType(eventId, eventWorkTypeId, null, req);
  }

  @Transactional
  public List<CompetitionStageVO> saveDraftForWorkType(
      Long eventId,
      Long eventWorkTypeId,
      String eventWorkTypeLevelCode,
      CompetitionStageDraftSaveReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, true);
    requireStagePlanDraft(event);
    CompetitionEventEntity locked = eventService.lockForUpdate(principal.getTenantId(), eventId);
    if (locked != null) {
      event = locked;
      requireStagePlanDraft(event);
    }
    if (eventWorkTypeMapper == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "工种赛段作用域未启用");
    }
    CompetitionEventWorkTypeEntity workType = eventWorkTypeMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionEventWorkTypeEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeEntity::getId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
    if (workType == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    }
    if (eventWorkTypeLevelCode == null || eventWorkTypeLevelCode.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "工种赛段编排必须指定工种等级");
    }
    String normalizedLevelCode = normalizeLevelCode(eventWorkTypeLevelCode);
    assertWorkTypeStagePlanDraft(event, workType, principal.getTenantId(), eventId,
        eventWorkTypeId, normalizedLevelCode);
    List<CompetitionStageEntity> saved = stageService.replaceDraftForScope(
        principal.getTenantId(), event.getId(), principal.getUserId(), eventWorkTypeId,
        normalizedLevelCode, req);
    List<CompetitionStageVO> result = new ArrayList<>();
    if (saved != null) {
      saved.forEach(stage -> result.add(CompetitionStageConverter.toVO(stage)));
    }
    return result;
  }

  @Transactional
  public CompetitionEventVO confirmStagePlan(Long eventId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, true);
    com.guoyu.platform.competition.domain.CompetitionEventArrangementGuard.requireLive(
        event, "已关闭或已归档赛事不能确认赛段编排");
    if (CompetitionStagePlanStatus.CONFIRMED.getCode().equals(event.getStagePlanStatus())) {
      return CompetitionEventConverter.toVO(event);
    }
    List<CompetitionStageEntity> stages = stageService.list(principal.getTenantId(), event.getId());
    if (stages == null || stages.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先保存至少一个赛段后再确认编排");
    }
    return CompetitionEventConverter.toVO(
        eventService.confirmStagePlan(principal.getTenantId(), principal.getUserId(), eventId));
  }

  @Transactional
  public CompetitionStageVO update(Long stageId, CompetitionStageUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    CompetitionEventEntity event = accessibleEvent(principal, stage.getEventId(), true);
    CompetitionEventEntity locked = eventService.lockForUpdate(principal.getTenantId(), stage.getEventId());
    if (locked != null) {
      event = locked;
    }
    requireStagePlanDraft(event);
    assertStagePlanScopeDraft(principal, stage);
    return CompetitionStageConverter.toVO(stageService.update(principal.getTenantId(), stageId, req));
  }

  @Transactional
  public void delete(Long stageId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    CompetitionEventEntity event = accessibleEvent(principal, stage.getEventId(), true);
    CompetitionEventEntity locked = eventService.lockForUpdate(principal.getTenantId(), stage.getEventId());
    if (locked != null) {
      event = locked;
    }
    requireStagePlanDraft(event);
    assertStagePlanScopeDraft(principal, stage);
    stageService.delete(principal.getTenantId(), stageId);
  }

  @Transactional
  public CompetitionStageVO beginConfiguration(Long stageId, String reason) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    accessibleEvent(principal, stage.getEventId(), true);
    return CompetitionStageConverter.toVO(
        stageService.beginConfiguration(principal.getTenantId(), principal.getUserId(), stageId, reason));
  }

  @Transactional
  public CompetitionStageVO lockReady(Long stageId) {
    return action(stageId, "lockReady");
  }

  @Transactional
  public CompetitionStageVO startStage(Long stageId) {
    return action(stageId, "startStage");
  }

  @Transactional
  public CompetitionStageVO closeStage(Long stageId) {
    return action(stageId, "closeStage");
  }

  @Transactional
  public CompetitionStageVO markSettled(Long stageId) {
    return action(stageId, "markSettled");
  }

  @Transactional
  public CompetitionStageVO publishResult(Long stageId) {
    return action(stageId, "publishResult");
  }

  private CompetitionStageVO action(Long stageId, String action) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    accessibleEvent(principal, stage.getEventId(), true);
    CompetitionStageEntity result = switch (action) {
      case "lockReady" -> stageService.lockReady(principal.getTenantId(), principal.getUserId(), stageId);
      case "startStage" -> stageService.startStage(principal.getTenantId(), principal.getUserId(), stageId);
      case "closeStage" -> stageService.closeStage(principal.getTenantId(), principal.getUserId(), stageId);
      case "markSettled" -> stageService.markSettled(principal.getTenantId(), principal.getUserId(), stageId);
      case "publishResult" -> stageService.publishResult(principal.getTenantId(), principal.getUserId(), stageId);
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "未知赛段领域动作");
    };
    return CompetitionStageConverter.toVO(result);
  }

  private CompetitionEventEntity accessibleEvent(
      PlatformUserPrincipal principal,
      Long eventId,
      boolean manage) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (PlatformAdminSupport.isSuperAdmin(principal)) {
      return event;
    }
    Long orgId = currentUserOrgId(principal);
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      if (orgId.equals(event.getOwnerOrgId())) {
        return event;
      }
      log.warn("User attempted to access stages outside organization scope, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
    }
    if (manage && CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())) {
      log.warn(
          "Enterprise participant attempted to manage platform event stages, "
              + "eventId={}, userId={}, orgId={}",
          eventId, principal.getUserId(), orgId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "平台赛事赛段仅平台管理员可管理");
    }
    CompetitionEventOrgEntity relation = eventOrgMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventOrgEntity>()
            .eq(CompetitionEventOrgEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionEventOrgEntity::getEventId, eventId)
            .eq(CompetitionEventOrgEntity::getOrgId, orgId)
            .eq(CompetitionEventOrgEntity::getStatus, CompetitionEventOrgStatus.ACTIVE.getCode())
            .eq(CompetitionEventOrgEntity::getDeleted, 0));
    if (relation != null) {
      return event;
    }
    log.warn("User attempted to access stages without active event organization, eventId={}, orgId={}, userId={}, tenantId={}",
        eventId, orgId, principal.getUserId(), principal.getTenantId());
    throw new BusinessException(ErrorCode.FORBIDDEN, "当前企业不是有效赛事参与企业");
  }

  private void requireStagePlanDraft(CompetitionEventEntity event) {
    com.guoyu.platform.competition.domain.CompetitionEventArrangementGuard.requireOpen(
        event, "已关闭或已归档赛事不允许修改赛段");
  }

  private void assertWorkTypeStagePlanDraft(
      CompetitionEventEntity event,
      CompetitionEventWorkTypeEntity workType,
      Long tenantId,
      Long eventId,
      Long eventWorkTypeId,
      String levelCode) {
    if (CompetitionStagePlanStatus.CONFIRMED.getCode().equals(event.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛事的赛段编排已确认，不能继续修改");
    }
    if (levelCode == null
        && CompetitionStagePlanStatus.CONFIRMED.getCode().equals(workType.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前工种赛段编排已确认，不能继续修改");
    }
    if (levelCode != null && eventWorkTypeLevelMapper != null) {
      CompetitionEventWorkTypeLevelEntity level = findLevel(
          tenantId, eventId, eventWorkTypeId, levelCode);
      if (level != null
          && CompetitionStagePlanStatus.CONFIRMED.getCode().equals(level.getStagePlanStatus())) {
        throw new BusinessException(ErrorCode.CONFLICT, "当前工种当前等级赛段编排已确认，不能继续修改");
      }
    }
  }

  private void assertStagePlanScopeDraft(
      PlatformUserPrincipal principal, CompetitionStageEntity stage) {
    if (stage.getEventWorkTypeId() == null) {
      return;
    }
    if (eventWorkTypeMapper == null) {
      return;
    }
    CompetitionEventWorkTypeEntity workType = eventWorkTypeMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionEventWorkTypeEntity::getEventId, stage.getEventId())
            .eq(CompetitionEventWorkTypeEntity::getId, stage.getEventWorkTypeId())
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
    if (workType == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    }
    String levelCode = normalizeLevelCode(stage.getEventWorkTypeLevelCode());
    if (levelCode == null) {
      if (CompetitionStagePlanStatus.CONFIRMED.getCode().equals(workType.getStagePlanStatus())) {
        throw new BusinessException(ErrorCode.CONFLICT, "当前工种赛段编排已确认，不能继续修改");
      }
      return;
    }
    if (eventWorkTypeLevelMapper == null) {
      return;
    }
    CompetitionEventWorkTypeLevelEntity level = findLevel(
        principal.getTenantId(), stage.getEventId(), stage.getEventWorkTypeId(), levelCode);
    if (level != null
        && CompetitionStagePlanStatus.CONFIRMED.getCode().equals(level.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前工种当前等级赛段编排已确认，不能继续修改");
    }
  }

  private CompetitionEventWorkTypeLevelEntity findLevel(
      Long tenantId, Long eventId, Long eventWorkTypeId, String levelCode) {
    return eventWorkTypeLevelMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeLevelEntity::getLevelCode, levelCode)
            .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
  }

  private String normalizeLevelCode(String levelCode) {
    if (levelCode == null || levelCode.isBlank()) {
      return null;
    }
    String normalized = levelCode.trim();
    if (!java.util.Set.of("1", "2", "3", "4", "5").contains(normalized)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事工种等级编码必须是1至5");
    }
    return normalized;
  }

  private Long currentUserOrgId(PlatformUserPrincipal principal) {
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId()));
    if (user == null || user.getOrgId() == null) {
      log.warn("Stage access has no user organization, userId={}, tenantId={}",
          principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无企业数据权限");
    }
    return user.getOrgId();
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition stage request has no authenticated platform user");
      throw ex;
    }
  }
}
