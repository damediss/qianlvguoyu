package com.guoyu.platform.ai.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * LLM Chat Completion 结果（content + usage 明细）。
 *
 * <p>usage 双口径归一（并发与成本优化方案 v0.3 P2）：
 * <ul>
 *   <li>智谱 GLM：{@code usage.prompt_tokens_details.cached_tokens}</li>
 *   <li>DeepSeek：{@code usage.prompt_cache_hit_tokens}</li>
 * </ul>
 * 字段缺失或类型异常一律按 0 记，不抛异常（输出零信任）。
 */
public record LlmChatCompletionResult(
    String content,
    long promptTokens,
    long completionTokens,
    long totalTokens,
    long cachedTokens) {

  /** 从完整响应 JsonNode 构建（usage 解析零信任：缺字段按 0 记；response 为 null 时 usage 全 0） */
  public static LlmChatCompletionResult from(JsonNode response, String content) {
    if (response == null) {
      return empty(content);
    }
    JsonNode usage = response.path("usage");
    long prompt = usage.path("prompt_tokens").asLong(0);
    long completion = usage.path("completion_tokens").asLong(0);
    long total = usage.path("total_tokens").asLong(prompt + completion);
    long cached = usage.path("prompt_tokens_details").path("cached_tokens").asLong(0);
    if (cached == 0) {
      // DeepSeek 口径
      cached = usage.path("prompt_cache_hit_tokens").asLong(0);
    }
    return new LlmChatCompletionResult(content, prompt, completion, total, cached);
  }

  /** usage 全 0 的结果（DeepSeek JSON mode 重试后仍为空等场景） */
  public static LlmChatCompletionResult empty(String content) {
    return new LlmChatCompletionResult(content, 0, 0, 0, 0);
  }
}
