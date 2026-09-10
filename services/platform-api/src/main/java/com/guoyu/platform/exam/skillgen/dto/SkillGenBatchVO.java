package com.guoyu.platform.exam.skillgen.dto;

import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;

/**
 * 生成批次 VO（进度轮询 / 工作台总览返回体）。
 * 审核进度/入库计数由批次分页聚合查询填充；进度轮询接口以零值填充。
 */
public record SkillGenBatchVO(
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
    Integer publishedCount) {

  /** 草稿统计（按批次聚合，工作台总览表用） */
  public record DraftStats(int approved, int pendingReview, int rejected, int published) {
    public static final DraftStats EMPTY = new DraftStats(0, 0, 0, 0);
  }

  public static SkillGenBatchVO from(SkillGenBatchEntity e) {
    return from(e, DraftStats.EMPTY);
  }

  public static SkillGenBatchVO from(SkillGenBatchEntity e, DraftStats s) {
    DraftStats stats = s == null ? DraftStats.EMPTY : s;
    return new SkillGenBatchVO(e.getId(), e.getBlueprintId(), e.getWorkTypeId(), e.getLevel(),
        e.getModelId(), e.getThinking(), e.getReasoningEffort(), e.getQuotaConfig(), e.getStatus(),
        e.getTotalPoints(), e.getDonePoints(), e.getFailedPoints(), e.getTotalQuestions(),
        e.getTokenUsage(), e.getFailReason(), e.getCreatedAt(), e.getStartedAt(), e.getFinishedAt(),
        stats.approved(), stats.pendingReview(), stats.rejected(), stats.published());
  }
}
