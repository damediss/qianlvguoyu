package com.guoyu.platform.exam.theorygen.dto;

import com.guoyu.platform.exam.theorygen.entity.TheoryGenBatchEntity;

/** 生成批次 VO（进度轮询返回体） */
public record TheoryGenBatchVO(
    Long id,
    Long blueprintId,
    String modelId,
    String thinking,
    String reasoningEffort,
    String quotaConfig,
    String status,
    Integer totalPoints,
    Integer donePoints,
    Integer failedPoints,
    Integer totalQuestions,
    Long tokenUsage,
    String failReason,
    java.time.LocalDateTime startedAt,
    java.time.LocalDateTime finishedAt) {

  public static TheoryGenBatchVO from(TheoryGenBatchEntity e) {
    return new TheoryGenBatchVO(e.getId(), e.getBlueprintId(), e.getModelId(), e.getThinking(),
        e.getReasoningEffort(), e.getQuotaConfig(), e.getStatus(), e.getTotalPoints(),
        e.getDonePoints(), e.getFailedPoints(), e.getTotalQuestions(), e.getTokenUsage(),
        e.getFailReason(), e.getStartedAt(), e.getFinishedAt());
  }
}
