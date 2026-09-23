package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.competition.domain.CompetitionScoringSystemBCalculator;
import com.guoyu.platform.competition.domain.CompetitionStageContentScoringWeights;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionTotalScoreSnapshotEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTotalScoreSnapshotMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionAutomaticScorePipelineService {
  private final CompetitionScoreSourceMapper sourceMapper;
  private final CompetitionScoreRuleMapper ruleMapper;
  private final CompetitionStageContentMapper stageContentMapper;
  private final CompetitionTotalScoreSnapshotMapper totalMapper;
  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final CompetitionFormalCandidateExamMapper candidateMapper;
  private final CompetitionRankingSnapshotMapper rankingMapper;
  private final CompetitionRankingEntryMapper entryMapper;
  private final CompetitionScoringSystemBCalculator calculator =
      new CompetitionScoringSystemBCalculator();

  public CompetitionAutomaticScorePipelineService(CompetitionScoreSourceMapper sourceMapper,
      CompetitionScoreRuleMapper ruleMapper, CompetitionStageContentMapper stageContentMapper,
      CompetitionTotalScoreSnapshotMapper totalMapper,
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalCandidateExamMapper candidateMapper,
      CompetitionRankingSnapshotMapper rankingMapper, CompetitionRankingEntryMapper entryMapper) {
    this.sourceMapper = sourceMapper;
    this.ruleMapper = ruleMapper;
    this.stageContentMapper = stageContentMapper;
    this.totalMapper = totalMapper;
    this.sessionMapper = sessionMapper;
    this.candidateMapper = candidateMapper;
    this.rankingMapper = rankingMapper;
    this.entryMapper = entryMapper;
  }

  @Transactional
  public CompetitionTotalScoreSnapshotEntity onConfirmedSource(
      CompetitionScoreSourceEntity source, Long operatorId) {
    if (source == null || !"confirmed".equals(source.getConfirmationStatus())) return null;
    List<CompetitionScoreSourceEntity> sources = sourceMapper.selectList(
        scopeSources(source));
    if (sources == null) return null;
    Map<String, CompetitionScoreSourceEntity> byType = sources.stream()
        .collect(Collectors.toMap(CompetitionScoreSourceEntity::getSourceType,
            Function.identity(), (left, right) -> left));
    LocalDateTime now = LocalDateTime.now();
    if (isDirectTheorySource(source)) {
      return createDirectTheoryTotal(source, byType.get("theory"), operatorId, now);
    }
    if (source.getScoreRuleId() != null) {
      CompetitionScoreRuleEntity rule = ruleMapper.selectById(source.getScoreRuleId());
      if (rule != null && "B".equalsIgnoreCase(rule.getScoringSystem())
          && "published".equals(rule.getStatus())) {
        return createRuleWeightedTotal(source, byType, rule, operatorId, now);
      }
    }
    CompetitionStageContentScoringWeights weights = stageContentWeights(source.getStageId());
    if (weights.valid() && !weights.theoryOnly()) {
      return createContentWeightedTotal(source, byType, weights, operatorId, now);
    }
    return null;
  }

  private CompetitionTotalScoreSnapshotEntity createRuleWeightedTotal(
      CompetitionScoreSourceEntity source,
      Map<String, CompetitionScoreSourceEntity> byType,
      CompetitionScoreRuleEntity rule,
      Long operatorId,
      LocalDateTime now) {
    CompetitionScoreSourceEntity theory = byType.get("theory");
    CompetitionScoreSourceEntity simulation = sourceOrDisabledZero(
        source, byType.get("simulation"), "simulation", rule, operatorId, now);
    CompetitionScoreSourceEntity practical = sourceOrDisabledZero(
        source, byType.get("practical"), "practical", rule, operatorId, now);
    if (theory == null || simulation == null || practical == null) return null;
    if (!"confirmed".equalsIgnoreCase(theory.getConfirmationStatus())
        || !"confirmed".equalsIgnoreCase(simulation.getConfirmationStatus())
        || !"confirmed".equalsIgnoreCase(practical.getConfirmationStatus())) return null;
    if (!Objects.equals(theory.getScoreRuleId(), source.getScoreRuleId())
        || !Objects.equals(simulation.getScoreRuleId(), source.getScoreRuleId())
        || !Objects.equals(practical.getScoreRuleId(), source.getScoreRuleId())) return null;

    var calculation = hasIndependentWeights(rule)
        ? calculator.calculateLevelScoped(
            input(theory), input(simulation), input(practical), rule.getTheoryWeight(),
            rule.getSimulationWeight(), rule.getPracticalWeight())
        : calculator.calculate(
            input(theory), input(simulation), input(practical),
            rule.getTheoryWeight(), rule.getSimulationPracticalWeight());
    if (!calculation.confirmed()) return null;
    String fingerprint = sha256(theory.getId() + ":" + simulation.getId() + ":"
        + practical.getId() + ":" + rule.getId());
    return persistTotal(source, rule, theory, simulation, practical, calculation, fingerprint,
        operatorId, now);
  }

  private CompetitionTotalScoreSnapshotEntity createContentWeightedTotal(
      CompetitionScoreSourceEntity source,
      Map<String, CompetitionScoreSourceEntity> byType,
      CompetitionStageContentScoringWeights weights,
      Long operatorId,
      LocalDateTime now) {
    CompetitionScoreSourceEntity theory = requiredOrZeroInput(byType.get("theory"), weights, "theory");
    CompetitionScoreSourceEntity simulation =
        requiredOrZeroInput(byType.get("simulation"), weights, "simulation");
    CompetitionScoreSourceEntity practical =
        requiredOrZeroInput(byType.get("practical"), weights, "practical");
    if (theory == null || simulation == null || practical == null) return null;
    var calculation = calculator.calculateLevelScoped(
        input(theory), input(simulation), input(practical),
        weights.theoryWeight(), weights.simulationWeight(), weights.practicalWeight());
    if (!calculation.confirmed()) return null;
    String fingerprint = sha256("content-weights:"
        + idOrNone(theory) + ":" + idOrNone(simulation) + ":" + idOrNone(practical) + ":"
        + weights.theoryWeight().toPlainString() + ":"
        + weights.simulationWeight().toPlainString() + ":"
        + weights.practicalWeight().toPlainString());
    CompetitionTotalScoreSnapshotEntity existing = totalMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity>()
            .eq(CompetitionTotalScoreSnapshotEntity::getInputFingerprint, fingerprint)
            .eq(CompetitionTotalScoreSnapshotEntity::getDeleted, 0).last("LIMIT 1"));
    if (existing != null) {
      tryGenerateRankingDraft(source, null, operatorId, now);
      return existing;
    }
    return persistContentWeightedTotal(
        source, theory, simulation, practical, weights, calculation, fingerprint, operatorId, now);
  }

  /**
   * Theory-only stages do not have a cross-subject scoring rule.  The formal exam scorer already
   * computed the score from the immutable question snapshot, so the theory source itself is the
   * complete total-score input (and is still persisted as an immutable total snapshot).
   */
  private CompetitionTotalScoreSnapshotEntity createDirectTheoryTotal(
      CompetitionScoreSourceEntity source,
      CompetitionScoreSourceEntity theory,
      Long operatorId,
      LocalDateTime now) {
    if (theory == null || theory.getRawScore() == null
        || !Objects.equals(theory.getSourceType(), "theory")
        || !Objects.equals(theory.getScoreRuleId(), source.getScoreRuleId())) {
      return null;
    }
    String fingerprint = sha256(theory.getId() + ":direct-theory");
    CompetitionTotalScoreSnapshotEntity existing = totalMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity>()
            .eq(CompetitionTotalScoreSnapshotEntity::getInputFingerprint, fingerprint)
            .eq(CompetitionTotalScoreSnapshotEntity::getDeleted, 0).last("LIMIT 1"));
    if (existing != null) {
      tryGenerateRankingDraft(source, null, operatorId, now);
      return existing;
    }
    CompetitionTotalScoreSnapshotEntity current = totalMapper.selectOne(totalScope(source)
        .eq(CompetitionTotalScoreSnapshotEntity::getCurrentFlag, 1).last("LIMIT 1"));
    int version = current == null ? 1 : current.getCalculationVersion() + 1;
    if (current != null) {
      current.setCurrentFlag(0);
      current.setUpdatedBy(operatorId);
      current.setUpdatedAt(now);
      totalMapper.updateById(current);
    }
    CompetitionTotalScoreSnapshotEntity total = new CompetitionTotalScoreSnapshotEntity();
    total.setId(IdWorker.getId());
    total.setTenantId(source.getTenantId());
    total.setEventId(source.getEventId());
    total.setStageId(source.getStageId());
    total.setEventWorkTypeId(source.getEventWorkTypeId());
    total.setEventWorkTypeLevelId(source.getEventWorkTypeLevelId());
    total.setContestantMemberId(source.getContestantMemberId());
    total.setScoreRuleId(null);
    total.setTheorySourceId(theory.getId());
    total.setSimulationSourceId(null);
    total.setPracticalSourceId(null);
    total.setInputFingerprint(fingerprint);
    total.setTheoryScore(theory.getRawScore());
    total.setSimulationScore(BigDecimal.ZERO);
    total.setPracticalScore(BigDecimal.ZERO);
    total.setSimulationPracticalScore(BigDecimal.ZERO);
    total.setTheoryWeight(new BigDecimal("100.00"));
    total.setSimulationWeight(BigDecimal.ZERO);
    total.setPracticalWeight(BigDecimal.ZERO);
    total.setSimulationPracticalWeight(BigDecimal.ZERO);
    total.setTotalScore(theory.getRawScore());
    total.setCalculationVersion(version);
    total.setConfirmationStatus("confirmed");
    total.setCurrentFlag(1);
    total.setCalculatedAt(now);
    total.setCreatedBy(operatorId);
    total.setUpdatedBy(operatorId);
    total.setCreatedAt(now);
    total.setUpdatedAt(now);
    total.setDeleted(0);
    totalMapper.insert(total);
    tryGenerateRankingDraft(source, null, operatorId, now);
    return total;
  }

  private CompetitionTotalScoreSnapshotEntity persistTotal(
      CompetitionScoreSourceEntity source,
      CompetitionScoreRuleEntity rule,
      CompetitionScoreSourceEntity theory,
      CompetitionScoreSourceEntity simulation,
      CompetitionScoreSourceEntity practical,
      CompetitionScoringSystemBCalculator.SystemBCalculationResult calculation,
      String fingerprint,
      Long operatorId,
      LocalDateTime now) {
    CompetitionTotalScoreSnapshotEntity existing = totalMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity>()
            .eq(CompetitionTotalScoreSnapshotEntity::getInputFingerprint, fingerprint)
            .eq(CompetitionTotalScoreSnapshotEntity::getDeleted, 0).last("LIMIT 1"));
    if (existing != null) {
      tryGenerateRankingDraft(source, rule, operatorId, now);
      return existing;
    }
    CompetitionTotalScoreSnapshotEntity current = totalMapper.selectOne(totalScope(source)
        .eq(CompetitionTotalScoreSnapshotEntity::getCurrentFlag, 1).last("LIMIT 1"));
    int version = current == null ? 1 : current.getCalculationVersion() + 1;
    if (current != null) {
      current.setCurrentFlag(0);
      current.setUpdatedBy(operatorId);
      totalMapper.updateById(current);
    }
    CompetitionTotalScoreSnapshotEntity total = new CompetitionTotalScoreSnapshotEntity();
    total.setId(IdWorker.getId()); total.setTenantId(source.getTenantId());
    total.setEventId(source.getEventId()); total.setStageId(source.getStageId());
    total.setEventWorkTypeId(source.getEventWorkTypeId());
    total.setEventWorkTypeLevelId(source.getEventWorkTypeLevelId());
    total.setContestantMemberId(source.getContestantMemberId());
    total.setScoreRuleId(rule.getId()); total.setTheorySourceId(theory.getId());
    total.setSimulationSourceId(simulation.getId()); total.setPracticalSourceId(practical.getId());
    total.setInputFingerprint(fingerprint); total.setTheoryScore(theory.getRawScore());
    total.setSimulationScore(simulation.getRawScore()); total.setPracticalScore(practical.getRawScore());
    total.setSimulationPracticalScore(calculation.simulationPracticalScore());
    total.setTheoryWeight(rule.getTheoryWeight());
    total.setSimulationPracticalWeight(combinedWeight(rule));
    total.setSimulationWeight(rule.getSimulationWeight()); total.setPracticalWeight(rule.getPracticalWeight());
    total.setTotalScore(calculation.totalScore()); total.setCalculationVersion(version);
    total.setConfirmationStatus("confirmed"); total.setCurrentFlag(1); total.setCalculatedAt(now);
    total.setCreatedBy(operatorId); total.setUpdatedBy(operatorId);
    total.setCreatedAt(now); total.setUpdatedAt(now); total.setDeleted(0);
    totalMapper.insert(total);
    tryGenerateRankingDraft(source, rule, operatorId, now);
    return total;
  }

  private CompetitionTotalScoreSnapshotEntity persistContentWeightedTotal(
      CompetitionScoreSourceEntity source,
      CompetitionScoreSourceEntity theory,
      CompetitionScoreSourceEntity simulation,
      CompetitionScoreSourceEntity practical,
      CompetitionStageContentScoringWeights weights,
      CompetitionScoringSystemBCalculator.SystemBCalculationResult calculation,
      String fingerprint,
      Long operatorId,
      LocalDateTime now) {
    CompetitionTotalScoreSnapshotEntity current = totalMapper.selectOne(totalScope(source)
        .eq(CompetitionTotalScoreSnapshotEntity::getCurrentFlag, 1).last("LIMIT 1"));
    int version = current == null ? 1 : current.getCalculationVersion() + 1;
    if (current != null) {
      current.setCurrentFlag(0);
      current.setUpdatedBy(operatorId);
      current.setUpdatedAt(now);
      totalMapper.updateById(current);
    }
    CompetitionTotalScoreSnapshotEntity total = new CompetitionTotalScoreSnapshotEntity();
    total.setId(IdWorker.getId());
    total.setTenantId(source.getTenantId());
    total.setEventId(source.getEventId());
    total.setStageId(source.getStageId());
    total.setEventWorkTypeId(source.getEventWorkTypeId());
    total.setEventWorkTypeLevelId(source.getEventWorkTypeLevelId());
    total.setContestantMemberId(source.getContestantMemberId());
    total.setScoreRuleId(null);
    total.setTheorySourceId(sourceId(theory));
    total.setSimulationSourceId(sourceId(simulation));
    total.setPracticalSourceId(sourceId(practical));
    total.setInputFingerprint(fingerprint);
    total.setTheoryScore(theory.getRawScore());
    total.setSimulationScore(simulation.getRawScore());
    total.setPracticalScore(practical.getRawScore());
    total.setSimulationPracticalScore(calculation.simulationPracticalScore());
    total.setTheoryWeight(weights.theoryWeight());
    total.setSimulationWeight(weights.simulationWeight());
    total.setPracticalWeight(weights.practicalWeight());
    total.setSimulationPracticalWeight(weights.combinedSimulationPracticalWeight());
    total.setTotalScore(calculation.totalScore());
    total.setCalculationVersion(version);
    total.setConfirmationStatus("confirmed");
    total.setCurrentFlag(1);
    total.setCalculatedAt(now);
    total.setCreatedBy(operatorId);
    total.setUpdatedBy(operatorId);
    total.setCreatedAt(now);
    total.setUpdatedAt(now);
    total.setDeleted(0);
    totalMapper.insert(total);
    tryGenerateRankingDraft(source, null, operatorId, now);
    return total;
  }

  private CompetitionScoreSourceEntity requiredOrZeroInput(
      CompetitionScoreSourceEntity existing,
      CompetitionStageContentScoringWeights weights,
      String sourceType) {
    if (weights.enabled(sourceType)) {
      if (existing == null
          || existing.getRawScore() == null
          || !"confirmed".equalsIgnoreCase(existing.getConfirmationStatus())) {
        return null;
      }
      return existing;
    }
    CompetitionScoreSourceEntity zero = new CompetitionScoreSourceEntity();
    zero.setRawScore(BigDecimal.ZERO);
    zero.setConfirmationStatus("confirmed");
    zero.setSourceType(sourceType);
    return zero;
  }

  private String idOrNone(CompetitionScoreSourceEntity source) {
    return source == null || source.getId() == null ? "none" : String.valueOf(source.getId());
  }

  private Long sourceId(CompetitionScoreSourceEntity source) {
    return source == null ? null : source.getId();
  }

  private boolean isDirectTheorySource(CompetitionScoreSourceEntity source) {
    return source.getScoreRuleId() == null
        && "theory".equalsIgnoreCase(source.getSourceType())
        && isTheoryOnlyStage(source.getStageId());
  }

  private CompetitionScoreSourceEntity sourceOrDisabledZero(
      CompetitionScoreSourceEntity scope,
      CompetitionScoreSourceEntity existing,
      String sourceType,
      CompetitionScoreRuleEntity rule,
      Long operatorId,
      LocalDateTime now) {
    if (existing != null) return existing;
    if (!isZeroWeighted(rule, sourceType) || isContentEnabled(scope.getStageId(), sourceType)) {
      return null;
    }
    List<CompetitionScoreSourceEntity> history = sourceMapper.selectList(
        new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
            .eq(CompetitionScoreSourceEntity::getEventId, scope.getEventId())
            .eq(CompetitionScoreSourceEntity::getStageId, scope.getStageId())
            .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, scope.getEventWorkTypeId())
            .eq(scope.getEventWorkTypeLevelId() != null,
                CompetitionScoreSourceEntity::getEventWorkTypeLevelId,
                scope.getEventWorkTypeLevelId())
            .isNull(scope.getEventWorkTypeLevelId() == null,
                CompetitionScoreSourceEntity::getEventWorkTypeLevelId)
            .eq(CompetitionScoreSourceEntity::getContestantMemberId,
                scope.getContestantMemberId())
            .eq(CompetitionScoreSourceEntity::getSourceType, sourceType)
            .eq(CompetitionScoreSourceEntity::getDeleted, 0));
    int revisionNo = history == null ? 1 : history.stream()
        .map(CompetitionScoreSourceEntity::getRevisionNo)
        .filter(Objects::nonNull)
        .max(Integer::compareTo).orElse(0) + 1;
    CompetitionScoreSourceEntity zero = new CompetitionScoreSourceEntity();
    zero.setId(IdWorker.getId());
    zero.setTenantId(scope.getTenantId());
    zero.setEventId(scope.getEventId());
    zero.setStageId(scope.getStageId());
    zero.setEventWorkTypeId(scope.getEventWorkTypeId());
    zero.setEventWorkTypeLevelId(scope.getEventWorkTypeLevelId());
    zero.setContestantMemberId(scope.getContestantMemberId());
    zero.setSourceType(sourceType);
    zero.setRawScore(BigDecimal.ZERO);
    zero.setConfirmationStatus("confirmed");
    zero.setSourceSystem("competition-disabled-content");
    zero.setSourceReference("disabled-content:" + sourceType + ":rule:" + rule.getId());
    zero.setScoreRuleId(rule.getId());
    zero.setRevisionNo(revisionNo);
    zero.setCurrentFlag(1);
    zero.setAchievedAt(scope.getAchievedAt() == null ? now : scope.getAchievedAt());
    zero.setConfirmedAt(now);
    zero.setConfirmedBy(operatorId == null ? 0L : operatorId);
    zero.setCreatedBy(operatorId);
    zero.setUpdatedBy(operatorId);
    zero.setCreatedAt(now);
    zero.setUpdatedAt(now);
    zero.setDeleted(0);
    sourceMapper.insert(zero);
    return zero;
  }

  private boolean isContentEnabled(Long stageId, String sourceType) {
    CompetitionStageContentEntity content = stageContentMapper.selectOne(
        new LambdaQueryWrapper<CompetitionStageContentEntity>()
            .eq(CompetitionStageContentEntity::getStageId, stageId)
            .eq(CompetitionStageContentEntity::getContentType, sourceType)
            .eq(CompetitionStageContentEntity::getStatus, "enabled")
            .eq(CompetitionStageContentEntity::getDeleted, 0)
            .last("LIMIT 1"));
    return content != null;
  }

  private boolean isTheoryOnlyStage(Long stageId) {
    return stageContentWeights(stageId).theoryOnly();
  }

  private CompetitionStageContentScoringWeights stageContentWeights(Long stageId) {
    if (stageContentMapper == null || stageId == null) {
      return CompetitionStageContentScoringWeights.invalid();
    }
    List<CompetitionStageContentEntity> contents = stageContentMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageContentEntity>()
            .eq(CompetitionStageContentEntity::getStageId, stageId)
            .eq(CompetitionStageContentEntity::getDeleted, 0));
    return CompetitionStageContentScoringWeights.from(contents);
  }

  private boolean allCandidatesClosed(List<CompetitionFormalCandidateExamEntity> candidates) {
    return candidates != null && !candidates.isEmpty()
        && candidates.stream().allMatch(candidate -> candidate != null
            && CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus()) != null
            && CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus()).isClosed());
  }

  private boolean isZeroWeighted(CompetitionScoreRuleEntity rule, String sourceType) {
    if (hasIndependentWeights(rule)) {
      BigDecimal weight = "simulation".equals(sourceType)
          ? rule.getSimulationWeight() : rule.getPracticalWeight();
      return weight != null && weight.compareTo(BigDecimal.ZERO) == 0;
    }
    return rule.getSimulationPracticalWeight() != null
        && rule.getSimulationPracticalWeight().compareTo(BigDecimal.ZERO) == 0;
  }

  private boolean hasIndependentWeights(CompetitionScoreRuleEntity rule) {
    return rule.getSimulationWeight() != null || rule.getPracticalWeight() != null;
  }

  private BigDecimal combinedWeight(CompetitionScoreRuleEntity rule) {
    if (rule.getSimulationPracticalWeight() != null) {
      return rule.getSimulationPracticalWeight();
    }
    return rule.getSimulationWeight().add(rule.getPracticalWeight());
  }

  /** Rebuilds the roster/status/total input fingerprint used by an automatic draft. */
  @Transactional(readOnly = true)
  public boolean isRankingFingerprintCurrent(CompetitionRankingSnapshotEntity snapshot) {
    if (snapshot == null || snapshot.getInputFingerprint() == null) return false;
    CompetitionScoreSourceEntity scope = new CompetitionScoreSourceEntity();
    scope.setEventId(snapshot.getEventId());
    scope.setStageId(snapshot.getStageId());
    scope.setEventWorkTypeId(snapshot.getEventWorkTypeId());
    scope.setEventWorkTypeLevelId(snapshot.getEventWorkTypeLevelId());
    scope.setScoreRuleId(snapshot.getScoreRuleId());
    List<RankRow> rows = loadRankingRows(scope);
    if (rows == null || rows.isEmpty()) return false;
    rows.sort(rankingComparator());
    String currentFingerprint = sha256(rows.stream().map(RankRow::fingerprint)
        .collect(Collectors.joining("|")) + ":rule:" + snapshot.getScoreRuleId());
    return currentFingerprint.equals(snapshot.getInputFingerprint());
  }

  private void tryGenerateRankingDraft(CompetitionScoreSourceEntity source,
      CompetitionScoreRuleEntity rule, Long operatorId, LocalDateTime now) {
    List<RankRow> rows = loadRankingRows(source);
    if (rows == null || rows.isEmpty()) return;
    rows.sort(rankingComparator());
    String fingerprint = sha256(rows.stream().map(RankRow::fingerprint)
        .collect(Collectors.joining("|")) + ":rule:" + (rule == null ? null : rule.getId()));
    CompetitionRankingSnapshotEntity existing = rankingMapper.selectOne(
        new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
            .eq(CompetitionRankingSnapshotEntity::getInputFingerprint, fingerprint)
            .eq(CompetitionRankingSnapshotEntity::getDeleted, 0).last("LIMIT 1"));
    if (existing != null) return;
    CompetitionRankingSnapshotEntity latest = rankingMapper.selectOne(rankingScope(source)
        .orderByDesc(CompetitionRankingSnapshotEntity::getSnapshotVersion).last("LIMIT 1"));
    CompetitionRankingSnapshotEntity snapshot = new CompetitionRankingSnapshotEntity();
    snapshot.setId(IdWorker.getId()); snapshot.setTenantId(source.getTenantId());
    snapshot.setEventId(source.getEventId()); snapshot.setStageId(source.getStageId());
    snapshot.setEventWorkTypeId(source.getEventWorkTypeId());
    snapshot.setEventWorkTypeLevelId(source.getEventWorkTypeLevelId());
    snapshot.setScoreRuleId(rule == null ? null : rule.getId());
    snapshot.setSnapshotVersion(latest == null ? 1 : latest.getSnapshotVersion() + 1);
    snapshot.setInputFingerprint(fingerprint); snapshot.setStatus("draft");
    snapshot.setGenerationReason(rule == null ? "赛段内容权重计分后自动生成" : "三类成绩齐备后自动生成");
    snapshot.setGeneratedAt(now);
    snapshot.setCreatedBy(operatorId); snapshot.setUpdatedBy(operatorId);
    snapshot.setCreatedAt(now); snapshot.setUpdatedAt(now); snapshot.setDeleted(0);
    rankingMapper.insert(snapshot);
    List<Integer> displayRanks = new ArrayList<>();
    int rankedPosition = 0;
    for (int index = 0; index < rows.size();) {
      RankRow row = rows.get(index);
      if (row.absent()) {
        displayRanks.add(null);
        index++;
        continue;
      }
      int end = index + 1;
      while (end < rows.size() && !rows.get(end).absent()
          && row.tieKey().equals(rows.get(end).tieKey())) end++;
      int groupSize = end - index;
      int groupRank = rankedPosition + groupSize;
      for (int i = index; i < end; i++) displayRanks.add(groupRank);
      rankedPosition += groupSize;
      index = end;
    }
    int sortOrder = 0;
    List<CompetitionRankingEntryEntity> entries = new ArrayList<>();
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      RankRow row = rows.get(rowIndex);
      sortOrder++;
      String tie = row.absent() ? null : row.tieKey();
      CompetitionRankingEntryEntity entry = new CompetitionRankingEntryEntity();
      entry.setId(IdWorker.getId()); entry.setTenantId(source.getTenantId());
      entry.setRankingSnapshotId(snapshot.getId());
      entry.setContestantMemberId(row.candidate().getContestantMemberId());
      entry.setTotalScoreSnapshotId(row.totalSnapshot() == null ? null : row.totalSnapshot().getId());
      entry.setTotalScore(row.absent() ? BigDecimal.ZERO : row.total());
      entry.setDisplayRank(displayRanks.get(rowIndex)); entry.setTieKey(tie);
      entry.setContestantStatus(row.absent() ? "absent" : "completed");
      entry.setSortOrder(sortOrder); entry.setCreatedBy(operatorId); entry.setUpdatedBy(operatorId);
      entry.setCreatedAt(now); entry.setUpdatedAt(now); entry.setDeleted(0);
      entries.add(entry);
    }
    Map<String, Long> tieCounts = entries.stream().filter(e -> e.getTieKey() != null)
        .collect(Collectors.groupingBy(CompetitionRankingEntryEntity::getTieKey, Collectors.counting()));
    for (CompetitionRankingEntryEntity entry : entries) {
      entry.setTiedFlag(entry.getTieKey() != null && tieCounts.get(entry.getTieKey()) > 1 ? 1 : 0);
      entryMapper.insert(entry);
    }
  }

  private List<RankRow> loadRankingRows(CompetitionScoreSourceEntity source) {
    CompetitionFormalExamSessionEntity session = sessionMapper.selectOne(
        new LambdaQueryWrapper<CompetitionFormalExamSessionEntity>()
            .eq(CompetitionFormalExamSessionEntity::getEventId, source.getEventId())
            .eq(CompetitionFormalExamSessionEntity::getStageId, source.getStageId())
            .eq(CompetitionFormalExamSessionEntity::getEventWorkTypeId, source.getEventWorkTypeId())
            .eq(source.getEventWorkTypeLevelId() != null,
                CompetitionFormalExamSessionEntity::getEventWorkTypeLevelId,
                source.getEventWorkTypeLevelId())
            .isNull(source.getEventWorkTypeLevelId() == null,
                CompetitionFormalExamSessionEntity::getEventWorkTypeLevelId)
            .eq(CompetitionFormalExamSessionEntity::getDeleted, 0)
            .orderByDesc(CompetitionFormalExamSessionEntity::getId).last("LIMIT 1"));
    if (session == null) {
      return rankingRowsFromTotals(source);
    }
    List<CompetitionFormalCandidateExamEntity> candidates =
        candidateMapper.selectByExamSessionId(session.getId());
    if (!allCandidatesClosed(candidates)) return null;
    List<RankRow> rows = new ArrayList<>();
    for (CompetitionFormalCandidateExamEntity candidate : candidates) {
      CompetitionFormalCandidateExamStatus status =
          CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus());
      if (status == CompetitionFormalCandidateExamStatus.ABSENT) {
        rows.add(RankRow.absent(candidate));
        continue;
      }
      CompetitionTotalScoreSnapshotEntity total = totalMapper.selectOne(
          totalScopeWithoutContestant(source)
              .eq(CompetitionTotalScoreSnapshotEntity::getContestantMemberId,
                  candidate.getContestantMemberId())
              .eq(CompetitionTotalScoreSnapshotEntity::getCurrentFlag, 1)
              .eq(CompetitionTotalScoreSnapshotEntity::getConfirmationStatus, "confirmed")
              .last("LIMIT 1"));
      if (total == null) return null;
      rows.add(RankRow.completed(candidate, total));
    }
    return rows;
  }

  private List<RankRow> rankingRowsFromTotals(CompetitionScoreSourceEntity source) {
    List<CompetitionTotalScoreSnapshotEntity> totals = totalMapper.selectList(
        totalScopeWithoutContestant(source)
            .eq(CompetitionTotalScoreSnapshotEntity::getCurrentFlag, 1)
            .eq(CompetitionTotalScoreSnapshotEntity::getConfirmationStatus, "confirmed"));
    if (totals == null || totals.isEmpty()) return List.of();
    List<RankRow> rows = new ArrayList<>();
    for (CompetitionTotalScoreSnapshotEntity total : totals) {
      if (total == null || total.getContestantMemberId() == null) continue;
      rows.add(RankRow.completed(syntheticCandidate(total.getContestantMemberId()), total));
    }
    return rows;
  }

  private CompetitionFormalCandidateExamEntity syntheticCandidate(Long contestantMemberId) {
    CompetitionFormalCandidateExamEntity candidate = new CompetitionFormalCandidateExamEntity();
    candidate.setId(contestantMemberId);
    candidate.setContestantMemberId(contestantMemberId);
    candidate.setStatus(CompetitionFormalCandidateExamStatus.SUBMITTED.getCode());
    return candidate;
  }

  private LambdaQueryWrapper<CompetitionScoreSourceEntity> scopeSources(CompetitionScoreSourceEntity s) {
    return new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
        .eq(CompetitionScoreSourceEntity::getEventId, s.getEventId())
        .eq(CompetitionScoreSourceEntity::getStageId, s.getStageId())
        .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, s.getEventWorkTypeId())
        .eq(s.getEventWorkTypeLevelId() != null, CompetitionScoreSourceEntity::getEventWorkTypeLevelId,
            s.getEventWorkTypeLevelId())
        .isNull(s.getEventWorkTypeLevelId() == null, CompetitionScoreSourceEntity::getEventWorkTypeLevelId)
        .eq(CompetitionScoreSourceEntity::getContestantMemberId, s.getContestantMemberId())
        .eq(s.getScoreRuleId() != null, CompetitionScoreSourceEntity::getScoreRuleId,
            s.getScoreRuleId())
        .isNull(s.getScoreRuleId() == null, CompetitionScoreSourceEntity::getScoreRuleId)
        .eq(CompetitionScoreSourceEntity::getCurrentFlag, 1)
        .eq(CompetitionScoreSourceEntity::getConfirmationStatus, "confirmed")
        .eq(CompetitionScoreSourceEntity::getDeleted, 0);
  }

  private LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity> totalScope(CompetitionScoreSourceEntity s) {
    return totalScopeWithoutContestant(s)
        .eq(CompetitionTotalScoreSnapshotEntity::getContestantMemberId, s.getContestantMemberId());
  }

  private LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity> totalScopeWithoutContestant(
      CompetitionScoreSourceEntity s) {
    return new LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity>()
        .eq(CompetitionTotalScoreSnapshotEntity::getEventId, s.getEventId())
        .eq(CompetitionTotalScoreSnapshotEntity::getStageId, s.getStageId())
        .eq(CompetitionTotalScoreSnapshotEntity::getEventWorkTypeId, s.getEventWorkTypeId())
        .eq(s.getEventWorkTypeLevelId() != null,
            CompetitionTotalScoreSnapshotEntity::getEventWorkTypeLevelId, s.getEventWorkTypeLevelId())
        .isNull(s.getEventWorkTypeLevelId() == null,
            CompetitionTotalScoreSnapshotEntity::getEventWorkTypeLevelId)
        .eq(s.getScoreRuleId() != null, CompetitionTotalScoreSnapshotEntity::getScoreRuleId,
            s.getScoreRuleId())
        .isNull(s.getScoreRuleId() == null, CompetitionTotalScoreSnapshotEntity::getScoreRuleId)
        .eq(CompetitionTotalScoreSnapshotEntity::getDeleted, 0);
  }

  private LambdaQueryWrapper<CompetitionRankingSnapshotEntity> rankingScope(CompetitionScoreSourceEntity s) {
    return new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
        .eq(CompetitionRankingSnapshotEntity::getEventId, s.getEventId())
        .eq(CompetitionRankingSnapshotEntity::getStageId, s.getStageId())
        .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeId, s.getEventWorkTypeId())
        .eq(s.getEventWorkTypeLevelId() != null,
            CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId, s.getEventWorkTypeLevelId())
        .isNull(s.getEventWorkTypeLevelId() == null,
            CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId)
        .eq(s.getScoreRuleId() != null, CompetitionRankingSnapshotEntity::getScoreRuleId,
            s.getScoreRuleId())
        .isNull(s.getScoreRuleId() == null, CompetitionRankingSnapshotEntity::getScoreRuleId)
        .eq(CompetitionRankingSnapshotEntity::getDeleted, 0);
  }

  private CompetitionScoringSystemBCalculator.ScoreInput input(CompetitionScoreSourceEntity s) {
    return new CompetitionScoringSystemBCalculator.ScoreInput(s.getRawScore(), true);
  }

  private Comparator<RankRow> rankingComparator() {
    return Comparator.comparing((RankRow row) -> row.absent())
        .thenComparing(RankRow::total, Comparator.reverseOrder())
        .thenComparing(RankRow::practical, Comparator.reverseOrder())
        .thenComparing(RankRow::simulation, Comparator.reverseOrder())
        .thenComparing(RankRow::theory, Comparator.reverseOrder())
        .thenComparing(row -> row.candidate().getId());
  }

  private String sha256(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }

  private record RankRow(CompetitionFormalCandidateExamEntity candidate,
                         CompetitionTotalScoreSnapshotEntity totalSnapshot,
                         BigDecimal total, BigDecimal practical,
                         BigDecimal simulation, BigDecimal theory, boolean absent) {
    static RankRow absent(CompetitionFormalCandidateExamEntity candidate) {
      return new RankRow(candidate, null, BigDecimal.ZERO, BigDecimal.ZERO,
          BigDecimal.ZERO, BigDecimal.ZERO, true);
    }
    static RankRow completed(CompetitionFormalCandidateExamEntity candidate,
        CompetitionTotalScoreSnapshotEntity total) {
      return new RankRow(candidate, total, total.getTotalScore(), total.getPracticalScore(),
          total.getSimulationScore(), total.getTheoryScore(), false);
    }
    String tieKey() { return total.toPlainString() + "|" + practical.toPlainString() + "|"
        + simulation.toPlainString() + "|" + theory.toPlainString(); }
    String fingerprint() { return candidate.getId() + ":" + (absent ? "absent" : totalSnapshot.getId()); }
  }
}
