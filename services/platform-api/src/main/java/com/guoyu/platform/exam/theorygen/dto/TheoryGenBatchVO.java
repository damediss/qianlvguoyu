package com.guoyu.platform.exam.theorygen.dto;

import com.guoyu.platform.exam.theorygen.entity.TheoryGenBatchEntity;

/**
 * 生成批次 VO（进度轮询 / 工作台总览返回体）。
 *
 * <p>审核进度/入库/主观题计数由批次分页聚合查询填充（工作台总览表列）；
 * 进度轮询接口不需要这些统计，{@link #from(TheoryGenBatchEntity)} 以零值填充。
 */
public record TheoryGenBatchVO(
    Long id,
    Long blueprintId,
    Integer workTypeId,
    Integer level,
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
    java.time.LocalDateTime createdAt,
    java.time.LocalDateTime startedAt,
    java.time.LocalDateTime finishedAt,
    Integer approvedCount,
    Integer pendingReviewCount,
    Integer rejectedCount,
    Integer publishedCount,
    Integer subjectiveCount) {

  /** 草稿统计（按批次聚合，工作台总览表用） */
  public record DraftStats(int approved, int pendingReview, int rejected, int published,
      int subjective) {
    public static final DraftStats EMPTY = new DraftStats(0, 0, 0, 0, 0);
  }

  public static TheoryGenBatchVO from(TheoryGenBatchEntity e) {
    return from(e, DraftStats.EMPTY);
  }

  public static TheoryGenBatchVO from(TheoryGenBatchEntity e, DraftStats s) {
    DraftStats stats = s == null ? DraftStats.EMPTY : s;
    return new TheoryGenBatchVO(e.getId(), e.getBlueprintId(), e.getWorkTypeId(), e.getLevel(),
        e.getModelId(), e.getThinking(), e.getReasoningEffort(), e.getQuotaConfig(), e.getStatus(),
        e.getTotalPoints(), e.getDonePoints(), e.getFailedPoints(), e.getTotalQuestions(),
        e.getTokenUsage(), e.getFailReason(), e.getCreatedAt(), e.getStartedAt(), e.getFinishedAt(),
        stats.approved(), stats.pendingReview(), stats.rejected(), stats.published(),
        stats.subjective());
  }
}
