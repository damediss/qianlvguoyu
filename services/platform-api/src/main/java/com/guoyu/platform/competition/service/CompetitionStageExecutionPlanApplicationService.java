package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionStageExecutionPlanCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStagePlanResourceBatchReq;
import com.guoyu.platform.competition.dto.CompetitionStageRosterAdjustmentReq;
import com.guoyu.platform.competition.dto.CompetitionStageRosterGenerateReq;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePlanResourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterSnapshotEntity;
import com.guoyu.platform.competition.vo.CompetitionStageExecutionPlanVO;
import com.guoyu.platform.competition.vo.CompetitionStagePlanPreflightVO;
import com.guoyu.platform.competition.vo.CompetitionStagePlanResourceVO;
import com.guoyu.platform.competition.vo.CompetitionStageRosterItemVO;
import com.guoyu.platform.competition.vo.CompetitionStageRosterSnapshotVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionStageExecutionPlanApplicationService {
  private final CompetitionStageApplicationService stageApplicationService;
  private final CompetitionStageService stageService;
  private final CompetitionStageExecutionPlanService planService;

  public CompetitionStageExecutionPlanApplicationService(
      CompetitionStageApplicationService stageApplicationService,
      CompetitionStageService stageService,
      CompetitionStageExecutionPlanService planService) {
    this.stageApplicationService = stageApplicationService;
    this.stageService = stageService;
    this.planService = planService;
  }

  @Transactional(readOnly = true)
  public List<CompetitionStageExecutionPlanVO> list(Long stageId, Long eventWorkTypeId, Long levelId) {
    CompetitionStageEntity stage = scopedStage(stageId, false);
    return planService.list(currentUser().getTenantId(), stageId, eventWorkTypeId, levelId).stream()
        .map(plan -> toPlanVO(plan, planService.resources(currentUser().getTenantId(), plan.getId())))
        .toList();
  }

  @Transactional
  public CompetitionStageExecutionPlanVO create(Long stageId, CompetitionStageExecutionPlanCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = scopedStage(stageId, true);
    return toPlanVO(planService.create(principal.getTenantId(), principal.getUserId(), stage, req), List.of());
  }

  @Transactional(readOnly = true)
  public CompetitionStageExecutionPlanVO get(Long planId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageExecutionPlanEntity plan = planService.get(principal.getTenantId(), planId);
    scopedStage(plan.getStageId(), false);
    return toPlanVO(plan, planService.resources(principal.getTenantId(), planId));
  }

  @Transactional
  public CompetitionStageExecutionPlanVO replaceResources(Long planId, CompetitionStagePlanResourceBatchReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageExecutionPlanEntity plan = planService.get(principal.getTenantId(), planId);
    CompetitionStageEntity stage = scopedStage(plan.getStageId(), true);
    List<CompetitionStagePlanResourceEntity> resources = planService.replaceResources(
        principal.getTenantId(), principal.getUserId(), stage, plan, req);
    return toPlanVO(plan, resources);
  }

  @Transactional
  public CompetitionStagePlanPreflightVO preflight(Long planId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageExecutionPlanEntity plan = planService.get(principal.getTenantId(), planId);
    CompetitionStageEntity stage = scopedStage(plan.getStageId(), false);
    return planService.preflight(principal.getTenantId(), plan, stage);
  }

  @Transactional
  public CompetitionStageExecutionPlanVO publish(Long planId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageExecutionPlanEntity plan = planService.get(principal.getTenantId(), planId);
    CompetitionStageEntity stage = scopedStage(plan.getStageId(), true);
    return toPlanVO(planService.publish(principal.getTenantId(), principal.getUserId(), plan, stage),
        planService.resources(principal.getTenantId(), planId));
  }

  @Transactional
  public CompetitionStageExecutionPlanVO withdraw(Long planId, String reason) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageExecutionPlanEntity plan = planService.get(principal.getTenantId(), planId);
    scopedStage(plan.getStageId(), true);
    return toPlanVO(planService.withdraw(principal.getTenantId(), principal.getUserId(), plan, reason),
        planService.resources(principal.getTenantId(), planId));
  }

  @Transactional
  public CompetitionStageRosterSnapshotVO generateRoster(Long planId, CompetitionStageRosterGenerateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageExecutionPlanEntity plan = planService.get(principal.getTenantId(), planId);
    scopedStage(plan.getStageId(), true);
    return rosterVO(planService.generateRoster(principal.getTenantId(), principal.getUserId(), plan, req));
  }

  @Transactional
  public CompetitionStageRosterSnapshotVO publishRoster(Long planId, Long snapshotId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageExecutionPlanEntity plan = planService.get(principal.getTenantId(), planId);
    scopedStage(plan.getStageId(), true);
    return rosterVO(planService.publishRoster(principal.getTenantId(), principal.getUserId(), plan, snapshotId));
  }

  @Transactional
  public CompetitionStageRosterItemVO adjustRoster(
      Long planId, Long snapshotId, CompetitionStageRosterAdjustmentReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageExecutionPlanEntity plan = planService.get(principal.getTenantId(), planId);
    scopedStage(plan.getStageId(), true);
    return itemVO(planService.adjustRoster(principal.getTenantId(), principal.getUserId(), plan, snapshotId, req));
  }

  @Transactional(readOnly = true)
  public List<CompetitionStageRosterSnapshotVO> rosterHistory(Long planId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageExecutionPlanEntity plan = planService.get(principal.getTenantId(), planId);
    scopedStage(plan.getStageId(), false);
    return planService.rosterSnapshots(principal.getTenantId(), planId).stream().map(this::rosterVO).toList();
  }

  private CompetitionStageEntity scopedStage(Long stageId, boolean manage) {
    var stageVO = manage ? stageApplicationService.assertCanManage(stageId) : stageApplicationService.get(stageId);
    CompetitionStageEntity stage = new CompetitionStageEntity();
    stage.setId(stageVO.getId());
    stage.setEventId(stageVO.getEventId());
    stage.setStartAt(stageVO.getStartAt());
    stage.setEndAt(stageVO.getEndAt());
    stage.setStatus(stageVO.getStatus());
    return stage;
  }

  private CompetitionStageExecutionPlanVO toPlanVO(
      CompetitionStageExecutionPlanEntity plan, List<CompetitionStagePlanResourceEntity> resources) {
    return new CompetitionStageExecutionPlanVO(
        plan.getId(), plan.getEventId(), plan.getStageId(), plan.getEventWorkTypeId(),
        plan.getEventWorkTypeLevelId(), plan.getPlanVersion(), plan.getStatus(),
        plan.getResourceVersion(), plan.getPreflightStatus(), plan.getScoreRuleId(),
        plan.getPromotionQuota(), plan.getPromotionRuleType(), plan.getPromotionPercentage(),
        plan.getLegacyRule(), plan.getTargetStageId(), plan.getRosterSnapshotId(),
        plan.getPublishedAt(), plan.getWithdrawnAt(), plan.getWithdrawReason(),
        resources == null ? List.of() : resources.stream().map(resource -> new CompetitionStagePlanResourceVO(
            resource.getId(), resource.getPlanId(), resource.getContentType(), resource.getResourceType(),
            resource.getResourceId(), resource.getResourceCode(), resource.getResourceVersion(),
            resource.getStartAt(), resource.getEndAt(), resource.getResourcePayload(), resource.getStatus())).toList());
  }

  private CompetitionStageRosterSnapshotVO rosterVO(CompetitionStageRosterSnapshotEntity snapshot) {
    PlatformUserPrincipal principal = currentUser();
    List<CompetitionStageRosterItemVO> items = planService.rosterItems(principal.getTenantId(), snapshot.getId()).stream()
        .map(this::itemVO).toList();
    return new CompetitionStageRosterSnapshotVO(snapshot.getId(), snapshot.getPlanId(), snapshot.getEventId(),
        snapshot.getStageId(), snapshot.getEventWorkTypeId(), snapshot.getEventWorkTypeLevelId(),
        snapshot.getSnapshotVersion(), snapshot.getSourceType(), snapshot.getSourcePromotionSnapshotId(),
        snapshot.getStatus(), snapshot.getGeneratedAt(),
        snapshot.getPublishedAt(), items);
  }

  private CompetitionStageRosterItemVO itemVO(CompetitionStageRosterItemEntity item) {
    return new CompetitionStageRosterItemVO(item.getId(), item.getSnapshotId(), item.getContestantMemberId(),
        item.getContestantProfileId(), item.getRegistrationId(), item.getInclusionStatus(),
        item.getEligibilityStatus(), item.getReason());
  }

  private PlatformUserPrincipal currentUser() { return SecurityContext.currentUser(); }
}
