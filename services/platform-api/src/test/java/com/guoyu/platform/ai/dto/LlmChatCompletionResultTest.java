package com.guoyu.platform.ai.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** usage 双口径解析零信任单测（优化方案 v0.3 §7 OPT-08/09/10，桩响应不依赖真实模型） */
class LlmChatCompletionResultTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private JsonNode json(String s) throws Exception {
    return objectMapper.readTree(s);
  }

  /** OPT-08：智谱 GLM 口径 usage.prompt_tokens_details.cached_tokens */
  @Test
  void glmCachedTokens() throws Exception {
    JsonNode response = json("""
        {"choices":[{"message":{"content":"ok"}}],
         "usage":{"prompt_tokens":100,"completion_tokens":50,"total_tokens":150,
                  "prompt_tokens_details":{"cached_tokens":80}}}
        """);
    LlmChatCompletionResult r = LlmChatCompletionResult.from(response, "ok");
    assertEquals(100, r.promptTokens());
    assertEquals(50, r.completionTokens());
    assertEquals(150, r.totalTokens());
    assertEquals(80, r.cachedTokens());
    assertEquals("ok", r.content());
  }

  /** OPT-09：DeepSeek 口径 usage.prompt_cache_hit_tokens */
  @Test
  void deepSeekCachedTokens() throws Exception {
    JsonNode response = json("""
        {"choices":[{"message":{"content":"ok"}}],
         "usage":{"prompt_tokens":100,"completion_tokens":50,"total_tokens":150,
                  "prompt_cache_hit_tokens":60}}
        """);
    LlmChatCompletionResult r = LlmChatCompletionResult.from(response, "ok");
    assertEquals(60, r.cachedTokens());
    assertEquals(150, r.totalTokens());
  }

  /** OPT-10：usage 缺失/类型异常一律按 0 记，不抛异常，content 正常携带 */
  @Test
  void missingUsageTolerated() throws Exception {
    JsonNode noUsage = json("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
    LlmChatCompletionResult r1 = LlmChatCompletionResult.from(noUsage, "ok");
    assertEquals(0, r1.promptTokens());
    assertEquals(0, r1.completionTokens());
    assertEquals(0, r1.cachedTokens());

    JsonNode badTypes = json("""
        {"usage":{"prompt_tokens":"abc","completion_tokens":null,"total_tokens":false}}
        """);
    LlmChatCompletionResult r2 = LlmChatCompletionResult.from(badTypes, "ok");
    assertEquals(0, r2.promptTokens());
    assertEquals(0, r2.completionTokens());
    assertEquals(0, r2.totalTokens());

    LlmChatCompletionResult r3 = LlmChatCompletionResult.from(null, "ok");
    assertEquals(0, r3.promptTokens());
  }

  /** total_tokens 缺失时按 prompt+completion 兜底 */
  @Test
  void totalTokensFallback() throws Exception {
    JsonNode response = json("""
        {"usage":{"prompt_tokens":30,"completion_tokens":12}}
        """);
    LlmChatCompletionResult r = LlmChatCompletionResult.from(response, "ok");
    assertEquals(42, r.totalTokens());
  }
}
