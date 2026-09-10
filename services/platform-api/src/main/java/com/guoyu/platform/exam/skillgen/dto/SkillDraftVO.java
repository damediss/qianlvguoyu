package com.guoyu.platform.exam.skillgen.dto;

import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;

/** 操作技能试题草稿 VO */
public record SkillDraftVO(
    Long id,
    Long batchId,
    Long blueprintId,
    Long nodeId,
    String questionName,
    java.math.BigDecimal score,
    java.math.BigDecimal weight,
    Integer durationMinutes,
    String examForm,
    String sitePreparation,
    String examineePrepare,
    String requirements,
    String negatives,
    String scoringItems,
    Integer difficulty,
    Integer consistency,
    String aiModel,
    Integer genDurationMs,
    String reviewStatus,
    Integer publishStatus,
    Long publishedTaskId,
    Long publishedDetailId) {

  public static SkillDraftVO from(SkillGenDraftEntity e) {
    return new SkillDraftVO(e.getId(), e.getBatchId(), e.getBlueprintId(), e.getNodeId(),
        e.getQuestionName(), e.getScore(), e.getWeight(), e.getDurationMinutes(), e.getExamForm(),
        e.getSitePreparation(), e.getExamineePrepare(), e.getRequirements(), e.getNegatives(),
        e.getScoringItems(), e.getDifficulty(), e.getConsistency(), e.getAiModel(),
        e.getGenDurationMs(), e.getReviewStatus(), e.getPublishStatus(), e.getPublishedTaskId(),
        e.getPublishedDetailId());
  }
}
