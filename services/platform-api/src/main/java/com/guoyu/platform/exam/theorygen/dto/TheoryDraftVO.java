package com.guoyu.platform.exam.theorygen.dto;

import com.guoyu.platform.exam.theorygen.entity.TheoryGenDraftEntity;

/** 草稿 VO */
public record TheoryDraftVO(
    Long id,
    Long batchId,
    Long blueprintId,
    Long nodeId,
    Long knowledgeId,
    String questionType,
    String typeCode,
    Integer difficulty,
    Integer consistency,
    String content,
    String options,
    String answer,
    String analysis,
    String referenceAnswer,
    String scorePoints,
    String scoringStandard,
    java.math.BigDecimal score,
    String aiModel,
    Integer genDurationMs,
    String reviewStatus,
    Integer publishStatus,
    String publishedTaskType,
    Long publishedTaskId,
    Long publishedDetailId) {

  public static TheoryDraftVO from(TheoryGenDraftEntity e) {
    return new TheoryDraftVO(e.getId(), e.getBatchId(), e.getBlueprintId(), e.getNodeId(),
        e.getKnowledgeId(), e.getQuestionType(), e.getTypeCode(), e.getDifficulty(),
        e.getConsistency(), e.getContent(), e.getOptions(), e.getAnswer(), e.getAnalysis(),
        e.getReferenceAnswer(), e.getScorePoints(), e.getScoringStandard(), e.getScore(),
        e.getAiModel(), e.getGenDurationMs(), e.getReviewStatus(), e.getPublishStatus(),
        e.getPublishedTaskType(), e.getPublishedTaskId(), e.getPublishedDetailId());
  }
}
