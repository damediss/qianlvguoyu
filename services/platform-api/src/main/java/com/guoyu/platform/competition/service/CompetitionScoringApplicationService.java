package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionScoringConverter;
import com.guoyu.platform.competition.domain.CompetitionRankingAllocator;
import com.guoyu.platform.competition.domain.CompetitionScoreRuleReferenceValidator;
import com.guoyu.platform.competition.domain.CompetitionScoringSystemBCalculator;
import com.guoyu.platform.competition.dto.CompetitionRankingSnapshotGenerateReq;
import com.guoyu.platform.competition.dto.CompetitionScoreSourceRevisionReq;
import com.guoyu.platform.competition.dto.CompetitionTotalScoreCalculateReq;
import com.guoyu.platform.competition.entity.CompetitionRankingEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionTotalScoreSnapshotEntity;
import com.guoyu.platform.competition.enums.CompetitionRankingContestantStatus;
import com.guoyu.platform.competition.enums.CompetitionScoreConfirmationStatus;
import com.guoyu.platform.competition.enums.CompetitionScoreSourceType;
import com.guoyu.platform.competition.mapper.CompetitionRankingEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionTotalScoreSnapshotMapper;
import com.guoyu.platform.competition.messaging.service.CompetitionScorePipelineOutboxService;
import com.guoyu.platform.competition.vo.CompetitionRankingEntryVO;
import com.guoyu.platform.competition.vo.CompetitionRankingSnapshotDetailVO;
import com.guoyu.platform.competition.vo.CompetitionRankingSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionScoreSourceVO;
import com.guoyu.platform.competition.vo.CompetitionTotalScoreSnapshotVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionScoringApplicationService {

  private static final String CURRENT_CONFIRMED =
      CompetitionScoreConfirmationStatus.CONFIRMED.getCode();
  private static final String SNAPSHOT_DRAFT = "draft";

  private final CompetitionScoreSourceMapper scoreSourceMapper;
  private final CompetitionTotalScoreSnapshotMapper totalScoreSnapshotMapper;
  private final CompetitionRankingSnapshotMapper rankingSnapshotMapper;
  private final CompetitionRankingEntryMapper rankingEntryMapper;
  private final CompetitionContestantScopeValidator contestantScopeValidator;
  private final CompetitionScoreRuleDomainService scoreRuleDomainService;
  private final CompetitionScoreRuleReferenceValidator scoreRuleReferenceValidator;
  private final CompetitionScoreRuleApplicationService scoreRuleApplicationService;
  private final CompetitionScoringSystemBCalculator systemBCalculator =
      new CompetitionScoringSystemBCalculator();
  private final CompetitionRankingAllocator rankingAllocator =
      new CompetitionRankingAllocator();
  private CompetitionScorePipelineOutboxService scorePipelineOutboxService;
  private CompetitionAutomaticScorePipelineService automaticScorePipelineService;

  public CompetitionScoringApplicationService(
      CompetitionScoreSourceMapper scoreSourceMapper,
      CompetitionTotalScoreSnapshotMapper totalScoreSnapshotMapper,
      CompetitionRankingSnapshotMapper rankingSnapshotMapper,
      CompetitionRankingEntryMapper rankingEntryMapper,
      CompetitionContestantScopeValidator contestantScopeValidator,
      CompetitionScoreRuleDomainService scoreRuleDomainService,
      CompetitionScoreRuleReferenceValidator scoreRuleReferenceValidator,
      CompetitionScoreRuleApplicationService scoreRuleApplicationService) {
    this.scoreSourceMapper = scoreSourceMapper;
    this.totalScoreSnapshotMapper = totalScoreSnapshotMapper;
    this.rankingSnapshotMapper = rankingSnapshotMapper;
    this.rankingEntryMapper = rankingEntryMapper;
    this.contestantScopeValidator = contestantScopeValidator;
    this.scoreRuleDomainService = scoreRuleDomainService;
    this.scoreRuleReferenceValidator = scoreRuleReferenceValidator;
    this.scoreRuleApplicationService = scoreRuleApplicationService;
  }

  @Autowired(required = false)
  public void setScorePipelineOutboxService(
      CompetitionScorePipelineOutboxService scorePipelineOutboxService) {
    this.scorePipelineOutboxService = scorePipelineOutboxService;
  }

  @Autowired(required = false)
  public void setAutomaticScorePipelineService(
      CompetitionAutomaticScorePipelineService automaticScorePipelineService) {
    this.automaticScorePipelineService = automaticScorePipelineService;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionScoreSourceVO> listScoreSources(
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long contestantMemberId,
      String sourceType,
      PageQuery pageQuery) {
    return listScoreSources(eventId, stageId, eventWorkTypeId, null, contestantMemberId,
        sourceType, pageQuery);
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionScoreSourceVO> listScoreSources(
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      Long contestantMemberId,
      String sourceType,
      PageQuery pageQuery) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    LambdaQueryWrapper<CompetitionScoreSourceEntity> wrapper =
        new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
            .eq(CompetitionScoreSourceEntity::getEventId, eventId)
            .eq(CompetitionScoreSourceEntity::getStageId, stageId)
            .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(eventWorkTypeLevelId != null,
                CompetitionScoreSourceEntity::getEventWorkTypeLevelId, eventWorkTypeLevelId)
            .eq(contestantMemberId != null,
                CompetitionScoreSourceEntity::getContestantMemberId, contestantMemberId)
            .eq(sourceType != null && !sourceType.isBlank(),
                CompetitionScoreSourceEntity::getSourceType, sourceType)
            .orderByAsc(CompetitionScoreSourceEntity::getContestantMemberId)
            .orderByAsc(CompetitionScoreSourceEntity::getSourceType)
            .orderByDesc(CompetitionScoreSourceEntity::getRevisionNo);
    IPage<CompetitionScoreSourceEntity> page = scoreSourceMapper.selectPage(
        new Page<>(pageQuery.getPage(), pageQuery.getPageSize()), wrapper);
    if (page == null || page.getRecords() == null) {
      log.warn("Score source mapper returned null page, tenantId={}, eventId={}",
          principal.getTenantId(), eventId);
      return PageResponse.empty(pageQuery.getPage(), pageQuery.getPageSize());
    }
    return new PageResponse<>(
        page.getRecords().stream().map(CompetitionScoringConverter::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CompetitionScoreSourceVO reviseScoreSource(
      Long eventId, CompetitionScoreSourceRevisionReq request) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    validateScoreSourceType(request.sourceType());
    validateConfirmationStatus(request.confirmationStatus());
    validateScoringScope(
        principal.getTenantId(), eventId, request.stageId(),
        request.eventWorkTypeId(), request.eventWorkTypeLevelId(),
        request.contestantMemberId(), request.scoreRuleId());

    List<CompetitionScoreSourceEntity> revisions = safeList(scoreSourceMapper.selectList(
        sourceRevisionScope(eventId, request)
            .orderByDesc(CompetitionScoreSourceEntity::getRevisionNo)
            .last("FOR UPDATE")),
        "score source revisions", principal, eventId);
    CompetitionScoreSourceEntity current = revisions.stream()
        .filter(item -> Integer.valueOf(1).equals(item.getCurrentFlag()))
        .findFirst()
        .orElse(null);
    if (current != null && ("competition-formal-exam".equals(current.getSourceSystem())
        || ("practical".equals(current.getSourceType())
            && "competition".equals(current.getSourceSystem())))) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "系统生成的理论或实操成绩不能通过人工通用接口覆盖");
    }
    int revisionNo = revisions.stream()
        .map(CompetitionScoreSourceEntity::getRevisionNo)
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(0) + 1;
    if (current != null) {
      current.setCurrentFlag(0);
      current.setUpdatedBy(principal.getUserId());
      ensureSingleRow(scoreSourceMapper.updateById(current), "成绩来源历史版本切换失败");
    }

    LocalDateTime now = LocalDateTime.now();
    CompetitionScoreSourceEntity entity = new CompetitionScoreSourceEntity();
    entity.setId(IdWorker.getId());
    entity.setTenantId(principal.getTenantId());
    entity.setEventId(eventId);
    entity.setStageId(request.stageId());
    entity.setEventWorkTypeId(request.eventWorkTypeId());
    entity.setEventWorkTypeLevelId(request.eventWorkTypeLevelId());
    entity.setContestantMemberId(request.contestantMemberId());
    entity.setSourceType(request.sourceType());
    entity.setRawScore(request.rawScore());
    entity.setConfirmationStatus(request.confirmationStatus());
    entity.setSourceSystem(normalizeSourceSystem(request.sourceSystem()));
    entity.setSourceReference(trimToNull(request.sourceReference()));
    entity.setScoreRuleId(request.scoreRuleId());
    entity.setRevisionNo(revisionNo);
    entity.setCurrentFlag(1);
    entity.setAchievedAt(request.achievedAt());
    if (CURRENT_CONFIRMED.equals(request.confirmationStatus())) {
      entity.setConfirmedAt(now);
      entity.setConfirmedBy(principal.getUserId());
    }
    entity.setCreatedBy(principal.getUserId());
    entity.setUpdatedBy(principal.getUserId());
    ensureSingleRow(scoreSourceMapper.insert(entity), "成绩来源修订保存失败");
    if (CURRENT_CONFIRMED.equals(entity.getConfirmationStatus())
        && scorePipelineOutboxService != null) {
      scorePipelineOutboxService.enqueue(entity, principal.getUserId());
    }
    return CompetitionScoringConverter.toVO(entity);
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionTotalScoreSnapshotVO> listTotalScoreSnapshots(
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long contestantMemberId,
      Long scoreRuleId,
      PageQuery pageQuery) {
    return listTotalScoreSnapshots(eventId, stageId, eventWorkTypeId, null, contestantMemberId,
        scoreRuleId, pageQuery);
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionTotalScoreSnapshotVO> listTotalScoreSnapshots(
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      Long contestantMemberId,
      Long scoreRuleId,
      PageQuery pageQuery) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    IPage<CompetitionTotalScoreSnapshotEntity> page =
        totalScoreSnapshotMapper.selectPage(
            new Page<>(pageQuery.getPage(), pageQuery.getPageSize()),
            new LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity>()
                .eq(CompetitionTotalScoreSnapshotEntity::getEventId, eventId)
                .eq(CompetitionTotalScoreSnapshotEntity::getStageId, stageId)
                .eq(CompetitionTotalScoreSnapshotEntity::getEventWorkTypeId, eventWorkTypeId)
                .eq(eventWorkTypeLevelId != null,
                    CompetitionTotalScoreSnapshotEntity::getEventWorkTypeLevelId,
                    eventWorkTypeLevelId)
                .eq(contestantMemberId != null,
                    CompetitionTotalScoreSnapshotEntity::getContestantMemberId,
                    contestantMemberId)
                .eq(scoreRuleId != null,
                    CompetitionTotalScoreSnapshotEntity::getScoreRuleId, scoreRuleId)
                .orderByAsc(CompetitionTotalScoreSnapshotEntity::getContestantMemberId)
                .orderByDesc(CompetitionTotalScoreSnapshotEntity::getCalculationVersion));
    if (page == null || page.getRecords() == null) {
      log.warn("Total score mapper returned null page, tenantId={}, eventId={}",
          principal.getTenantId(), eventId);
      return PageResponse.empty(pageQuery.getPage(), pageQuery.getPageSize());
    }
    return new PageResponse<>(
        page.getRecords().stream().map(CompetitionScoringConverter::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CompetitionTotalScoreSnapshotVO calculateTotalScore(
      Long eventId, CompetitionTotalScoreCalculateReq request) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    validateScoringScope(
        principal.getTenantId(), eventId, request.stageId(),
        request.eventWorkTypeId(), request.eventWorkTypeLevelId(),
        request.contestantMemberId(), request.scoreRuleId());
    CompetitionScoreRuleEntity rule =
        scoreRuleDomainService.getById(principal.getTenantId(), request.scoreRuleId());

    CompetitionScoreSourceEntity theory = requireCurrentSource(
        eventId, request, CompetitionScoreSourceType.THEORY);
    CompetitionScoreSourceEntity simulation = requireCurrentSource(
        eventId, request, CompetitionScoreSourceType.SIMULATION);
    CompetitionScoreSourceEntity practical = requireCurrentSource(
        eventId, request, CompetitionScoreSourceType.PRACTICAL);
    CompetitionScoringSystemBCalculator.SystemBCalculationResult calculation =
        rule.getSimulationWeight() != null || rule.getPracticalWeight() != null
            ? systemBCalculator.calculateLevelScoped(
                scoreInput(theory), scoreInput(simulation), scoreInput(practical),
                rule.getTheoryWeight(), rule.getSimulationWeight(), rule.getPracticalWeight())
            : systemBCalculator.calculate(
                scoreInput(theory), scoreInput(simulation), scoreInput(practical),
                rule.getTheoryWeight(), rule.getSimulationPracticalWeight());

    List<CompetitionTotalScoreSnapshotEntity> existing = safeList(
        totalScoreSnapshotMapper.selectList(
            totalSnapshotScope(eventId, request)
                .orderByDesc(CompetitionTotalScoreSnapshotEntity::getCalculationVersion)
                .last("FOR UPDATE")),
        "total score snapshots", principal, eventId);
    CompetitionTotalScoreSnapshotEntity current = existing.stream()
        .filter(item -> Integer.valueOf(1).equals(item.getCurrentFlag()))
        .findFirst()
        .orElse(null);
    int calculationVersion = existing.stream()
        .map(CompetitionTotalScoreSnapshotEntity::getCalculationVersion)
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(0) + 1;
    if (current != null) {
      current.setCurrentFlag(0);
      current.setUpdatedBy(principal.getUserId());
      ensureSingleRow(
          totalScoreSnapshotMapper.updateById(current),
          "总分历史版本切换失败");
    }

    CompetitionTotalScoreSnapshotEntity snapshot = new CompetitionTotalScoreSnapshotEntity();
    snapshot.setId(IdWorker.getId());
    snapshot.setTenantId(principal.getTenantId());
    snapshot.setEventId(eventId);
    snapshot.setStageId(request.stageId());
    snapshot.setEventWorkTypeId(request.eventWorkTypeId());
    snapshot.setEventWorkTypeLevelId(request.eventWorkTypeLevelId());
    snapshot.setContestantMemberId(request.contestantMemberId());
    snapshot.setScoreRuleId(request.scoreRuleId());
    snapshot.setTheorySourceId(theory.getId());
    snapshot.setSimulationSourceId(simulation.getId());
    snapshot.setPracticalSourceId(practical.getId());
    snapshot.setTheoryScore(theory.getRawScore());
    snapshot.setSimulationScore(simulation.getRawScore());
    snapshot.setPracticalScore(practical.getRawScore());
    snapshot.setSimulationPracticalScore(calculation.simulationPracticalScore());
    snapshot.setTheoryWeight(rule.getTheoryWeight());
    snapshot.setSimulationWeight(rule.getSimulationWeight());
    snapshot.setPracticalWeight(rule.getPracticalWeight());
    snapshot.setSimulationPracticalWeight(rule.getSimulationPracticalWeight());
    snapshot.setTotalScore(calculation.totalScore());
    snapshot.setCalculationVersion(calculationVersion);
    snapshot.setConfirmationStatus(calculation.confirmed()
        ? CURRENT_CONFIRMED : CompetitionScoreConfirmationStatus.PENDING.getCode());
    snapshot.setCurrentFlag(1);
    snapshot.setCalculatedAt(LocalDateTime.now());
    snapshot.setCreatedBy(principal.getUserId());
    snapshot.setUpdatedBy(principal.getUserId());
    ensureSingleRow(totalScoreSnapshotMapper.insert(snapshot), "总分快照保存失败");
    return CompetitionScoringConverter.toVO(snapshot);
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionRankingSnapshotVO> listRankingSnapshots(
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long scoreRuleId,
      PageQuery pageQuery) {
    return listRankingSnapshots(eventId, stageId, eventWorkTypeId, null, scoreRuleId, pageQuery);
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionRankingSnapshotVO> listRankingSnapshots(
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      Long scoreRuleId,
      PageQuery pageQuery) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    IPage<CompetitionRankingSnapshotEntity> page = rankingSnapshotMapper.selectPage(
        new Page<>(pageQuery.getPage(), pageQuery.getPageSize()),
        new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
            .eq(CompetitionRankingSnapshotEntity::getEventId, eventId)
            .eq(CompetitionRankingSnapshotEntity::getStageId, stageId)
            .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(eventWorkTypeLevelId != null,
                CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId, eventWorkTypeLevelId)
            .eq(scoreRuleId != null,
                CompetitionRankingSnapshotEntity::getScoreRuleId, scoreRuleId)
            .orderByDesc(CompetitionRankingSnapshotEntity::getSnapshotVersion));
    if (page == null || page.getRecords() == null) {
      log.warn("Ranking snapshot mapper returned null page, tenantId={}, eventId={}",
          principal.getTenantId(), eventId);
      return PageResponse.empty(pageQuery.getPage(), pageQuery.getPageSize());
    }
    return new PageResponse<>(
        page.getRecords().stream().map(CompetitionScoringConverter::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionRankingEntryVO> listRankingEntries(
      Long rankingSnapshotId, PageQuery pageQuery) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRankingSnapshotEntity snapshot =
        findRankingSnapshot(rankingSnapshotId);
    scoreRuleApplicationService.accessibleEvent(principal, snapshot.getEventId());
    IPage<CompetitionRankingEntryEntity> page = rankingEntryMapper.selectPage(
        new Page<>(pageQuery.getPage(), pageQuery.getPageSize()),
        new LambdaQueryWrapper<CompetitionRankingEntryEntity>()
            .eq(CompetitionRankingEntryEntity::getRankingSnapshotId, rankingSnapshotId)
            .orderByAsc(CompetitionRankingEntryEntity::getSortOrder));
    if (page == null || page.getRecords() == null) {
      log.warn("Ranking entry mapper returned null page, tenantId={}, snapshotId={}",
          principal.getTenantId(), rankingSnapshotId);
      return PageResponse.empty(pageQuery.getPage(), pageQuery.getPageSize());
    }
    return new PageResponse<>(
        page.getRecords().stream().map(CompetitionScoringConverter::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CompetitionRankingSnapshotDetailVO generateRankingSnapshot(
      Long eventId, CompetitionRankingSnapshotGenerateReq request) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    request.candidates().stream()
        .map(CompetitionRankingSnapshotGenerateReq.Candidate::contestantMemberId)
        .distinct()
        .sorted()
        .forEach(memberId -> validateScopeForWrite(
            principal.getTenantId(), eventId, request.stageId(), request.eventWorkTypeId(), memberId));
    scoreRuleReferenceValidator.validatePublishedRule(
        principal.getTenantId(), eventId, request.stageId(),
        request.eventWorkTypeId(), request.scoreRuleId());
    validateUniqueCandidates(request.candidates());

    CompetitionRankingAllocator.RankingScope scope =
        new CompetitionRankingAllocator.RankingScope(
            eventId, request.stageId(), request.eventWorkTypeId(), request.scoreRuleId());
    List<CompetitionRankingAllocator.RankingCandidate> candidates = new ArrayList<>();
    List<Long> totalSnapshotIds = new ArrayList<>();
    for (CompetitionRankingSnapshotGenerateReq.Candidate item : request.candidates()) {
      CompetitionRankingContestantStatus status = parseContestantStatus(item.contestantStatus());
      if (status == CompetitionRankingContestantStatus.ABSENT) {
        if (item.totalScoreSnapshotId() != null
            || (item.tieKey() != null && !item.tieKey().isBlank())) {
          throw new BusinessException(
              ErrorCode.UNPROCESSABLE_ENTITY,
              "缺考选手不得引用总分快照或提供并列键");
        }
        candidates.add(new CompetitionRankingAllocator.RankingCandidate(
            scope, item.contestantMemberId(), BigDecimal.ZERO, null, status));
        totalSnapshotIds.add(null);
        continue;
      }
      if (item.totalScoreSnapshotId() == null
          || item.tieKey() == null
          || item.tieKey().isBlank()) {
        throw new BusinessException(
            ErrorCode.UNPROCESSABLE_ENTITY,
            "正常完赛选手必须引用已确认总分快照并提供完整排序键摘要");
      }
      CompetitionTotalScoreSnapshotEntity total = requireTotalSnapshot(
          eventId, request, item);
      candidates.add(new CompetitionRankingAllocator.RankingCandidate(
          scope, item.contestantMemberId(), total.getTotalScore(), item.tieKey(), status));
      totalSnapshotIds.add(total.getId());
    }
    validateCandidateOrdering(candidates);
    List<CompetitionRankingAllocator.RankedCandidate> ranked =
        rankingAllocator.allocate(scope, candidates);

    List<CompetitionRankingSnapshotEntity> priorSnapshots = safeList(
        rankingSnapshotMapper.selectList(
            rankingSnapshotScope(eventId, request)
                .orderByDesc(CompetitionRankingSnapshotEntity::getSnapshotVersion)
                .last("FOR UPDATE")),
        "ranking snapshots", principal, eventId);
    int snapshotVersion = priorSnapshots.stream()
        .map(CompetitionRankingSnapshotEntity::getSnapshotVersion)
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(0) + 1;

    CompetitionRankingSnapshotEntity snapshot = new CompetitionRankingSnapshotEntity();
    snapshot.setId(IdWorker.getId());
    snapshot.setTenantId(principal.getTenantId());
    snapshot.setEventId(eventId);
    snapshot.setStageId(request.stageId());
    snapshot.setEventWorkTypeId(request.eventWorkTypeId());
    snapshot.setEventWorkTypeLevelId(request.eventWorkTypeLevelId());
    snapshot.setScoreRuleId(request.scoreRuleId());
    snapshot.setSnapshotVersion(snapshotVersion);
    snapshot.setStatus(SNAPSHOT_DRAFT);
    snapshot.setGenerationReason(trimToNull(request.generationReason()));
    snapshot.setGeneratedAt(LocalDateTime.now());
    snapshot.setCreatedBy(principal.getUserId());
    snapshot.setUpdatedBy(principal.getUserId());
    ensureSingleRow(rankingSnapshotMapper.insert(snapshot), "排名快照保存失败");

    List<CompetitionRankingEntryVO> entries = new ArrayList<>();
    for (int index = 0; index < ranked.size(); index++) {
      CompetitionRankingAllocator.RankedCandidate item = ranked.get(index);
      CompetitionRankingEntryEntity entry = new CompetitionRankingEntryEntity();
      entry.setId(IdWorker.getId());
      entry.setTenantId(principal.getTenantId());
      entry.setRankingSnapshotId(snapshot.getId());
      entry.setContestantMemberId(item.contestantMemberId());
      entry.setTotalScoreSnapshotId(totalSnapshotIds.get(index));
      entry.setTotalScore(item.totalScore());
      entry.setDisplayRank(item.rank());
      entry.setTieKey(item.tieKey());
      entry.setTiedFlag(item.tied() ? 1 : 0);
      entry.setContestantStatus(item.status().getCode());
      entry.setSortOrder(index + 1);
      entry.setCreatedBy(principal.getUserId());
      entry.setUpdatedBy(principal.getUserId());
      ensureSingleRow(rankingEntryMapper.insert(entry), "排名快照条目保存失败");
      entries.add(CompetitionScoringConverter.toVO(entry));
    }
    return new CompetitionRankingSnapshotDetailVO(
        CompetitionScoringConverter.toVO(snapshot), List.copyOf(entries));
  }

  @Transactional
  public CompetitionRankingSnapshotVO confirmRankingSnapshot(Long snapshotId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRankingSnapshotEntity snapshot = requireRankingSnapshot(snapshotId, principal);
    if ("confirmed".equals(snapshot.getStatus()) || "published".equals(snapshot.getStatus())) {
      return CompetitionScoringConverter.toVO(snapshot);
    }
    if (!"draft".equals(snapshot.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有排名草稿可以复核确认");
    }
    if (snapshot.getInputFingerprint() == null || snapshot.getInputFingerprint().isBlank()) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅自动生成的排名草稿可以复核确认");
    }
    assertRankingInputsCurrent(snapshot);
    snapshot.setStatus("confirmed");
    snapshot.setConfirmedAt(LocalDateTime.now());
    snapshot.setConfirmedBy(principal.getUserId());
    snapshot.setUpdatedBy(principal.getUserId());
    ensureSingleRow(rankingSnapshotMapper.updateById(snapshot), "排名快照确认失败");
    return CompetitionScoringConverter.toVO(snapshot);
  }

  @Transactional
  public CompetitionRankingSnapshotVO publishRankingSnapshot(Long snapshotId, String correctionReason) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRankingSnapshotEntity snapshot = requireRankingSnapshot(snapshotId, principal);
    if ("published".equals(snapshot.getStatus())) return CompetitionScoringConverter.toVO(snapshot);
    if (!"confirmed".equals(snapshot.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "排名必须先复核确认后才能发布");
    }
    assertRankingInputsCurrent(snapshot);
    CompetitionRankingSnapshotEntity previous = rankingSnapshotMapper.selectOne(
        rankingSnapshotScope(snapshot).eq(CompetitionRankingSnapshotEntity::getStatus, "published")
            .last("FOR UPDATE"));
    if (previous != null && (correctionReason == null || correctionReason.isBlank())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "更正发布必须填写原因");
    }
    LocalDateTime now = LocalDateTime.now();
    if (previous != null) {
      previous.setStatus("superseded");
      previous.setSupersededAt(now);
      previous.setSupersededBy(principal.getUserId());
      previous.setUpdatedBy(principal.getUserId());
      ensureSingleRow(rankingSnapshotMapper.updateById(previous), "旧排名版本替代失败");
      snapshot.setSupersedesSnapshotId(previous.getId());
      snapshot.setCorrectionReason(correctionReason.trim());
    }
    snapshot.setStatus("published");
    snapshot.setPublishedAt(now);
    snapshot.setPublishedBy(principal.getUserId());
    snapshot.setUpdatedBy(principal.getUserId());
    ensureSingleRow(rankingSnapshotMapper.updateById(snapshot), "排名快照发布失败");
    return CompetitionScoringConverter.toVO(snapshot);
  }

  private CompetitionRankingSnapshotEntity requireRankingSnapshot(
      Long snapshotId, PlatformUserPrincipal principal) {
    CompetitionRankingSnapshotEntity snapshot = rankingSnapshotMapper.selectById(snapshotId);
    if (snapshot == null || !Objects.equals(snapshot.getTenantId(), principal.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排名快照不存在");
    }
    scoreRuleApplicationService.accessibleEvent(principal, snapshot.getEventId());
    return snapshot;
  }

  private void assertRankingInputsCurrent(CompetitionRankingSnapshotEntity snapshot) {
    if (automaticScorePipelineService != null
        && !automaticScorePipelineService.isRankingFingerprintCurrent(snapshot)) {
      throw new BusinessException(ErrorCode.CONFLICT, "名单或成绩输入已变化，请使用最新排名草稿");
    }
    List<CompetitionRankingEntryEntity> entries = rankingEntryMapper.selectList(
        new LambdaQueryWrapper<CompetitionRankingEntryEntity>()
            .eq(CompetitionRankingEntryEntity::getRankingSnapshotId, snapshot.getId())
            .eq(CompetitionRankingEntryEntity::getDeleted, 0));
    for (CompetitionRankingEntryEntity entry : safeList(entries, "ranking entries",
        currentUser(), snapshot.getEventId())) {
      if (entry.getTotalScoreSnapshotId() == null) continue;
      CompetitionTotalScoreSnapshotEntity total =
          totalScoreSnapshotMapper.selectById(entry.getTotalScoreSnapshotId());
      if (total == null || !Integer.valueOf(1).equals(total.getCurrentFlag())
          || !CURRENT_CONFIRMED.equals(total.getConfirmationStatus())) {
        throw new BusinessException(ErrorCode.CONFLICT, "成绩输入已变化，请使用最新排名草稿");
      }
    }
  }

  private LambdaQueryWrapper<CompetitionRankingSnapshotEntity> rankingSnapshotScope(
      CompetitionRankingSnapshotEntity snapshot) {
    return new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
        .eq(CompetitionRankingSnapshotEntity::getEventId, snapshot.getEventId())
        .eq(CompetitionRankingSnapshotEntity::getStageId, snapshot.getStageId())
        .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeId, snapshot.getEventWorkTypeId())
        .eq(snapshot.getEventWorkTypeLevelId() != null,
            CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId,
            snapshot.getEventWorkTypeLevelId())
        .isNull(snapshot.getEventWorkTypeLevelId() == null,
            CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId)
        .eq(snapshot.getScoreRuleId() != null,
            CompetitionRankingSnapshotEntity::getScoreRuleId, snapshot.getScoreRuleId())
        .isNull(snapshot.getScoreRuleId() == null,
            CompetitionRankingSnapshotEntity::getScoreRuleId)
        .eq(CompetitionRankingSnapshotEntity::getDeleted, 0);
  }

  private LambdaQueryWrapper<CompetitionScoreSourceEntity> sourceRevisionScope(
      Long eventId, CompetitionScoreSourceRevisionReq request) {
    return new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
        .eq(CompetitionScoreSourceEntity::getEventId, eventId)
        .eq(CompetitionScoreSourceEntity::getStageId, request.stageId())
        .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, request.eventWorkTypeId())
        .eq(CompetitionScoreSourceEntity::getEventWorkTypeLevelId, request.eventWorkTypeLevelId())
        .eq(CompetitionScoreSourceEntity::getContestantMemberId, request.contestantMemberId())
        .eq(CompetitionScoreSourceEntity::getSourceType, request.sourceType());
  }

  private LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity> totalSnapshotScope(
      Long eventId, CompetitionTotalScoreCalculateReq request) {
    return new LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity>()
        .eq(CompetitionTotalScoreSnapshotEntity::getEventId, eventId)
        .eq(CompetitionTotalScoreSnapshotEntity::getStageId, request.stageId())
        .eq(CompetitionTotalScoreSnapshotEntity::getEventWorkTypeId, request.eventWorkTypeId())
        .eq(CompetitionTotalScoreSnapshotEntity::getEventWorkTypeLevelId, request.eventWorkTypeLevelId())
        .eq(CompetitionTotalScoreSnapshotEntity::getContestantMemberId,
            request.contestantMemberId())
        .eq(CompetitionTotalScoreSnapshotEntity::getScoreRuleId, request.scoreRuleId());
  }

  private LambdaQueryWrapper<CompetitionRankingSnapshotEntity> rankingSnapshotScope(
      Long eventId, CompetitionRankingSnapshotGenerateReq request) {
    return new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
        .eq(CompetitionRankingSnapshotEntity::getEventId, eventId)
        .eq(CompetitionRankingSnapshotEntity::getStageId, request.stageId())
        .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeId, request.eventWorkTypeId())
        .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId, request.eventWorkTypeLevelId())
        .eq(CompetitionRankingSnapshotEntity::getScoreRuleId, request.scoreRuleId());
  }

  private CompetitionScoreSourceEntity requireCurrentSource(
      Long eventId,
      CompetitionTotalScoreCalculateReq request,
      CompetitionScoreSourceType type) {
    CompetitionScoreSourceEntity entity = scoreSourceMapper.selectOne(
        new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
            .eq(CompetitionScoreSourceEntity::getEventId, eventId)
            .eq(CompetitionScoreSourceEntity::getStageId, request.stageId())
            .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, request.eventWorkTypeId())
            .eq(CompetitionScoreSourceEntity::getEventWorkTypeLevelId, request.eventWorkTypeLevelId())
            .eq(CompetitionScoreSourceEntity::getContestantMemberId,
                request.contestantMemberId())
            .eq(CompetitionScoreSourceEntity::getScoreRuleId, request.scoreRuleId())
            .eq(CompetitionScoreSourceEntity::getSourceType, type.getCode())
            .eq(CompetitionScoreSourceEntity::getCurrentFlag, 1));
    if (entity == null) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY,
          "缺少当前" + type.getCode() + "成绩来源");
    }
    return entity;
  }

  private CompetitionScoringSystemBCalculator.ScoreInput scoreInput(
      CompetitionScoreSourceEntity source) {
    return new CompetitionScoringSystemBCalculator.ScoreInput(
        source.getRawScore(), CURRENT_CONFIRMED.equals(source.getConfirmationStatus()));
  }

  private CompetitionTotalScoreSnapshotEntity requireTotalSnapshot(
      Long eventId,
      CompetitionRankingSnapshotGenerateReq request,
      CompetitionRankingSnapshotGenerateReq.Candidate candidate) {
    CompetitionTotalScoreSnapshotEntity total = totalScoreSnapshotMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity>()
            .eq(CompetitionTotalScoreSnapshotEntity::getId, candidate.totalScoreSnapshotId())
            .eq(CompetitionTotalScoreSnapshotEntity::getEventId, eventId)
            .eq(CompetitionTotalScoreSnapshotEntity::getStageId, request.stageId())
            .eq(CompetitionTotalScoreSnapshotEntity::getEventWorkTypeId,
                request.eventWorkTypeId())
            .eq(CompetitionTotalScoreSnapshotEntity::getEventWorkTypeLevelId,
                request.eventWorkTypeLevelId())
            .eq(CompetitionTotalScoreSnapshotEntity::getContestantMemberId,
                candidate.contestantMemberId())
            .eq(CompetitionTotalScoreSnapshotEntity::getScoreRuleId, request.scoreRuleId())
            .eq(CompetitionTotalScoreSnapshotEntity::getConfirmationStatus, CURRENT_CONFIRMED));
    if (total == null || total.getTotalScore() == null) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY,
          "排名候选引用的总分快照不存在、未确认或不属于当前范围");
    }
    return total;
  }

  private CompetitionRankingSnapshotEntity findRankingSnapshot(
      Long rankingSnapshotId) {
    CompetitionRankingSnapshotEntity snapshot = rankingSnapshotMapper.selectOne(
        new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
            .eq(CompetitionRankingSnapshotEntity::getId, rankingSnapshotId));
    if (snapshot == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排名快照不存在");
    }
    return snapshot;
  }

  private void validateScoringScope(
      Long tenantId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      Long contestantMemberId,
      Long scoreRuleId) {
    validateScopeForWrite(tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId);
    scoreRuleReferenceValidator.validatePublishedRule(
        tenantId, eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId, scoreRuleId);
  }

  private void validateScopeForWrite(
      Long tenantId, Long eventId, Long stageId, Long eventWorkTypeId, Long contestantMemberId) {
    // Keep the read-only rejection for existing callers, then acquire the shared profile lock
    // before any score/source/snapshot write. The second check closes the validation race.
    contestantScopeValidator.validate(
        tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId);
    contestantScopeValidator.validateForUpdate(
        tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId);
  }

  private void validateUniqueCandidates(
      List<CompetitionRankingSnapshotGenerateReq.Candidate> candidates) {
    Set<Long> contestantIds = new HashSet<>();
    for (CompetitionRankingSnapshotGenerateReq.Candidate candidate : candidates) {
      if (!contestantIds.add(candidate.contestantMemberId())) {
        throw new BusinessException(ErrorCode.CONFLICT, "排名候选选手不能重复");
      }
    }
  }

  private void validateCandidateOrdering(
      List<CompetitionRankingAllocator.RankingCandidate> candidates) {
    BigDecimal previousScore = null;
    String previousTieKey = null;
    boolean absentReached = false;
    Set<String> closedTieKeys = new HashSet<>();
    for (CompetitionRankingAllocator.RankingCandidate candidate : candidates) {
      if (candidate.status() == CompetitionRankingContestantStatus.ABSENT) {
        absentReached = true;
        continue;
      }
      if (absentReached) {
        throw new BusinessException(
            ErrorCode.UNPROCESSABLE_ENTITY,
            "正常完赛候选必须排在缺考候选之前");
      }
      if (previousScore != null && previousScore.compareTo(candidate.totalScore()) < 0) {
        throw new BusinessException(
            ErrorCode.UNPROCESSABLE_ENTITY,
            "排名候选总分必须按降序排列");
      }
      if (!Objects.equals(previousTieKey, candidate.tieKey())) {
        if (previousTieKey != null) {
          closedTieKeys.add(previousTieKey);
        }
        if (closedTieKeys.contains(candidate.tieKey())) {
          throw new BusinessException(
              ErrorCode.UNPROCESSABLE_ENTITY,
              "相同并列键的候选必须连续排列");
        }
      } else if (previousScore != null
          && previousScore.compareTo(candidate.totalScore()) != 0) {
        throw new BusinessException(
            ErrorCode.UNPROCESSABLE_ENTITY,
            "相同并列键必须对应相同总分");
      }
      previousScore = candidate.totalScore();
      previousTieKey = candidate.tieKey();
    }
  }

  private void validateScoreSourceType(String value) {
    for (CompetitionScoreSourceType type : CompetitionScoreSourceType.values()) {
      if (type.getCode().equals(value)) {
        return;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "成绩来源类型不合法");
  }

  private void validateConfirmationStatus(String value) {
    for (CompetitionScoreConfirmationStatus status :
        CompetitionScoreConfirmationStatus.values()) {
      if (status.getCode().equals(value)) {
        return;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "成绩确认状态不合法");
  }

  private CompetitionRankingContestantStatus parseContestantStatus(String value) {
    for (CompetitionRankingContestantStatus status :
        CompetitionRankingContestantStatus.values()) {
      if (status.getCode().equals(value)) {
        return status;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "排名选手状态不合法");
  }

  private String normalizeSourceSystem(String value) {
    String normalized = trimToNull(value);
    return normalized == null ? "competition" : normalized;
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private void ensureSingleRow(Integer affectedRows, String message) {
    if (affectedRows == null || affectedRows != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, message);
    }
  }

  private <T> List<T> safeList(
      List<T> values, String source, PlatformUserPrincipal principal, Long eventId) {
    if (values != null) {
      return values;
    }
    log.warn("Competition scoring mapper returned null {}, tenantId={}, eventId={}",
        source, principal.getTenantId(), eventId);
    return Collections.emptyList();
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition scoring request has no authenticated platform user");
      throw ex;
    }
  }
}
