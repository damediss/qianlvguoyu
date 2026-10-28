package com.guoyu.platform.ai.dto;

import java.util.Map;

/**
 * 多子问题响应（方案 §9.4）。
 *
 * @param requestId 上游 request id；响应未提供时为 null（不猜字段）
 */
public record JevBatchDecisionResult(
    Map<String, JevSubDecision> answers,
    long inputTokens,
    long outputTokens,
    boolean usageEstimated,
    String responseModel,
    String confidenceDefinition,
    String requestId,
    int durationMs) {
}
