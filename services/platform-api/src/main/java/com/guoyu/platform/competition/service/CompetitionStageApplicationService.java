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
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.enums.CompetitionStagePlanStatus;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
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
  private final UserMapper userMapper;

  public CompetitionStageApplicationService(
      CompetitionEventService eventService,
      CompetitionStageService stageService,
      CompetitionEventOrgMapper eventOrgMapper,
      UserMapper userMapper) {
    this(eventService, stageService, eventOrgMapper, null, userMapper);
  }

  @Autowired
  public CompetitionStageApplicationService(
      CompetitionEventService eventService,
      CompetitionStageService stageService,
      CompetitionEventOrgMapper eventOrgMapper,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      UserMapper userMapper) {
    this.eventService = eventService;
    this.stageService = stageService;
    this.eventOrgMapper = eventOrgMapper;
    this.eventWorkTypeMapper = eventWorkTypeMapper;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionStageVO> page(Long eventId, CompetitionStageQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, false);
    var page = stageService.page(principal.getTenantId(), event.getId(), req);
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
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, true);
    requireDraftEvent(event);
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
    if (CompetitionStagePlanStatus.CONFIRMED.getCode().equals(event.getStagePlanStatus())
        || CompetitionStagePlanStatus.CONFIRMED.getCode().equals(workType.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前工种赛段编排已确认，不能继续修改");
    }
    List<CompetitionStageEntity> saved = stageService.replaceDraft(
        principal.getTenantId(), event.getId(), principal.getUserId(), req);
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
    requireDraftEvent(event);
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
    requireStagePlanDraft(event);
    return CompetitionStageConverter.toVO(stageService.update(principal.getTenantId(), stageId, req));
  }

  @Transactional
  public void delete(Long stageId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    CompetitionEventEntity event = accessibleEvent(principal, stage.getEventId(), true);
    requireStagePlanDraft(event);
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

  private void requireDraftEvent(CompetitionEventEntity event) {
    if (!CompetitionEventStatus.DRAFT.getCode().equals(event.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有草稿状态赛事允许修改赛段");
    }
  }

  private void requireStagePlanDraft(CompetitionEventEntity event) {
    requireDraftEvent(event);
    if (CompetitionStagePlanStatus.CONFIRMED.getCode().equals(event.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛事的赛段编排已确认，不能继续修改");
    }
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
