package com.guoyu.platform.exam.knowledgefix.dto;

import java.util.Map;

/**
 * 批次累计 LLM 用量（FIX-10/v1.7）：调用次数与 token 口径，供"费用超预期就暂停"的现场判断。
 * cachedTokens = 命中前缀缓存的输入 token（智谱 prompt_tokens_details.cached_tokens /
 * DeepSeek prompt_cache_hit_tokens，已在 LlmChatCompletionResult 归一）。
 */
public record FixBatchUsageVO(
    long calls,
    long promptTokens,
    long completionTokens,
    long totalTokens,
    long cachedTokens) {

  /** MyBatis 聚合返回 Map（别名即字段名）；空值统一兜底 0 */
  public static FixBatchUsageVO from(Map<String, Object> raw) {
    if (raw == null) {
      return null;
    }
    return new FixBatchUsageVO(num(raw.get("calls")), num(raw.get("promptTokens")),
        num(raw.get("completionTokens")), num(raw.get("totalTokens")),
        num(raw.get("cachedTokens")));
  }

  private static long num(Object v) {
    return v instanceof Number n ? n.longValue() : 0L;
  }
}
