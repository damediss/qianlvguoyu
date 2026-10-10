package com.guoyu.platform.competition.service;

import com.guoyu.platform.competition.dto.CompetitionStageSettlementCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageSettlementPublishReq;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionStageSettlementEntity;
import com.guoyu.platform.competition.vo.CompetitionStageSettlementItemVO;
import com.guoyu.platform.competition.vo.CompetitionStageSettlementPreflightVO;
import com.guoyu.platform.competition.vo.CompetitionStageSettlementVO;
import com.guoyu.platform.competition.entity.CompetitionStageReferenceSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionStageFinalPodiumEntity;
import com.guoyu.platform.competition.dto.CompetitionStageFinalPodiumReq;
import com.guoyu.platform.competition.vo.CompetitionStageReferenceItemVO;
import com.guoyu.platform.competition.vo.CompetitionStageReferenceSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionStageFinalPodiumVO;
import com.guoyu.platform.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionStageSettlementApplicationService {
  private final CompetitionStageSettlementService settlementService;
  private final CompetitionStageExecutionPlanService planService;
  private final CompetitionStageApplicationService stageService;

  public CompetitionStageSettlementApplicationService(CompetitionStageSettlementService settlementService,
      CompetitionStageExecutionPlanService planService, CompetitionStageApplicationService stageService) {
    this.settlementService = settlementService; this.planService = planService; this.stageService = stageService;
  }

  @Transactional(readOnly = true)
  public CompetitionStageSettlementPreflightVO preflight(Long planId, Long scoreRuleId, java.util.List<Long> absentIds) {
    CompetitionStageExecutionPlanEntity plan = planService.get(tenant(), planId); stageService.get(plan.getStageId());
    return settlementService.preflight(tenant(), plan, scoreRuleId, absentIds);
  }

  @Transactional
  public CompetitionStageSettlementVO create(Long pathPlanId, CompetitionStageSettlementCreateReq req) {
    CompetitionStageExecutionPlanEntity plan = planService.get(tenant(), pathPlanId); stageService.assertCanManage(plan.getStageId());
    return vo(settlementService.create(tenant(), userId(), pathPlanId, req));
  }

  @Transactional(readOnly = true)
  public CompetitionStageSettlementVO get(Long settlementId) {
    CompetitionStageSettlementEntity settlement = settlementService.get(tenant(), settlementId); stageService.get(settlement.getStageId()); return vo(settlement);
  }

  @Transactional(readOnly = true)
  public java.util.List<CompetitionStageSettlementVO> list(Long planId) {
    CompetitionStageExecutionPlanEntity plan = planService.get(tenant(), planId);
    stageService.get(plan.getStageId());
    return settlementService.list(tenant(), planId).stream().map(this::vo).toList();
  }

  @Transactional
  public CompetitionStageSettlementVO publish(Long settlementId, CompetitionStageSettlementPublishReq req) {
    CompetitionStageSettlementEntity settlement = settlementService.get(tenant(), settlementId); stageService.assertCanManage(settlement.getStageId());
    return vo(settlementService.publish(tenant(), userId(), settlementId));
  }

  @Transactional
  public CompetitionStageReferenceSnapshotVO createReference(Long settlementId) {
    CompetitionStageSettlementEntity settlement = settlementService.get(tenant(), settlementId);
    stageService.assertCanManage(settlement.getStageId());
    return referenceVO(settlementService.createReference(tenant(), userId(), settlementId));
  }

  @Transactional(readOnly = true)
  public CompetitionStageReferenceSnapshotVO getReference(Long settlementId) {
    CompetitionStageSettlementEntity settlement = settlementService.get(tenant(), settlementId);
    stageService.get(settlement.getStageId());
    CompetitionStageReferenceSnapshotEntity reference = settlementService.reference(tenant(), settlementId);
    if (reference == null) return null;
    return referenceVO(reference);
  }

  @Transactional
  public CompetitionStageReferenceSnapshotVO confirmReference(Long settlementId) {
    CompetitionStageSettlementEntity settlement = settlementService.get(tenant(), settlementId);
    stageService.assertCanManage(settlement.getStageId());
    return referenceVO(settlementService.confirmReference(tenant(), userId(), settlementId));
  }

  @Transactional
  public CompetitionStageFinalPodiumVO saveFinalPodium(Long settlementId, CompetitionStageFinalPodiumReq req) {
    CompetitionStageSettlementEntity settlement = settlementService.get(tenant(), settlementId);
    stageService.assertCanManage(settlement.getStageId());
    return podiumVO(settlementService.saveFinalPodium(tenant(), userId(), settlementId, req));
  }

  @Transactional(readOnly = true)
  public CompetitionStageFinalPodiumVO getFinalPodium(Long settlementId) {
    CompetitionStageSettlementEntity settlement = settlementService.get(tenant(), settlementId);
    stageService.get(settlement.getStageId());
    CompetitionStageFinalPodiumEntity row = settlementService.finalPodium(tenant(), settlementId);
    return row == null ? null : podiumVO(row);
  }

  @Transactional
  public CompetitionStageFinalPodiumVO confirmFinalPodium(Long settlementId) {
    CompetitionStageSettlementEntity settlement = settlementService.get(tenant(), settlementId);
    stageService.assertCanManage(settlement.getStageId());
    return podiumVO(settlementService.confirmFinalPodium(tenant(), userId(), settlementId));
  }

  private CompetitionStageSettlementVO vo(CompetitionStageSettlementEntity s) {
    var promotion = settlementService.promotion(tenant(), s.getPromotionSnapshotId());
    return new CompetitionStageSettlementVO(s.getId(), s.getPlanId(), s.getEventId(), s.getStageId(), s.getEventWorkTypeId(), s.getEventWorkTypeLevelId(), s.getScoreRuleId(), s.getSettlementVersion(), s.getStatus(), s.getReadinessStatus(), s.getRankingSnapshotId(), s.getPromotionSnapshotId(), s.getPublishedAt(), promotion == null ? null : promotion.getPromotionRuleType(), promotion == null ? null : promotion.getPromotionPercentage(), promotion == null ? null : promotion.getCalculatedTargetCount(), promotion == null ? null : promotion.getReferenceStatus(), promotion == null ? null : promotion.getPromotionStatus(), promotion == null ? null : promotion.getFinalPodiumStatus(), settlementService.items(tenant(), s.getId()).stream().map(i -> new CompetitionStageSettlementItemVO(i.getId(), i.getContestantMemberId(), i.getTotalScore(), i.getContestantStatus(), i.getDisplayRank(), i.getTiedFlag(), i.getPromotionStatus(), i.getReason())).toList());
  }
  private CompetitionStageReferenceSnapshotVO referenceVO(CompetitionStageReferenceSnapshotEntity row) {
    return new CompetitionStageReferenceSnapshotVO(row.getId(), row.getSettlementId(), row.getPlanId(), row.getEventId(), row.getStageId(), row.getTargetStageId(), row.getEventWorkTypeId(), row.getEventWorkTypeLevelId(), row.getPromotionRuleType(), row.getPromotionQuota(), row.getPromotionPercentage(), row.getBaseCount(), row.getCalculatedTargetCount(), row.getStatus(), row.getConfirmedBy(), row.getConfirmedAt(), settlementService.referenceItems(tenant(), row.getId()).stream().map(i -> new CompetitionStageReferenceItemVO(i.getId(), i.getContestantMemberId(), i.getSourceRank(), i.getSelectionStatus(), i.getReason())).toList());
  }
  private CompetitionStageFinalPodiumVO podiumVO(CompetitionStageFinalPodiumEntity row) {
    return new CompetitionStageFinalPodiumVO(row.getId(), row.getSettlementId(), row.getPlanId(), row.getEventId(), row.getStageId(), row.getFirstContestantMemberId(), row.getSecondContestantMemberId(), row.getThirdContestantMemberId(), row.getSpecialExplanation(), row.getStatus(), row.getConfirmedBy(), row.getConfirmedAt(), row.getPublishedBy(), row.getPublishedAt());
  }
  private Long tenant() { return SecurityContext.currentUser().getTenantId(); }
  private Long userId() { return SecurityContext.currentUser().getUserId(); }
}
