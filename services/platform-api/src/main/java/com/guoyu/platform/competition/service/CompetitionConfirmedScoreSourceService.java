package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.messaging.service.CompetitionScorePipelineOutboxService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Single write boundary for confirmed external score facts.
 * Callers provide a validated contestant and immutable external reference; this service owns
 * revisioning, current-version switching and downstream score/ranking pipeline dispatch.
 */
@Service
public class CompetitionConfirmedScoreSourceService {
  private final CompetitionScoreSourceMapper sourceMapper;
  private final CompetitionScorePipelineOutboxService pipelineOutboxService;
  private final CompetitionAutomaticScorePipelineService automaticScorePipelineService;

  public CompetitionConfirmedScoreSourceService(
      CompetitionScoreSourceMapper sourceMapper,
      CompetitionScorePipelineOutboxService pipelineOutboxService,
      CompetitionAutomaticScorePipelineService automaticScorePipelineService) {
    this.sourceMapper = sourceMapper;
    this.pipelineOutboxService = pipelineOutboxService;
    this.automaticScorePipelineService = automaticScorePipelineService;
  }

  public CompetitionScoreSourceEntity saveConfirmed(
      Long tenantId,
      Long operatorId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      Long contestantMemberId,
      String sourceType,
      BigDecimal rawScore,
      String sourceSystem,
      String sourceReference,
      Long scoreRuleId,
      LocalDateTime achievedAt) {
    CompetitionScoreSourceEntity duplicate = sourceMapper.selectOne(
        new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
            .eq(CompetitionScoreSourceEntity::getSourceSystem, sourceSystem)
            .eq(CompetitionScoreSourceEntity::getSourceReference, sourceReference)
            .eq(CompetitionScoreSourceEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (duplicate != null) {
      if (!contestantMemberId.equals(duplicate.getContestantMemberId())
          || rawScore.compareTo(duplicate.getRawScore()) != 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "外部成绩流水号已被其他成绩占用");
      }
      return duplicate;
    }

    LambdaQueryWrapper<CompetitionScoreSourceEntity> scope = scope(
        eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId, contestantMemberId, sourceType);
    CompetitionScoreSourceEntity latest = sourceMapper.selectOne(
        scope.clone().orderByDesc(CompetitionScoreSourceEntity::getRevisionNo).last("LIMIT 1"));
    CompetitionScoreSourceEntity current = sourceMapper.selectOne(
        scope.clone().eq(CompetitionScoreSourceEntity::getCurrentFlag, 1).last("LIMIT 1"));
    if (current != null) {
      current.setCurrentFlag(0);
      current.setUpdatedBy(operatorId);
      current.setUpdatedAt(LocalDateTime.now());
      if (sourceMapper.updateById(current) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "当前成绩版本已变化，请重试");
      }
    }

    LocalDateTime now = LocalDateTime.now();
    CompetitionScoreSourceEntity source = new CompetitionScoreSourceEntity();
    source.setId(IdWorker.getId());
    source.setTenantId(tenantId);
    source.setEventId(eventId);
    source.setStageId(stageId);
    source.setEventWorkTypeId(eventWorkTypeId);
    source.setEventWorkTypeLevelId(eventWorkTypeLevelId);
    source.setContestantMemberId(contestantMemberId);
    source.setSourceType(sourceType);
    source.setRawScore(rawScore);
    source.setConfirmationStatus("confirmed");
    source.setSourceSystem(sourceSystem);
    source.setSourceReference(sourceReference);
    source.setScoreRuleId(scoreRuleId);
    source.setRevisionNo(latest == null || latest.getRevisionNo() == null
        ? 1 : latest.getRevisionNo() + 1);
    source.setCurrentFlag(1);
    source.setAchievedAt(achievedAt == null ? now : achievedAt);
    source.setConfirmedAt(now);
    source.setConfirmedBy(operatorId);
    source.setCreatedBy(operatorId);
    source.setUpdatedBy(operatorId);
    source.setCreatedAt(now);
    source.setUpdatedAt(now);
    source.setDeleted(0);
    if (sourceMapper.insert(source) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "成绩来源保存失败");
    }
    pipelineOutboxService.enqueue(source, operatorId);
    automaticScorePipelineService.onConfirmedSource(source, operatorId);
    return source;
  }

  private LambdaQueryWrapper<CompetitionScoreSourceEntity> scope(
      Long eventId, Long stageId, Long workTypeId, Long levelId, Long memberId,
      String sourceType) {
    return new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
        .eq(CompetitionScoreSourceEntity::getEventId, eventId)
        .eq(CompetitionScoreSourceEntity::getStageId, stageId)
        .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, workTypeId)
        .eq(levelId != null, CompetitionScoreSourceEntity::getEventWorkTypeLevelId, levelId)
        .isNull(levelId == null, CompetitionScoreSourceEntity::getEventWorkTypeLevelId)
        .eq(CompetitionScoreSourceEntity::getContestantMemberId, memberId)
        .eq(CompetitionScoreSourceEntity::getSourceType, sourceType)
        .eq(CompetitionScoreSourceEntity::getDeleted, 0);
  }
}
