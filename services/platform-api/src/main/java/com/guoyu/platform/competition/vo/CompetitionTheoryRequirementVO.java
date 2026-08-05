package com.guoyu.platform.competition.vo;

import com.guoyu.platform.competition.entity.CompetitionTheoryRequirementEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionTheoryRequirementVO(
    Long id,
    Long eventId,
    Long stageId,
    Long eventWorkTypeId,
    Long scoreRuleId,
    String templateCode,
    Integer durationMinutes,
    Integer singleCount,
    BigDecimal singleScore,
    Integer multiCount,
    BigDecimal multiScore,
    Integer judgeCount,
    BigDecimal judgeScore,
    BigDecimal totalScore,
    BigDecimal passScore,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public static CompetitionTheoryRequirementVO from(
      CompetitionTheoryRequirementEntity entity) {
    if (entity == null) {
      return null;
    }
    return new CompetitionTheoryRequirementVO(
        entity.getId(),
        entity.getEventId(),
        entity.getStageId(),
        entity.getEventWorkTypeId(),
        entity.getScoreRuleId(),
        entity.getTemplateCode(),
        entity.getDurationMinutes(),
        entity.getSingleCount(),
        entity.getSingleScore(),
        entity.getMultiCount(),
        entity.getMultiScore(),
        entity.getJudgeCount(),
        entity.getJudgeScore(),
        entity.getTotalScore(),
        entity.getPassScore(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
