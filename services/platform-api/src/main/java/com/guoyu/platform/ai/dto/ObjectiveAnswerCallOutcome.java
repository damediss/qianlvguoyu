package com.guoyu.platform.ai.dto;

/**
 * 单个逻辑块的调用审计（成功或失败都要上报，方案 §6 / §9.3）。
 *
 * @param status      success / failed
 * @param errorCode   失败分类；成功为 null
 * @param previewTail 题干末尾摘要，不含原答案/解析/API Key
 */
public record ObjectiveAnswerCallOutcome(
    String callId,
    Long batchId,
    Long itemId,
    Integer detailId,
    Long runId,
    Integer claimAttempt,
    int chunkIndex,
    int chunkCount,
    String questionType,
    String requestedModel,
    String responseModel,
    String confidenceDefinition,
    String promptSha256,
    String promptPreview,
    Long inputTokens,
    Long outputTokens,
    boolean usageEstimated,
    Integer durationMs,
    String status,
    String errorCode,
    String errorMessage) {

  public static final String STATUS_SUCCESS = "success";
  public static final String STATUS_FAILED = "failed";
}
