package com.guoyu.platform.competition.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionStageSettlementEntity;
import com.guoyu.platform.competition.entity.CompetitionStageSettlementItemEntity;
import com.guoyu.platform.competition.enums.CompetitionStageStatus;
import com.guoyu.platform.competition.mapper.CompetitionStageExecutionPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageSettlementItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageSettlementMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Shared server-side guard for the stage execution boundary.
 *
 * <p>Every interactive score write locks the stage row before checking its state. The close action
 * uses the same row lock, so a score write and stage close have a deterministic database order.</p>
 */
@Component
public class CompetitionStageLifecycleGuard {

  private final CompetitionStageMapper stageMapper;
  private final CompetitionStageExecutionPlanMapper planMapper;
  private final CompetitionStageRosterSnapshotMapper rosterSnapshotMapper;
  private final CompetitionStageRosterItemMapper rosterItemMapper;
  private final CompetitionStageSettlementMapper settlementMapper;
  private final CompetitionStageSettlementItemMapper settlementItemMapper;

  public CompetitionStageLifecycleGuard(
      CompetitionStageMapper stageMapper,
      CompetitionStageExecutionPlanMapper planMapper,
      CompetitionStageRosterSnapshotMapper rosterSnapshotMapper,
      CompetitionStageRosterItemMapper rosterItemMapper,
      CompetitionStageSettlementMapper settlementMapper,
      CompetitionStageSettlementItemMapper settlementItemMapper) {
    this.stageMapper = stageMapper;
    this.planMapper = planMapper;
    this.rosterSnapshotMapper = rosterSnapshotMapper;
    this.rosterItemMapper = rosterItemMapper;
    this.settlementMapper = settlementMapper;
    this.settlementItemMapper = settlementItemMapper;
  }

  public CompetitionStageEntity lockStage(Long tenantId, Long stageId) {
    CompetitionStageEntity stage = stageMapper.selectOne(new LambdaQueryWrapper<CompetitionStageEntity>()
        .eq(CompetitionStageEntity::getTenantId, tenantId)
        .eq(CompetitionStageEntity::getId, stageId)
        .eq(CompetitionStageEntity::getDeleted, 0)
        .last("FOR UPDATE"));
    if (stage == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛段不存在");
    }
    return stage;
  }

  public void requireStageWritable(CompetitionStageEntity stage) {
    CompetitionStageStatus status = CompetitionStageStatus.fromCode(stage == null ? null : stage.getStatus());
    if (status == null || !CompetitionStageStatus.writableStatuses().contains(status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛段状态不允许修改配置");
    }
  }

  public void requireReadyToLock(Long tenantId, CompetitionStageEntity stage) {
    if (stage == null || !CompetitionStageStatus.CONFIGURING.getCode().equals(stage.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有配置中的赛段可以锁定就绪");
    }
    requireWindow(stage);
    List<CompetitionStageExecutionPlanEntity> plans = publishedPlans(tenantId, stage);
    if (plans.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "执行方案尚未发布");
    }
    for (CompetitionStageExecutionPlanEntity plan : plans) {
      if (!"passed".equals(plan.getPreflightStatus())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "执行方案预检尚未通过");
      }
      requirePublishedRoster(tenantId, plan);
    }
  }

  public void requireStartable(Long tenantId, CompetitionStageEntity stage) {
    if (stage == null || !CompetitionStageStatus.READY_LOCKED.getCode().equals(stage.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛段必须先锁定为就绪状态");
    }
    LocalDateTime now = LocalDateTime.now();
    if (stage.getStartAt() == null || now.isBefore(stage.getStartAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "尚未到达赛段允许开始时间");
    }
    if (stage.getEndAt() != null && !now.isBefore(stage.getEndAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛段已超过结束时间，不能开始");
    }
    List<CompetitionStageExecutionPlanEntity> plans = publishedPlans(tenantId, stage);
    if (plans.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "没有有效的已发布执行方案");
    }
    for (CompetitionStageExecutionPlanEntity plan : plans) {
      requirePublishedRoster(tenantId, plan);
    }
  }

  public void requireCloseable(CompetitionStageEntity stage) {
    if (stage == null || !CompetitionStageStatus.IN_PROGRESS.getCode().equals(stage.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有执行中的赛段可以关闭");
    }
    if (stage.getEndAt() != null && LocalDateTime.now().isBefore(stage.getEndAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛段结束时间尚未到达");
    }
  }

  public CompetitionStageExecutionPlanEntity requirePublishedPlan(
      Long tenantId,
      Long planId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long levelId) {
    CompetitionStageExecutionPlanEntity plan = planMapper.selectOne(new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
        .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
        .eq(CompetitionStageExecutionPlanEntity::getId, planId)
        .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0));
    if (plan == null || !Objects.equals(plan.getEventId(), eventId)
        || !Objects.equals(plan.getStageId(), stageId)
        || !Objects.equals(plan.getEventWorkTypeId(), eventWorkTypeId)
        || !Objects.equals(plan.getEventWorkTypeLevelId(), levelId)
        || !"published".equals(plan.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "执行对象不属于当前已发布执行方案");
    }
    return plan;
  }

  public CompetitionStageRosterItemEntity requireIncludedRosterItem(
      Long tenantId,
      CompetitionStageExecutionPlanEntity plan,
      Long rosterItemId,
      Long contestantMemberId) {
    if (plan.getRosterSnapshotId() == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "执行方案尚未发布有效名单");
    }
    CompetitionStageRosterItemEntity item = rosterItemMapper.selectOne(new LambdaQueryWrapper<CompetitionStageRosterItemEntity>()
        .eq(CompetitionStageRosterItemEntity::getTenantId, tenantId)
        .eq(CompetitionStageRosterItemEntity::getId, rosterItemId)
        .eq(CompetitionStageRosterItemEntity::getSnapshotId, plan.getRosterSnapshotId())
        .eq(CompetitionStageRosterItemEntity::getContestantMemberId, contestantMemberId)
        .eq(CompetitionStageRosterItemEntity::getDeleted, 0));
    if (item == null || !"included".equals(item.getInclusionStatus())
        || !Objects.equals(item.getEventId(), plan.getEventId())
        || !Objects.equals(item.getStageId(), plan.getStageId())
        || !Objects.equals(item.getEventWorkTypeId(), plan.getEventWorkTypeId())
        || !Objects.equals(item.getEventWorkTypeLevelId(), plan.getEventWorkTypeLevelId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手名单项不属于当前执行方案");
    }
    return item;
  }

  public void requireInteractiveWrite(Long tenantId, Long stageId) {
    CompetitionStageEntity stage = lockStage(tenantId, stageId);
    requireStageInProgress(stage);
    LocalDateTime now = LocalDateTime.now();
    if (stage.getStartAt() != null && now.isBefore(stage.getStartAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛段尚未开始，不能写入成绩");
    }
    if (stage.getEndAt() != null && !now.isBefore(stage.getEndAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛段已到截止时间，不能写入成绩");
    }
  }

  public void requireStageInProgress(CompetitionStageEntity stage) {
    if (stage == null || !CompetitionStageStatus.IN_PROGRESS.getCode().equals(stage.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛段不允许执行交互式操作");
    }
  }

  public void requireStageAllocationWritable(CompetitionStageEntity stage) {
    CompetitionStageStatus status = CompetitionStageStatus.fromCode(stage == null ? null : stage.getStatus());
    if (status == null || !CompetitionStageStatus.allocationWritableStatuses().contains(status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛段不允许调整分场或裁判分配");
    }
  }

  public void requireSettlementReady(Long tenantId, CompetitionStageEntity stage) {
    if (stage == null || !CompetitionStageStatus.CLOSED.getCode().equals(stage.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛段必须关闭后才能标记结算");
    }
    List<CompetitionStageSettlementEntity> settlements = settlementMapper.selectList(new LambdaQueryWrapper<CompetitionStageSettlementEntity>()
        .eq(CompetitionStageSettlementEntity::getTenantId, tenantId)
        .eq(CompetitionStageSettlementEntity::getEventId, stage.getEventId())
        .eq(CompetitionStageSettlementEntity::getStageId, stage.getId())
        .eq(CompetitionStageSettlementEntity::getStatus, "draft")
        .eq(CompetitionStageSettlementEntity::getReadinessStatus, "passed")
        .eq(CompetitionStageSettlementEntity::getDeleted, 0));
    if (settlements == null || settlements.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "没有完成计算的有效结算事实");
    }
    List<CompetitionStageExecutionPlanEntity> plans = publishedPlans(tenantId, stage);
    if (plans.isEmpty() || plans.stream().anyMatch(plan -> settlements.stream()
        .noneMatch(settlement -> Objects.equals(settlement.getPlanId(), plan.getId())))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "存在执行方案尚未完成有效结算事实");
    }
    for (CompetitionStageSettlementEntity settlement : settlements) {
      Long itemCount = settlementItemMapper.selectCount(new LambdaQueryWrapper<CompetitionStageSettlementItemEntity>()
          .eq(CompetitionStageSettlementItemEntity::getTenantId, tenantId)
          .eq(CompetitionStageSettlementItemEntity::getSettlementId, settlement.getId())
          .eq(CompetitionStageSettlementItemEntity::getDeleted, 0));
      if (itemCount == null || itemCount == 0) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "结算没有排名事实");
      }
    }
  }

  public void requireResultPublishable(Long tenantId, CompetitionStageEntity stage) {
    if (stage == null || !CompetitionStageStatus.SETTLED.getCode().equals(stage.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有已结算赛段可以发布结果");
    }
    List<CompetitionStageSettlementEntity> settlements = settlementMapper.selectList(new LambdaQueryWrapper<CompetitionStageSettlementEntity>()
        .eq(CompetitionStageSettlementEntity::getTenantId, tenantId)
        .eq(CompetitionStageSettlementEntity::getEventId, stage.getEventId())
        .eq(CompetitionStageSettlementEntity::getStageId, stage.getId())
        .eq(CompetitionStageSettlementEntity::getStatus, "published")
        .eq(CompetitionStageSettlementEntity::getDeleted, 0));
    if (settlements == null || settlements.isEmpty()
        || settlements.stream().anyMatch(item -> item.getRankingSnapshotId() == null
        || item.getPromotionSnapshotId() == null)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "排名或晋级快照尚未生成");
    }
    List<CompetitionStageExecutionPlanEntity> plans = publishedPlans(tenantId, stage);
    if (plans.isEmpty() || plans.stream().anyMatch(plan -> settlements.stream()
        .noneMatch(settlement -> Objects.equals(settlement.getPlanId(), plan.getId())))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "存在执行方案尚未生成排名或晋级快照");
    }
  }

  private List<CompetitionStageExecutionPlanEntity> publishedPlans(
      Long tenantId, CompetitionStageEntity stage) {
    List<CompetitionStageExecutionPlanEntity> plans = planMapper.selectList(new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
        .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
        .eq(CompetitionStageExecutionPlanEntity::getEventId, stage.getEventId())
        .eq(CompetitionStageExecutionPlanEntity::getStageId, stage.getId())
        .eq(CompetitionStageExecutionPlanEntity::getStatus, "published")
        .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0));
    return plans == null ? List.of() : plans;
  }

  private void requirePublishedRoster(Long tenantId, CompetitionStageExecutionPlanEntity plan) {
    if (plan.getRosterSnapshotId() == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "执行方案没有已发布名单");
    }
    CompetitionStageRosterSnapshotEntity snapshot = rosterSnapshotMapper.selectOne(new LambdaQueryWrapper<CompetitionStageRosterSnapshotEntity>()
        .eq(CompetitionStageRosterSnapshotEntity::getTenantId, tenantId)
        .eq(CompetitionStageRosterSnapshotEntity::getId, plan.getRosterSnapshotId())
        .eq(CompetitionStageRosterSnapshotEntity::getPlanId, plan.getId())
        .eq(CompetitionStageRosterSnapshotEntity::getStatus, "published")
        .eq(CompetitionStageRosterSnapshotEntity::getDeleted, 0));
    if (snapshot == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "执行方案名单尚未发布");
    }
    Long count = rosterItemMapper.selectCount(new LambdaQueryWrapper<CompetitionStageRosterItemEntity>()
        .eq(CompetitionStageRosterItemEntity::getTenantId, tenantId)
        .eq(CompetitionStageRosterItemEntity::getSnapshotId, snapshot.getId())
        .eq(CompetitionStageRosterItemEntity::getInclusionStatus, "included")
        .eq(CompetitionStageRosterItemEntity::getDeleted, 0));
    if (count == null || count == 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "执行方案名单没有有效选手");
    }
  }

  private void requireWindow(CompetitionStageEntity stage) {
    if (stage.getStartAt() == null || stage.getEndAt() == null
        || !stage.getEndAt().isAfter(stage.getStartAt())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛段时间窗口未配置完成");
    }
  }
}
