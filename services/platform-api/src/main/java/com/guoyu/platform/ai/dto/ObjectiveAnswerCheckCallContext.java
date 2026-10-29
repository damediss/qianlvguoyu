package com.guoyu.platform.ai.dto;

/**
 * 调用上下文：全部经参数传递，异步链不得读取 SecurityContext（方案 §3 / §11.8）。
 * baseUrl/modelId/readTimeout 必须是批次创建时固化的快照，Worker 禁止重新读 DB 配置。
 */
public record ObjectiveAnswerCheckCallContext(
    Long tenantId,
    Long userId,
    Long batchId,
    Long itemId,
    Long runId,
    int claimAttempt,
    String modelId,
    String jevBaseUrl,
    int jevReadTimeoutSeconds) {
}
