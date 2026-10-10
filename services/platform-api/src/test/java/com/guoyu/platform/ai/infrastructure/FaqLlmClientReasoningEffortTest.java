package com.guoyu.platform.ai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * reasoning_effort 请求体组装回归（2026-08-11，DeepSeek 官方文档口径）：
 * reasoning_effort 是 thinking 对象内字段，仅 type=enabled 生效；仅 DeepSeek 发送，
 * LongCat 维持只发 {type}，其它模型不发 thinking 节点。
 *
 * <p>用本地 HttpServer 捕获实际发出的请求体断言，不依赖真实 LLM。
 */
class FaqLlmClientReasoningEffortTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String CANNED_RESPONSE =
      "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"ok\"}}]}";

  private HttpServer server;
  private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
  private String deepSeekBase;
  private String longcatBase;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] body = CANNED_RESPONSE.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(body);
      }
    });
    server.start();
    int port = server.getAddress().getPort();
    deepSeekBase = "http://127.0.0.1:" + port + "/deepseek";
    longcatBase = "http://127.0.0.1:" + port + "/longcat";
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private JsonNode lastThinkingNode() throws Exception {
    JsonNode body = MAPPER.readTree(lastRequestBody.get());
    return body.path("thinking");
  }

  @Test
  void deepSeekEnabledSendsReasoningEffort() throws Exception {
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    String content = client.chatCompletion(deepSeekBase, "k", "deepseek-v4-flash",
        null, "prompt", 100, "enabled", "low", false, 1L);
    assertEquals("ok", content);
    JsonNode thinking = lastThinkingNode();
    assertTrue(thinking.isObject());
    assertEquals("enabled", thinking.path("type").asText());
    assertEquals("low", thinking.path("reasoning_effort").asText());
  }

  @Test
  void deepSeekDisabledOmitsReasoningEffort() throws Exception {
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    client.chatCompletion(deepSeekBase, "k", "deepseek-v4-flash",
        null, "prompt", 100, "disabled", "max", false, 1L);
    JsonNode thinking = lastThinkingNode();
    assertTrue(thinking.isObject());
    assertEquals("disabled", thinking.path("type").asText());
    assertFalse(thinking.has("reasoning_effort"), "disabled 时不得带 reasoning_effort");
  }

  @Test
  void deepSeekBlankThinkingOmitsWholeNode() throws Exception {
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    client.chatCompletion(deepSeekBase, "k", "deepseek-v4-flash",
        null, "prompt", 100, null, "high", false, 1L);
    assertTrue(lastThinkingNode().isMissingNode(), "thinking 为空时整个节点不发");
  }

  @Test
  void longcatNeverSendsReasoningEffort() throws Exception {
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    client.chatCompletion(longcatBase, "k", "longcat-flash",
        null, "prompt", 100, "enabled", "low", false, 1L);
    JsonNode thinking = lastThinkingNode();
    assertTrue(thinking.isObject());
    assertEquals("enabled", thinking.path("type").asText());
    assertFalse(thinking.has("reasoning_effort"), "LongCat 不支持 reasoning_effort，一律不传");
  }

  @Test
  void messagesModeDeepSeekSendsReasoningEffort() throws Exception {
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    JsonNode messages = MAPPER.readTree("[{\"role\":\"user\",\"content\":\"hi\"}]");
    client.chatCompletionMessages(deepSeekBase, "k", "deepseek-v4-flash",
        messages, 100, BigDecimal.ONE, "enabled", "max", false, 1L);
    JsonNode thinking = lastThinkingNode();
    assertEquals("enabled", thinking.path("type").asText());
    assertEquals("max", thinking.path("reasoning_effort").asText());
  }

  @Test
  void deepSeekEnabledBlankEffortOmitsField() throws Exception {
    // thinking=enabled 但 reasoningEffort 为空：只发 type，由服务端用默认 high
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    client.chatCompletion(deepSeekBase, "k", "deepseek-v4-flash",
        null, "prompt", 100, "enabled", "", false, 1L);
    JsonNode thinking = lastThinkingNode();
    assertEquals("enabled", thinking.path("type").asText());
    assertFalse(thinking.has("reasoning_effort"));
  }
}
