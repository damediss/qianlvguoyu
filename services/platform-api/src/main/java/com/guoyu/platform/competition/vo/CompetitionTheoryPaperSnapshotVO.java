package com.guoyu.platform.competition.vo;

import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionTheoryPaperSnapshotVO(
    Long id,
    Long eventId,
    Long stageId,
    Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    Long scoreRuleId,
    Long requirementId,
    String paperCode,
    Integer versionNo,
    Long sourceCourseId,
    Long sourcePlanId,
    Long sourcePaperId,
    Long sourceCompetitionPaperSnapshotId,
    String sourceTitle,
    String sourceHash,
    Integer durationMinutes,
    Integer questionCount,
    BigDecimal totalScore,
    BigDecimal passScore,
    String status,
    LocalDateTime importedAt,
    Long importedBy,
    LocalDateTime publishedAt
) {

  public static CompetitionTheoryPaperSnapshotVO from(
      CompetitionTheoryPaperSnapshotEntity entity) {
    return new CompetitionTheoryPaperSnapshotVO(
        entity.getId(),
        entity.getEventId(),
        entity.getStageId(),
        entity.getEventWorkTypeId(),
        entity.getEventWorkTypeLevelId(),
        entity.getScoreRuleId(),
        entity.getRequirementId(),
        entity.getPaperCode(),
        entity.getVersionNo(),
        entity.getSourceCourseId(),
        entity.getSourcePlanId(),
        entity.getSourcePaperId(),
        entity.getSourceCompetitionPaperSnapshotId(),
        entity.getSourceTitle(),
        entity.getSourceHash(),
        entity.getDurationMinutes(),
        entity.getQuestionCount(),
        entity.getTotalScore(),
        entity.getPassScore(),
        entity.getStatus(),
        entity.getImportedAt(),
        entity.getImportedBy(),
        entity.getPublishedAt());
  }
}
