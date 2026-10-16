package com.guoyu.platform.ai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 原始响应体校验解析（链路截断留痕加固，2026-10-16 DeepSeek 事故）：
 * 空体 / 截断 JSON / 网关畸形页 → FaqLlmServerException（瞬时）；合法 JSON 正常返回。
 */
class FaqLlmClientResponseBodyTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final FaqLlmClient client = new FaqLlmClient(MAPPER);

  @Test
  @DisplayName("合法 JSON 正常解析返回")
  void parsesValidJson() {
    JsonNode node = client.parseResponseBody("{\"id\":\"x\",\"choices\":[]}", 10159L, "https://api.example.com/chat/completions");
    assertEquals("x", node.get("id").asText());
  }

  @Test
  @DisplayName("null 响应体 → 服务端异常（HTTP 2xx 空体）")
  void nullBodyRejected() {
    FaqLlmServerException ex = assertThrows(FaqLlmServerException.class,
        () -> client.parseResponseBody(null, 10159L, "u"));
    assertEquals("LLM 返回空响应体（HTTP 2xx）", ex.getMessage());
  }

  @Test
  @DisplayName("空白响应体 → 服务端异常")
  void blankBodyRejected() {
    assertThrows(FaqLlmServerException.class,
        () -> client.parseResponseBody("   \n\t ", 10159L, "u"));
  }

  @Test
  @DisplayName("截断 JSON（只吐一个 '{' 即 EOF，10-16 事故形态）→ 服务端异常")
  void truncatedJsonRejected() {
    FaqLlmServerException ex = assertThrows(FaqLlmServerException.class,
        () -> client.parseResponseBody("{\"id\":\"chatcmpl-truncated\",\"object\":\"chat.completion\",\"created\":1", 10159L, "u"));
    assertEquals("LLM 响应体非 JSON（截断或畸形）", ex.getMessage());
  }

  @Test
  @DisplayName("网关畸形页（HTML）→ 服务端异常")
  void htmlBodyRejected() {
    assertThrows(FaqLlmServerException.class,
        () -> client.parseResponseBody("<html><body>Bad Gateway</body></html>", 10159L, "u"));
  }
}
