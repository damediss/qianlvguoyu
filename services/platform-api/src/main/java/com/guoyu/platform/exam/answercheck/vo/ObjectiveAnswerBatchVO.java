package com.guoyu.platform.exam.answercheck.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 批次（方案 §13.2）。所有雪花 ID 必须按字符串下发，避免前端精度丢失。 */
public record ObjectiveAnswerBatchVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    String status,
    String selectionMode,
    String filterSummary,
    int total,
    int done,
    int pending,
    int processing,
    int consistentHigh,
    int mismatchHigh,
    int lowConfidence,
    int invalid,
    int failed,
    int contentSuspect,
    int contentInvalid,
    int expectedCalls,
    int callCount,
    long inputTokensExact,
    long inputTokensEstimated,
    int usageEstimatedCalls,
    long outputTokens,
    String modelId,
    String promptVersion,
    BigDecimal confidenceThreshold,
    BigDecimal marginThreshold,
    @JsonSerialize(using = ToStringSerializer.class) Long createdBy,
    String createdByName,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    String failReason,
    boolean canPause,
    boolean canResume,
    boolean canRetryFailed) {
}
