package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePromotionItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePromotionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionStageReferenceSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionStageReferenceSnapshotItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageFinalPodiumEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionStageSettlementEntity;
import com.guoyu.platform.competition.entity.CompetitionStageSettlementItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageUnlockOutboxEntity;
import com.guoyu.platform.competition.entity.CompetitionTotalScoreSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.enums.CompetitionStageContentType;
import com.guoyu.platform.competition.domain.CompetitionPromotionRuleDomainService;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePromotionItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePromotionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageReferenceSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageReferenceSnapshotItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageFinalPodiumMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageSettlementItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageSettlementMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageUnlockOutboxMapper;
import com.guoyu.platform.competition.mapper.CompetitionTotalScoreSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.dto.CompetitionStageSettlementCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageFinalPodiumReq;
import com.guoyu.platform.competition.vo.CompetitionStageSettlementPreflightCheckVO;
import com.guoyu.platform.competition.vo.CompetitionStageSettlementPreflightVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionStageSettlementService {
  private final CompetitionStageSettlementMapper settlementMapper;
  private final CompetitionStageSettlementItemMapper settlementItemMapper;
  private final CompetitionStagePromotionSnapshotMapper promotionMapper;
  private final CompetitionStagePromotionItemMapper promotionItemMapper;
  private final CompetitionStageUnlockOutboxMapper outboxMapper;
  private final CompetitionScoreRuleMapper scoreRuleMapper;
  private final CompetitionTotalScoreSnapshotMapper totalMapper;
  private final CompetitionRankingSnapshotMapper rankingMapper;
  private final CompetitionRankingEntryMapper rankingEntryMapper;
  private final CompetitionContestantProfileMapper profileMapper;
  private final CompetitionStageExecutionPlanService planService;
  private final CompetitionStageContentService contentService;
  private final CompetitionStageService stageService;
  private final CompetitionStageRosterItemMapper rosterItemMapper;
  private final CompetitionStageRosterSnapshotMapper rosterSnapshotMapper;
  private final CompetitionStageReferenceSnapshotMapper referenceMapper;
  private final CompetitionStageReferenceSnapshotItemMapper referenceItemMapper;
  private final CompetitionStageFinalPodiumMapper finalPodiumMapper;
  private final CompetitionPromotionRuleDomainService promotionRules = new CompetitionPromotionRuleDomainService();

  public CompetitionStageSettlementService(
      CompetitionStageSettlementMapper settlementMapper,
      CompetitionStageSettlementItemMapper settlementItemMapper,
      CompetitionStagePromotionSnapshotMapper promotionMapper,
      CompetitionStagePromotionItemMapper promotionItemMapper,
      CompetitionStageUnlockOutboxMapper outboxMapper,
      CompetitionScoreRuleMapper scoreRuleMapper,
      CompetitionTotalScoreSnapshotMapper totalMapper,
      CompetitionRankingSnapshotMapper rankingMapper,
      CompetitionRankingEntryMapper rankingEntryMapper,
      CompetitionContestantProfileMapper profileMapper,
      CompetitionStageExecutionPlanService planService,
      CompetitionStageContentService contentService,
      CompetitionStageService stageService,
      CompetitionStageRosterItemMapper rosterItemMapper,
      CompetitionStageRosterSnapshotMapper rosterSnapshotMapper,
      CompetitionStageReferenceSnapshotMapper referenceMapper,
      CompetitionStageReferenceSnapshotItemMapper referenceItemMapper,
      CompetitionStageFinalPodiumMapper finalPodiumMapper) {
    this.settlementMapper = settlementMapper;
    this.settlementItemMapper = settlementItemMapper;
    this.promotionMapper = promotionMapper;
    this.promotionItemMapper = promotionItemMapper;
    this.outboxMapper = outboxMapper;
    this.scoreRuleMapper = scoreRuleMapper;
    this.totalMapper = totalMapper;
    this.rankingMapper = rankingMapper;
    this.rankingEntryMapper = rankingEntryMapper;
    this.profileMapper = profileMapper;
    this.planService = planService;
    this.contentService = contentService;
    this.stageService = stageService;
    this.rosterItemMapper = rosterItemMapper;
    this.rosterSnapshotMapper = rosterSnapshotMapper;
    this.referenceMapper = referenceMapper;
    this.referenceItemMapper = referenceItemMapper;
    this.finalPodiumMapper = finalPodiumMapper;
  }

  @Transactional(readOnly = true)
  public CompetitionStageSettlementPreflightVO preflight(Long tenantId,
      CompetitionStageExecutionPlanEntity plan, Long scoreRuleId, List<Long> absentIds) {
    List<CompetitionStageSettlementPreflightCheckVO> checks = new ArrayList<>();
    CompetitionStageEntity stage = stageService.getById(tenantId, plan.getStageId());
    boolean closed = "closed".equals(stage.getStatus()) || "settled".equals(stage.getStatus());
    checks.add(check("STAGE_NOT_CLOSED", closed, closed ? "赛段已关闭" : "赛段尚未关闭"));
    boolean publishedPlan = "published".equals(plan.getStatus());
    checks.add(check("PLAN_NOT_PUBLISHED", publishedPlan, publishedPlan ? "执行方案已发布" : "执行方案尚未发布"));
    boolean frozenRule = plan.getScoreRuleId() != null && Objects.equals(plan.getScoreRuleId(), scoreRuleId);
    checks.add(check("SCORE_RULE_NOT_FROZEN", frozenRule,
        frozenRule ? "计分规则已随执行方案冻结" : "请求计分规则不是执行方案冻结版本"));
    List<CompetitionStageContentEntity> contents = contentService.list(tenantId, plan.getStageId()).stream()
        .filter(c -> "enabled".equals(c.getStatus())).toList();
    boolean simulation = contents.stream().noneMatch(c -> CompetitionStageContentType.SIMULATION.getCode().equals(c.getContentType()));
    checks.add(check("SIMULATION_PROVIDER_UNAVAILABLE", simulation,
        simulation ? "未启用模拟内容" : "模拟 Provider 尚未实现，本期不可结算"));
    List<CompetitionStageRosterItemEntity> roster = publishedRoster(tenantId, plan);
    boolean rosterReady = !roster.isEmpty();
    checks.add(check("ROSTER_NOT_PUBLISHED", rosterReady, rosterReady ? "名单快照已发布" : "名单快照尚未发布"));
    Set<Long> absent = absentIds == null ? Set.of() : new HashSet<>(absentIds);
    Set<Long> rosterMembers = roster.stream().map(CompetitionStageRosterItemEntity::getContestantMemberId)
        .collect(java.util.stream.Collectors.toSet());
    checks.add(check("ABSENT_OUTSIDE_ROSTER", rosterMembers.containsAll(absent),
        rosterMembers.containsAll(absent) ? "缺考名单属于当前名单" : "缺考名单包含非当前选手"));
    boolean allResults = true;
    for (CompetitionStageRosterItemEntity item : roster) {
      if ("included".equals(item.getInclusionStatus()) && !absent.contains(item.getContestantMemberId())
          && !hasConfirmedTotal(tenantId, plan, scoreRuleId, item.getContestantMemberId())) allResults = false;
    }
    checks.add(check("CONTESTANT_NO_RESULT", allResults,
        allResults ? "参赛选手结果已齐备或已明确缺考" : "仍存在未确认总分的无结果选手"));
    boolean ruleExists = scoreRule(tenantId, plan, scoreRuleId) != null;
    checks.add(check("SCORE_RULE_NOT_PUBLISHED", ruleExists, ruleExists ? "计分规则已发布" : "计分规则不存在或范围不一致"));
    CompetitionPromotionRuleDomainService.ResolvedRule frozenPromotion = effectiveRule(plan);
    boolean targetValid = plan.getTargetStageId() == null
        || (frozenPromotion.legacy() && plan.getPromotionQuota() != null)
        || CompetitionPromotionRuleDomainService.FIXED_COUNT.equals(frozenPromotion.type())
        || CompetitionPromotionRuleDomainService.PERCENTAGE.equals(frozenPromotion.type());
    checks.add(check("PROMOTION_SCOPE_NOT_FROZEN", targetValid,
        targetValid ? "晋级目标和额度已冻结" : "晋级配置尚未冻结"));
    return new CompetitionStageSettlementPreflightVO(plan.getId(),
        checks.stream().allMatch(CompetitionStageSettlementPreflightCheckVO::passed), checks);
  }

  @Transactional
  public CompetitionStageSettlementEntity create(Long tenantId, Long userId, Long planId,
      CompetitionStageSettlementCreateReq req) {
    CompetitionStageExecutionPlanEntity plan = planService.get(tenantId, planId);
    if (req.planId() != null && !Objects.equals(req.planId(), planId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "路径方案与请求方案不一致");
    }
    CompetitionRankingSnapshotEntity sourceRanking = requirePublishedRanking(
        tenantId, plan, req.rankingSnapshotId(), req.scoreRuleId());
    List<CompetitionRankingEntryEntity> sourceEntries = rankingEntryMapper.selectList(
        new LambdaQueryWrapper<CompetitionRankingEntryEntity>()
            .eq(CompetitionRankingEntryEntity::getTenantId, tenantId)
            .eq(CompetitionRankingEntryEntity::getRankingSnapshotId, sourceRanking.getId())
            .eq(CompetitionRankingEntryEntity::getDeleted, 0)
            .orderByAsc(CompetitionRankingEntryEntity::getSortOrder));
    Set<Long> rankedAbsent = (sourceEntries == null ? List.<CompetitionRankingEntryEntity>of() : sourceEntries)
        .stream().filter(e -> "absent".equals(e.getContestantStatus()))
        .map(CompetitionRankingEntryEntity::getContestantMemberId).collect(java.util.stream.Collectors.toSet());
    CompetitionStageSettlementPreflightVO preflight = preflight(
        tenantId, plan, req.scoreRuleId(), new ArrayList<>(rankedAbsent));
    if (!preflight.ready()) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "结算预检未通过");
    validateFrozenPromotion(plan, req.promotionQuota(), req.targetStageId());
    CompetitionScoreRuleEntity scoreRule = scoreRule(tenantId, plan, req.scoreRuleId());
    if (scoreRule == null) throw new BusinessException(ErrorCode.CONFLICT, "计分规则不属于当前执行方案");
    List<CompetitionStageSettlementEntity> old = settlementMapper.selectList(new LambdaQueryWrapper<CompetitionStageSettlementEntity>()
        .eq(CompetitionStageSettlementEntity::getTenantId, tenantId).eq(CompetitionStageSettlementEntity::getPlanId, plan.getId())
        .eq(CompetitionStageSettlementEntity::getDeleted, 0).orderByDesc(CompetitionStageSettlementEntity::getSettlementVersion));
    if (old != null && !old.isEmpty() && "draft".equals(old.get(0).getStatus())) return old.get(0);
    int version = (old == null ? 0 : old.stream().map(CompetitionStageSettlementEntity::getSettlementVersion)
        .filter(Objects::nonNull).max(Integer::compareTo).orElse(0)) + 1;
    CompetitionStageSettlementEntity settlement = new CompetitionStageSettlementEntity();
    settlement.setId(IdWorker.getId()); settlement.setTenantId(tenantId); settlement.setPlanId(plan.getId());
    settlement.setEventId(plan.getEventId()); settlement.setStageId(plan.getStageId());
    settlement.setEventWorkTypeId(plan.getEventWorkTypeId()); settlement.setEventWorkTypeLevelId(plan.getEventWorkTypeLevelId());
    settlement.setScoreRuleId(scoreRule.getId()); settlement.setSettlementVersion(version); settlement.setStatus("draft");
    settlement.setRankingSnapshotId(sourceRanking.getId());
    settlement.setReadinessStatus("passed"); settlement.setCreatedBy(userId); settlement.setUpdatedBy(userId);
    if (settlementMapper.insert(settlement) != 1) throw new BusinessException(ErrorCode.CONFLICT, "结算快照保存失败");
    for (CompetitionRankingEntryEntity ranked :
        sourceEntries == null ? List.<CompetitionRankingEntryEntity>of() : sourceEntries) {
      CompetitionStageSettlementItemEntity item = new CompetitionStageSettlementItemEntity();
      item.setId(IdWorker.getId()); item.setTenantId(tenantId); item.setSettlementId(settlement.getId());
      item.setContestantMemberId(ranked.getContestantMemberId()); item.setTotalScore(ranked.getTotalScore());
      item.setTotalScoreSnapshotId(ranked.getTotalScoreSnapshotId());
      item.setContestantStatus(ranked.getContestantStatus()); item.setDisplayRank(ranked.getDisplayRank());
      item.setTiedFlag(ranked.getTiedFlag()); item.setPromotionStatus("not_selected"); item.setCreatedBy(userId); item.setUpdatedBy(userId);
      if (settlementItemMapper.insert(item) != 1) throw new BusinessException(ErrorCode.CONFLICT, "结算条目保存失败");
    }
    return settlement;
  }

  @Transactional
  public CompetitionStageSettlementEntity publish(Long tenantId, Long userId, Long settlementId) {
    CompetitionStageSettlementEntity settlement = lockSettlement(tenantId, settlementId);
    CompetitionPromotionRuleDomainService.ResolvedRule promotionRule;
    CompetitionStageReferenceSnapshotEntity reference;
    int calculatedTargetCount;
    if ("published".equals(settlement.getStatus())) return settlement;
    if (!"draft".equals(settlement.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "仅草稿结算允许发布");
    CompetitionStageExecutionPlanEntity plan = planService.get(tenantId, settlement.getPlanId());
    if (!"published".equals(plan.getStatus())
        || !Objects.equals(plan.getEventId(), settlement.getEventId()) || !Objects.equals(plan.getStageId(), settlement.getStageId())
        || !Objects.equals(plan.getScoreRuleId(), settlement.getScoreRuleId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "结算事实与冻结执行方案范围不一致");
    }
    CompetitionStageEntity stage = stageService.getById(tenantId, settlement.getStageId());
    if (!"settled".equals(stage.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "赛段尚未标记为已结算");
    List<CompetitionStageSettlementItemEntity> items = items(tenantId, settlementId);
    if (items.isEmpty()) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "结算没有排名事实");
    CompetitionRankingSnapshotEntity ranking = requirePublishedRanking(
        tenantId, plan, settlement.getRankingSnapshotId(), settlement.getScoreRuleId());
    promotionRule = effectiveRule(plan);
    reference = reference(tenantId, settlementId);
    if (plan.getTargetStageId() != null && !promotionRule.legacy()
        && !CompetitionPromotionRuleDomainService.FINAL_PODIUM.equals(promotionRule.type())
        && (reference == null || !"confirmed".equals(reference.getStatus()))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先确认参考名单后再发布晋级结果");
    }
    CompetitionStageFinalPodiumEntity podium = finalPodium(tenantId, settlementId);
    if (CompetitionPromotionRuleDomainService.FINAL_PODIUM.equals(promotionRule.type())
        && (podium == null || !"confirmed".equals(podium.getStatus()))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先确认决赛前三名后再发布决赛结果");
    }
    if (podium != null && CompetitionPromotionRuleDomainService.FINAL_PODIUM.equals(promotionRule.type())) {
      validatePodiumMembers(podium, items);
    }
    int baseCount = (int) items.stream()
        .filter(item -> "completed".equals(item.getContestantStatus())).count();
    calculatedTargetCount = reference == null
        ? promotionRules.calculateTargetCount(baseCount, promotionRule)
        : reference.getCalculatedTargetCount();
    if (podium != null && CompetitionPromotionRuleDomainService.FINAL_PODIUM.equals(promotionRule.type())) {
      podium.setStatus("published"); podium.setPublishedBy(userId); podium.setPublishedAt(LocalDateTime.now()); podium.setUpdatedBy(userId);
      if (finalPodiumMapper.updateById(podium) != 1) throw new BusinessException(ErrorCode.CONFLICT, "决赛前三名发布失败");
    }
    CompetitionStagePromotionSnapshotEntity promotion = new CompetitionStagePromotionSnapshotEntity(); promotion.setId(IdWorker.getId()); promotion.setTenantId(tenantId); promotion.setSettlementId(settlementId); promotion.setSourcePlanId(settlement.getPlanId()); promotion.setSourceStageId(settlement.getStageId()); promotion.setTargetStageId(plan.getTargetStageId()); promotion.setEventId(settlement.getEventId()); promotion.setEventWorkTypeId(settlement.getEventWorkTypeId()); promotion.setEventWorkTypeLevelId(settlement.getEventWorkTypeLevelId()); promotion.setQuota(plan.getTargetStageId() == null ? 0 : calculatedTargetCount); promotion.setPromotionRuleType(promotionRule.type()); promotion.setPromotionPercentage(promotionRule.percentage()); promotion.setCalculatedTargetCount(calculatedTargetCount); promotion.setReferenceStatus(reference == null ? "not_generated" : reference.getStatus()); promotion.setPromotionStatus(plan.getTargetStageId() == null ? "not_generated" : "published"); promotion.setFinalPodiumStatus(CompetitionPromotionRuleDomainService.FINAL_PODIUM.equals(promotionRule.type()) ? "published" : "not_generated"); promotion.setPromotionVersion(1); promotion.setStatus("published"); promotion.setPublishedAt(LocalDateTime.now()); promotion.setPublishedBy(userId); promotion.setCreatedBy(userId); promotion.setUpdatedBy(userId);
    if (promotionMapper.insert(promotion) != 1) throw new BusinessException(ErrorCode.CONFLICT, "晋级快照保存失败");
    for (CompetitionStageSettlementItemEntity item : items) {
      boolean selected = "completed".equals(item.getContestantStatus()) && item.getDisplayRank() != null && plan.getTargetStageId() != null && item.getDisplayRank() <= promotion.getQuota();
      item.setPromotionStatus(selected ? "selected" : "not_selected"); item.setUpdatedBy(userId); if (settlementItemMapper.updateById(item) != 1) throw new BusinessException(ErrorCode.CONFLICT, "结算晋级状态保存失败");
      if (!"completed".equals(item.getContestantStatus()) || item.getDisplayRank() == null) continue;
      CompetitionStagePromotionItemEntity promotionItem = new CompetitionStagePromotionItemEntity(); promotionItem.setId(IdWorker.getId()); promotionItem.setTenantId(tenantId); promotionItem.setPromotionSnapshotId(promotion.getId()); promotionItem.setContestantMemberId(item.getContestantMemberId()); promotionItem.setSourceRank(item.getDisplayRank()); promotionItem.setSelectionStatus(selected ? "selected" : "not_selected"); promotionItem.setReason(selected ? "frozen_quota" : "outside_frozen_quota"); promotionItem.setCreatedBy(userId); promotionItem.setUpdatedBy(userId); if (promotionItemMapper.insert(promotionItem) != 1) throw new BusinessException(ErrorCode.CONFLICT, "晋级条目保存失败");
      if (selected) {
        CompetitionStageUnlockOutboxEntity outbox = new CompetitionStageUnlockOutboxEntity(); outbox.setId(IdWorker.getId()); outbox.setTenantId(tenantId); outbox.setSourceStageId(settlement.getStageId()); outbox.setTargetStageId(plan.getTargetStageId()); outbox.setEventWorkTypeId(settlement.getEventWorkTypeId()); outbox.setEventWorkTypeLevelId(settlement.getEventWorkTypeLevelId()); outbox.setPromotionSnapshotId(promotion.getId()); outbox.setContestantMemberId(item.getContestantMemberId()); outbox.setEventType("promotion_published"); outbox.setStatus("pending"); outbox.setAttempts(0); outbox.setCreatedBy(userId); outbox.setUpdatedBy(userId); if (outboxMapper.insert(outbox) != 1) throw new BusinessException(ErrorCode.CONFLICT, "晋级解锁事件保存失败");
        CompetitionContestantProfileEntity profile = profileMapper.selectForUpdateByTenantMemberAndWorkType(
            tenantId, item.getContestantMemberId(), settlement.getEventWorkTypeId()); if (profile != null) { profile.setCurrentStageId(plan.getTargetStageId()); profile.setPromotionStatus("promoted"); profile.setUpdatedBy(userId); profileMapper.updateById(profile); }
      }
    }
    settlement.setRankingSnapshotId(ranking.getId()); settlement.setPromotionSnapshotId(promotion.getId()); settlement.setStatus("published"); settlement.setPublishedAt(LocalDateTime.now()); settlement.setPublishedBy(userId); settlement.setUpdatedBy(userId); if (settlementMapper.updateById(settlement) != 1) throw new BusinessException(ErrorCode.CONFLICT, "结算发布失败");
    return settlement;
  }

  @Transactional(readOnly = true)
  public CompetitionStageSettlementEntity get(Long tenantId, Long id) { CompetitionStageSettlementEntity row = settlementMapper.selectOne(new LambdaQueryWrapper<CompetitionStageSettlementEntity>().eq(CompetitionStageSettlementEntity::getTenantId, tenantId).eq(CompetitionStageSettlementEntity::getId, id).eq(CompetitionStageSettlementEntity::getDeleted, 0)); if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "结算快照不存在"); return row; }

  @Transactional(readOnly = true)
  public List<CompetitionStageSettlementEntity> list(Long tenantId, Long planId) {
    List<CompetitionStageSettlementEntity> rows = settlementMapper.selectList(new LambdaQueryWrapper<CompetitionStageSettlementEntity>()
        .eq(CompetitionStageSettlementEntity::getTenantId, tenantId)
        .eq(CompetitionStageSettlementEntity::getPlanId, planId)
        .eq(CompetitionStageSettlementEntity::getDeleted, 0)
        .orderByDesc(CompetitionStageSettlementEntity::getSettlementVersion));
    return rows == null ? List.of() : rows;
  }

  @Transactional(readOnly = true)
  public CompetitionStagePromotionSnapshotEntity promotion(Long tenantId, Long promotionId) {
    if (promotionId == null) return null;
    return promotionMapper.selectOne(new LambdaQueryWrapper<CompetitionStagePromotionSnapshotEntity>()
        .eq(CompetitionStagePromotionSnapshotEntity::getTenantId, tenantId)
        .eq(CompetitionStagePromotionSnapshotEntity::getId, promotionId)
        .eq(CompetitionStagePromotionSnapshotEntity::getDeleted, 0));
  }

  @Transactional
  public CompetitionStageReferenceSnapshotEntity createReference(Long tenantId, Long userId, Long settlementId) {
    CompetitionStageSettlementEntity settlement = get(tenantId, settlementId);
    CompetitionStageReferenceSnapshotEntity existing = reference(tenantId, settlementId);
    if (existing != null) return existing;
    CompetitionStageExecutionPlanEntity plan = planService.get(tenantId, settlement.getPlanId());
    CompetitionPromotionRuleDomainService.ResolvedRule rule = effectiveRule(plan);
    if (CompetitionPromotionRuleDomainService.FINAL_PODIUM.equals(rule.type())) {
      throw new BusinessException(ErrorCode.CONFLICT, "决赛前三名规则不生成晋级参考名单");
    }
    List<CompetitionStageSettlementItemEntity> settlementItems = items(tenantId, settlementId);
    int baseCount = (int) settlementItems.stream()
        .filter(item -> "completed".equals(item.getContestantStatus())).count();
    int targetCount = promotionRules.calculateTargetCount(baseCount, rule);
    CompetitionStageReferenceSnapshotEntity snapshot = new CompetitionStageReferenceSnapshotEntity();
    snapshot.setId(IdWorker.getId()); snapshot.setTenantId(tenantId); snapshot.setSettlementId(settlementId);
    snapshot.setPlanId(plan.getId()); snapshot.setEventId(plan.getEventId()); snapshot.setStageId(plan.getStageId());
    snapshot.setTargetStageId(plan.getTargetStageId()); snapshot.setEventWorkTypeId(plan.getEventWorkTypeId());
    snapshot.setEventWorkTypeLevelId(plan.getEventWorkTypeLevelId()); snapshot.setPromotionRuleType(rule.type());
    snapshot.setPromotionQuota(rule.quota()); snapshot.setPromotionPercentage(rule.percentage());
    snapshot.setBaseCount(baseCount); snapshot.setCalculatedTargetCount(targetCount); snapshot.setStatus("draft");
    snapshot.setCreatedBy(userId); snapshot.setUpdatedBy(userId);
    if (referenceMapper.insert(snapshot) != 1) throw new BusinessException(ErrorCode.CONFLICT, "参考名单快照保存失败");
    for (CompetitionStageSettlementItemEntity item : settlementItems) {
      if (!"completed".equals(item.getContestantStatus())) continue;
      CompetitionStageReferenceSnapshotItemEntity refItem = new CompetitionStageReferenceSnapshotItemEntity();
      refItem.setId(IdWorker.getId()); refItem.setTenantId(tenantId); refItem.setReferenceSnapshotId(snapshot.getId());
      refItem.setContestantMemberId(item.getContestantMemberId()); refItem.setSourceRank(item.getDisplayRank());
      boolean selected = promotionRules.selectedByRank(item.getDisplayRank(), targetCount);
      refItem.setSelectionStatus(selected ? "selected" : "not_selected");
      refItem.setReason(selected ? "calculated_target_count" : "outside_calculated_target_count");
      refItem.setCreatedBy(userId); refItem.setUpdatedBy(userId);
      if (referenceItemMapper.insert(refItem) != 1) throw new BusinessException(ErrorCode.CONFLICT, "参考名单条目保存失败");
    }
    return snapshot;
  }

  @Transactional
  public CompetitionStageReferenceSnapshotEntity confirmReference(Long tenantId, Long userId, Long settlementId) {
    CompetitionStageReferenceSnapshotEntity snapshot = reference(tenantId, settlementId);
    if (snapshot == null) throw new BusinessException(ErrorCode.NOT_FOUND, "参考名单快照不存在");
    if ("confirmed".equals(snapshot.getStatus())) return snapshot;
    if (!"draft".equals(snapshot.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "当前参考名单不可确认");
    snapshot.setStatus("confirmed"); snapshot.setConfirmedBy(userId); snapshot.setConfirmedAt(LocalDateTime.now()); snapshot.setUpdatedBy(userId);
    if (referenceMapper.updateById(snapshot) != 1) throw new BusinessException(ErrorCode.CONFLICT, "参考名单确认失败");
    return snapshot;
  }

  @Transactional(readOnly = true)
  public List<CompetitionStageReferenceSnapshotItemEntity> referenceItems(Long tenantId, Long snapshotId) {
    List<CompetitionStageReferenceSnapshotItemEntity> rows = referenceItemMapper.selectList(new LambdaQueryWrapper<CompetitionStageReferenceSnapshotItemEntity>()
        .eq(CompetitionStageReferenceSnapshotItemEntity::getTenantId, tenantId)
        .eq(CompetitionStageReferenceSnapshotItemEntity::getReferenceSnapshotId, snapshotId)
        .eq(CompetitionStageReferenceSnapshotItemEntity::getDeleted, 0)
        .orderByAsc(CompetitionStageReferenceSnapshotItemEntity::getSourceRank));
    return rows == null ? List.of() : rows;
  }

  @Transactional(readOnly = true)
  public CompetitionStageReferenceSnapshotEntity reference(Long tenantId, Long settlementId) {
    return referenceMapper.selectOne(new LambdaQueryWrapper<CompetitionStageReferenceSnapshotEntity>()
        .eq(CompetitionStageReferenceSnapshotEntity::getTenantId, tenantId)
        .eq(CompetitionStageReferenceSnapshotEntity::getSettlementId, settlementId)
        .eq(CompetitionStageReferenceSnapshotEntity::getDeleted, 0));
  }

  @Transactional
  public CompetitionStageFinalPodiumEntity saveFinalPodium(Long tenantId, Long userId, Long settlementId, CompetitionStageFinalPodiumReq req) {
    CompetitionStageSettlementEntity settlement = get(tenantId, settlementId);
    List<CompetitionStageSettlementItemEntity> items = items(tenantId, settlementId);
    Set<Long> completed = items.stream().filter(item -> "completed".equals(item.getContestantStatus()))
        .map(CompetitionStageSettlementItemEntity::getContestantMemberId).collect(java.util.stream.Collectors.toSet());
    List<Long> podium = java.util.Arrays.asList(req.firstContestantMemberId(), req.secondContestantMemberId(), req.thirdContestantMemberId());
    promotionRules.validateFinalPodium(podium, completed.size(), req.specialExplanation());
    for (Long memberId : podium) if (memberId != null && !completed.contains(memberId)) throw new BusinessException(ErrorCode.BAD_REQUEST, "决赛前三名必须来自有效完赛选手");
    CompetitionStageFinalPodiumEntity row = finalPodium(tenantId, settlementId);
    if (row != null && !"draft".equals(row.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "已确认的决赛前三名不可直接修改");
    if (row == null) {
      row = new CompetitionStageFinalPodiumEntity(); row.setId(IdWorker.getId()); row.setTenantId(tenantId);
      row.setSettlementId(settlementId); row.setPlanId(settlement.getPlanId()); row.setEventId(settlement.getEventId());
      row.setStageId(settlement.getStageId()); row.setCreatedBy(userId); row.setDeleted(0);
    }
    row.setFirstContestantMemberId(req.firstContestantMemberId()); row.setSecondContestantMemberId(req.secondContestantMemberId());
    row.setThirdContestantMemberId(req.thirdContestantMemberId()); row.setSpecialExplanation(req.specialExplanation()); row.setStatus("draft"); row.setUpdatedBy(userId);
    if (row.getCreatedAt() == null) finalPodiumMapper.insert(row); else finalPodiumMapper.updateById(row);
    return row;
  }

  @Transactional
  public CompetitionStageFinalPodiumEntity confirmFinalPodium(Long tenantId, Long userId, Long settlementId) {
    CompetitionStageFinalPodiumEntity row = finalPodium(tenantId, settlementId);
    if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "决赛前三名草稿不存在");
    if ("confirmed".equals(row.getStatus()) || "published".equals(row.getStatus())) return row;
    List<CompetitionStageSettlementItemEntity> items = items(tenantId, settlementId);
    Set<Long> completed = items.stream().filter(item -> "completed".equals(item.getContestantStatus()))
        .map(CompetitionStageSettlementItemEntity::getContestantMemberId).collect(java.util.stream.Collectors.toSet());
    promotionRules.validateFinalPodium(java.util.Arrays.asList(row.getFirstContestantMemberId(), row.getSecondContestantMemberId(), row.getThirdContestantMemberId()), completed.size(), row.getSpecialExplanation());
    row.setStatus("confirmed"); row.setConfirmedBy(userId); row.setConfirmedAt(LocalDateTime.now()); row.setUpdatedBy(userId);
    if (finalPodiumMapper.updateById(row) != 1) throw new BusinessException(ErrorCode.CONFLICT, "决赛前三名确认失败");
    return row;
  }

  @Transactional(readOnly = true)
  public CompetitionStageFinalPodiumEntity finalPodium(Long tenantId, Long settlementId) {
    return finalPodiumMapper.selectOne(new LambdaQueryWrapper<CompetitionStageFinalPodiumEntity>()
        .eq(CompetitionStageFinalPodiumEntity::getTenantId, tenantId)
        .eq(CompetitionStageFinalPodiumEntity::getSettlementId, settlementId)
        .eq(CompetitionStageFinalPodiumEntity::getDeleted, 0));
  }

  private CompetitionPromotionRuleDomainService.ResolvedRule effectiveRule(CompetitionStageExecutionPlanEntity plan) {
    if (plan.getPromotionRuleType() == null) {
      String type = plan.getPromotionQuota() == null ? CompetitionPromotionRuleDomainService.LEGACY : CompetitionPromotionRuleDomainService.FIXED_COUNT;
      return new CompetitionPromotionRuleDomainService.ResolvedRule(type, plan.getPromotionQuota(), null, true);
    }
    return promotionRules.resolve(plan.getPromotionRuleType(), plan.getPromotionQuota(), plan.getPromotionPercentage());
  }

  private void validatePodiumMembers(CompetitionStageFinalPodiumEntity podium, List<CompetitionStageSettlementItemEntity> items) {
    Set<Long> completed = items.stream().filter(item -> "completed".equals(item.getContestantStatus()))
        .map(CompetitionStageSettlementItemEntity::getContestantMemberId).collect(java.util.stream.Collectors.toSet());
    if (podium.getFirstContestantMemberId() != null && !completed.contains(podium.getFirstContestantMemberId())
        || podium.getSecondContestantMemberId() != null && !completed.contains(podium.getSecondContestantMemberId())
        || podium.getThirdContestantMemberId() != null && !completed.contains(podium.getThirdContestantMemberId())) throw new BusinessException(ErrorCode.CONFLICT, "决赛前三名包含无效选手");
  }

  private CompetitionStageSettlementEntity lockSettlement(Long tenantId, Long id) {
    CompetitionStageSettlementEntity row = settlementMapper.selectOne(new LambdaQueryWrapper<CompetitionStageSettlementEntity>()
        .eq(CompetitionStageSettlementEntity::getTenantId, tenantId)
        .eq(CompetitionStageSettlementEntity::getId, id)
        .eq(CompetitionStageSettlementEntity::getDeleted, 0)
        .last("FOR UPDATE"));
    if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "结算快照不存在");
    return row;
  }
  @Transactional(readOnly = true)
  public List<CompetitionStageSettlementItemEntity> items(Long tenantId, Long id) { List<CompetitionStageSettlementItemEntity> rows = settlementItemMapper.selectList(new LambdaQueryWrapper<CompetitionStageSettlementItemEntity>().eq(CompetitionStageSettlementItemEntity::getTenantId, tenantId).eq(CompetitionStageSettlementItemEntity::getSettlementId, id).eq(CompetitionStageSettlementItemEntity::getDeleted, 0).orderByAsc(CompetitionStageSettlementItemEntity::getDisplayRank)); return rows == null ? List.of() : rows; }
  private List<CompetitionStageRosterItemEntity> publishedRoster(Long tenantId, CompetitionStageExecutionPlanEntity plan) { if (plan.getRosterSnapshotId() == null) return List.of(); CompetitionStageRosterSnapshotEntity snapshot = rosterSnapshotMapper.selectOne(new LambdaQueryWrapper<CompetitionStageRosterSnapshotEntity>().eq(CompetitionStageRosterSnapshotEntity::getTenantId, tenantId).eq(CompetitionStageRosterSnapshotEntity::getId, plan.getRosterSnapshotId()).eq(CompetitionStageRosterSnapshotEntity::getPlanId, plan.getId()).eq(CompetitionStageRosterSnapshotEntity::getStatus, "published").eq(CompetitionStageRosterSnapshotEntity::getDeleted, 0)); if (snapshot == null) return List.of(); List<CompetitionStageRosterItemEntity> rows = rosterItemMapper.selectList(new LambdaQueryWrapper<CompetitionStageRosterItemEntity>().eq(CompetitionStageRosterItemEntity::getTenantId, tenantId).eq(CompetitionStageRosterItemEntity::getSnapshotId, plan.getRosterSnapshotId()).eq(CompetitionStageRosterItemEntity::getEventId, plan.getEventId()).eq(CompetitionStageRosterItemEntity::getStageId, plan.getStageId()).eq(CompetitionStageRosterItemEntity::getEventWorkTypeId, plan.getEventWorkTypeId()).eq(plan.getEventWorkTypeLevelId() != null, CompetitionStageRosterItemEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId()).isNull(plan.getEventWorkTypeLevelId() == null, CompetitionStageRosterItemEntity::getEventWorkTypeLevelId).eq(CompetitionStageRosterItemEntity::getDeleted, 0)); return rows == null ? List.of() : rows; }
  private boolean hasConfirmedTotal(Long tenantId, CompetitionStageExecutionPlanEntity plan, Long scoreRuleId, Long memberId) { return confirmedTotal(tenantId, plan, scoreRuleId, memberId) != null; }
  private CompetitionTotalScoreSnapshotEntity confirmedTotal(Long tenantId, CompetitionStageExecutionPlanEntity plan, Long scoreRuleId, Long memberId) { return totalMapper.selectOne(new LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity>().eq(CompetitionTotalScoreSnapshotEntity::getTenantId, tenantId).eq(CompetitionTotalScoreSnapshotEntity::getEventId, plan.getEventId()).eq(CompetitionTotalScoreSnapshotEntity::getStageId, plan.getStageId()).eq(CompetitionTotalScoreSnapshotEntity::getEventWorkTypeId, plan.getEventWorkTypeId()).eq(plan.getEventWorkTypeLevelId() != null, CompetitionTotalScoreSnapshotEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId()).isNull(plan.getEventWorkTypeLevelId() == null, CompetitionTotalScoreSnapshotEntity::getEventWorkTypeLevelId).eq(CompetitionTotalScoreSnapshotEntity::getContestantMemberId, memberId).eq(CompetitionTotalScoreSnapshotEntity::getScoreRuleId, scoreRuleId).eq(CompetitionTotalScoreSnapshotEntity::getCurrentFlag, 1).eq(CompetitionTotalScoreSnapshotEntity::getConfirmationStatus, "confirmed").eq(CompetitionTotalScoreSnapshotEntity::getDeleted, 0)); }
  private CompetitionScoreRuleEntity scoreRule(Long tenantId, CompetitionStageExecutionPlanEntity plan, Long scoreRuleId) { if (scoreRuleId == null || plan.getScoreRuleId() == null || !Objects.equals(plan.getScoreRuleId(), scoreRuleId)) return null; return scoreRuleMapper.selectOne(new LambdaQueryWrapper<CompetitionScoreRuleEntity>().eq(CompetitionScoreRuleEntity::getTenantId, tenantId).eq(CompetitionScoreRuleEntity::getId, scoreRuleId).eq(CompetitionScoreRuleEntity::getEventId, plan.getEventId()).eq(CompetitionScoreRuleEntity::getStageId, plan.getStageId()).eq(CompetitionScoreRuleEntity::getEventWorkTypeId, plan.getEventWorkTypeId()).eq(plan.getEventWorkTypeLevelId() != null, CompetitionScoreRuleEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId()).isNull(plan.getEventWorkTypeLevelId() == null, CompetitionScoreRuleEntity::getEventWorkTypeLevelId).eq(CompetitionScoreRuleEntity::getStatus, "published").eq(CompetitionScoreRuleEntity::getDeleted, 0)); }
  private void validateFrozenPromotion(CompetitionStageExecutionPlanEntity plan, Integer quota, Long targetStageId) { if (quota != null && !Objects.equals(quota, plan.getPromotionQuota())) throw new BusinessException(ErrorCode.CONFLICT, "请求晋级人数与冻结执行配置不一致"); if (targetStageId != null && !Objects.equals(targetStageId, plan.getTargetStageId())) throw new BusinessException(ErrorCode.CONFLICT, "请求晋级目标与冻结赛段拓扑不一致"); }
  private CompetitionRankingSnapshotEntity requirePublishedRanking(Long tenantId,
      CompetitionStageExecutionPlanEntity plan, Long rankingSnapshotId, Long scoreRuleId) {
    CompetitionRankingSnapshotEntity ranking = rankingMapper.selectOne(
        new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
            .eq(CompetitionRankingSnapshotEntity::getTenantId, tenantId)
            .eq(CompetitionRankingSnapshotEntity::getId, rankingSnapshotId)
            .eq(CompetitionRankingSnapshotEntity::getEventId, plan.getEventId())
            .eq(CompetitionRankingSnapshotEntity::getStageId, plan.getStageId())
            .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
            .eq(plan.getEventWorkTypeLevelId() != null,
                CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId,
                plan.getEventWorkTypeLevelId())
            .isNull(plan.getEventWorkTypeLevelId() == null,
                CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId)
            .eq(CompetitionRankingSnapshotEntity::getScoreRuleId, scoreRuleId)
            .eq(CompetitionRankingSnapshotEntity::getStatus, "published")
            .eq(CompetitionRankingSnapshotEntity::getDeleted, 0));
    if (ranking == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "赛段结算必须引用当前范围内已发布的排名快照");
    }
    return ranking;
  }
  private CompetitionStageSettlementPreflightCheckVO check(String code, boolean passed, String message) { return new CompetitionStageSettlementPreflightCheckVO(code, passed, message); }
  private record CompetitionStageSettlementCandidate(Long memberId, Long snapshotId, BigDecimal score, String status) { }
}
