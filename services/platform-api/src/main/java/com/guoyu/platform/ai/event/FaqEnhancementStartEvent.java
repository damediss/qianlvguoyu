package com.guoyu.platform.ai.event;

/**
 * FAQ LLM 异步增强启动事件。
 *
 * <p>由 {@code submitConversion} 或 {@code retryFailed} 在事务内发布，
 * 监听器在事务成功提交后（{@code AFTER_COMMIT}）启动异步 LLM 处理，
 * 避免事务未提交时异步线程查询不到 pending 记录。
 *
 * @param batchId   批次 ID
 * @param tenantId  租户 ID（从 HTTP 线程捕获后传递，异步链不调用 SecurityContext）
 * @param userId    操作用户 ID
 * @param modelCode 模型编码
 */
public record FaqEnhancementStartEvent(Long batchId, Long tenantId, Long userId, String modelCode) {
}
