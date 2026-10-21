package com.guoyu.platform.competition.player.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionTotalScoreSnapshotEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionTotalScoreSnapshotMapper;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.player.security.CompetitionPlayerSecurityContext;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerEventResultVO;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerEventResultVO.ResultItem;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerEventResultVO.SubjectScore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerScoreApplicationService {

  private static final String ELIGIBLE_ACTIVATED = "eligible_activated";
  private static final String SNAPSHOT_PUBLISHED = "published";

  private final CompetitionRegistrationMapper registrationMapper;
  private final CompetitionEventMapper eventMapper;
  private final CompetitionStageMapper stageMapper;
  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper workTypeLevelMapper;
  private final CompetitionRankingSnapshotMapper rankingSnapshotMapper;
  private final CompetitionRankingEntryMapper rankingEntryMapper;
  private final CompetitionTotalScoreSnapshotMapper totalScoreSnapshotMapper;

  public CompetitionPlayerScoreApplicationService(
      CompetitionRegistrationMapper registrationMapper,
      CompetitionEventMapper eventMapper,
      CompetitionStageMapper stageMapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper workTypeLevelMapper,
      CompetitionRankingSnapshotMapper rankingSnapshotMapper,
      CompetitionRankingEntryMapper rankingEntryMapper,
      CompetitionTotalScoreSnapshotMapper totalScoreSnapshotMapper) {
    this.registrationMapper = registrationMapper;
    this.eventMapper = eventMapper;
    this.stageMapper = stageMapper;
    this.workTypeMapper = workTypeMapper;
    this.workTypeLevelMapper = workTypeLevelMapper;
    this.rankingSnapshotMapper = rankingSnapshotMapper;
    this.rankingEntryMapper = rankingEntryMapper;
    this.totalScoreSnapshotMapper = totalScoreSnapshotMapper;
  }

  @Transactional(readOnly = true)
  public CompetitionPlayerEventResultVO getEventResult(Long eventId) {
    CompetitionPlayerPrincipal player = currentPlayer();
    CompetitionEventEntity event = eventMapper.selectById(eventId);
    if (event == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在或当前选手无权查看");
    }

    List<CompetitionRegistrationEntity> registrations = registrationMapper.selectList(
        new LambdaQueryWrapper<CompetitionRegistrationEntity>()
            .eq(CompetitionRegistrationEntity::getEventId, eventId)
            .eq(CompetitionRegistrationEntity::getPlayerId, player.getPlayerId())
            .eq(CompetitionRegistrationEntity::getStatus, ELIGIBLE_ACTIVATED));
    Set<Long> memberIds = new LinkedHashSet<>();
    if (registrations != null) {
      registrations.stream()
          .map(CompetitionRegistrationEntity::getMemberId)
          .filter(Objects::nonNull)
          .forEach(memberIds::add);
    }
    if (memberIds.isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "当前选手暂无该赛事参赛资格");
    }

    List<CompetitionRankingSnapshotEntity> snapshots = rankingSnapshotMapper.selectList(
        new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
            .eq(CompetitionRankingSnapshotEntity::getEventId, eventId)
            .eq(CompetitionRankingSnapshotEntity::getStatus, SNAPSHOT_PUBLISHED)
            .orderByDesc(CompetitionRankingSnapshotEntity::getPublishedAt)
            .orderByDesc(CompetitionRankingSnapshotEntity::getSnapshotVersion));
    if (snapshots == null || snapshots.isEmpty()) {
      return unpublished(event.getId(), event.getName());
    }

    List<Long> snapshotIds = snapshots.stream()
        .map(CompetitionRankingSnapshotEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    List<CompetitionRankingEntryEntity> entries = rankingEntryMapper.selectList(
        new LambdaQueryWrapper<CompetitionRankingEntryEntity>()
            .in(CompetitionRankingEntryEntity::getRankingSnapshotId, snapshotIds)
            .in(CompetitionRankingEntryEntity::getContestantMemberId, memberIds));
    if (entries == null || entries.isEmpty()) {
      return unpublished(event.getId(), event.getName());
    }

    Map<Long, List<CompetitionRankingEntryEntity>> entriesBySnapshot = new HashMap<>();
    entries.forEach(entry -> entriesBySnapshot
        .computeIfAbsent(entry.getRankingSnapshotId(), ignored -> new ArrayList<>())
        .add(entry));

    List<Long> totalSnapshotIds = entries.stream()
        .map(CompetitionRankingEntryEntity::getTotalScoreSnapshotId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<Long, CompetitionTotalScoreSnapshotEntity> totalsById = new HashMap<>();
    if (!totalSnapshotIds.isEmpty()) {
      List<CompetitionTotalScoreSnapshotEntity> totals =
          totalScoreSnapshotMapper.selectBatchIds(totalSnapshotIds);
      if (totals != null) totals.forEach(total -> totalsById.put(total.getId(), total));
    }

    Map<Long, CompetitionStageEntity> stagesById = indexStages(snapshots);
    Map<Long, CompetitionEventWorkTypeEntity> workTypesById = indexWorkTypes(snapshots);
    Map<Long, CompetitionEventWorkTypeLevelEntity> levelsById = indexLevels(snapshots);

    List<ResultItem> results = new ArrayList<>();
    for (CompetitionRankingSnapshotEntity snapshot : snapshots) {
      List<CompetitionRankingEntryEntity> snapshotEntries =
          entriesBySnapshot.getOrDefault(snapshot.getId(), List.of());
      for (CompetitionRankingEntryEntity entry : snapshotEntries) {
        CompetitionTotalScoreSnapshotEntity total =
            totalsById.get(entry.getTotalScoreSnapshotId());
        List<SubjectScore> subjects = total != null && matchesPublishedScope(snapshot, entry, total)
            ? subjectScores(total)
            : List.of();
        CompetitionStageEntity stage = stagesById.get(snapshot.getStageId());
        CompetitionEventWorkTypeEntity workType = workTypesById.get(snapshot.getEventWorkTypeId());
        CompetitionEventWorkTypeLevelEntity level =
            levelsById.get(snapshot.getEventWorkTypeLevelId());
        results.add(new ResultItem(
            entry.getId(),
            snapshot.getId(),
            snapshot.getSnapshotVersion(),
            stage == null ? null : stage.getName(),
            workType == null ? null : workType.getDisplayName(),
            level == null ? null : level.getLevelName(),
            entry.getTotalScore(),
            entry.getDisplayRank(),
            entry.getTiedFlag(),
            entry.getContestantStatus(),
            snapshot.getPublishedAt(),
            subjects));
      }
    }

    if (results.isEmpty()) return unpublished(event.getId(), event.getName());
    return new CompetitionPlayerEventResultVO(
        event.getId(), event.getName(), SNAPSHOT_PUBLISHED,
        results.get(0).publishedAt(), List.copyOf(results));
  }

  private Map<Long, CompetitionStageEntity> indexStages(
      List<CompetitionRankingSnapshotEntity> snapshots) {
    List<Long> ids = snapshots.stream().map(CompetitionRankingSnapshotEntity::getStageId)
        .filter(Objects::nonNull).distinct().toList();
    Map<Long, CompetitionStageEntity> result = new HashMap<>();
    if (!ids.isEmpty()) {
      List<CompetitionStageEntity> rows = stageMapper.selectBatchIds(ids);
      if (rows != null) rows.forEach(row -> result.put(row.getId(), row));
    }
    return result;
  }

  private Map<Long, CompetitionEventWorkTypeEntity> indexWorkTypes(
      List<CompetitionRankingSnapshotEntity> snapshots) {
    List<Long> ids = snapshots.stream().map(CompetitionRankingSnapshotEntity::getEventWorkTypeId)
        .filter(Objects::nonNull).distinct().toList();
    Map<Long, CompetitionEventWorkTypeEntity> result = new HashMap<>();
    if (!ids.isEmpty()) {
      List<CompetitionEventWorkTypeEntity> rows = workTypeMapper.selectBatchIds(ids);
      if (rows != null) rows.forEach(row -> result.put(row.getId(), row));
    }
    return result;
  }

  private Map<Long, CompetitionEventWorkTypeLevelEntity> indexLevels(
      List<CompetitionRankingSnapshotEntity> snapshots) {
    List<Long> ids = snapshots.stream().map(CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId)
        .filter(Objects::nonNull).distinct().toList();
    Map<Long, CompetitionEventWorkTypeLevelEntity> result = new HashMap<>();
    if (!ids.isEmpty()) {
      List<CompetitionEventWorkTypeLevelEntity> rows = workTypeLevelMapper.selectBatchIds(ids);
      if (rows != null) rows.forEach(row -> result.put(row.getId(), row));
    }
    return result;
  }

  private boolean matchesPublishedScope(
      CompetitionRankingSnapshotEntity snapshot,
      CompetitionRankingEntryEntity entry,
      CompetitionTotalScoreSnapshotEntity total) {
    return Objects.equals(snapshot.getEventId(), total.getEventId())
        && Objects.equals(snapshot.getStageId(), total.getStageId())
        && Objects.equals(snapshot.getEventWorkTypeId(), total.getEventWorkTypeId())
        && Objects.equals(snapshot.getEventWorkTypeLevelId(), total.getEventWorkTypeLevelId())
        && Objects.equals(entry.getContestantMemberId(), total.getContestantMemberId());
  }

  private List<SubjectScore> subjectScores(CompetitionTotalScoreSnapshotEntity total) {
    List<SubjectScore> subjects = new ArrayList<>();
    addSubject(subjects, "theory", "理论", total.getTheoryWeight(), total.getTheoryScore());
    addSubject(subjects, "simulation", "模拟", total.getSimulationWeight(), total.getSimulationScore());
    addSubject(subjects, "practical", "实操", total.getPracticalWeight(), total.getPracticalScore());
    addSubject(subjects, "simulationPractical", "模拟实操",
        total.getSimulationPracticalWeight(), total.getSimulationPracticalScore());
    return List.copyOf(subjects);
  }

  private void addSubject(List<SubjectScore> subjects, String code, String name,
      BigDecimal weight, BigDecimal score) {
    if (weight != null && weight.signum() > 0 && score != null) {
      subjects.add(new SubjectScore(code, name, score));
    }
  }

  private CompetitionPlayerEventResultVO unpublished(Long eventId, String eventName) {
    return new CompetitionPlayerEventResultVO(eventId, eventName, "unpublished", null, List.of());
  }

  private CompetitionPlayerPrincipal currentPlayer() {
    try {
      return CompetitionPlayerSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Competition player score request has no authenticated player context");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "选手登录已失效");
    }
  }
}
