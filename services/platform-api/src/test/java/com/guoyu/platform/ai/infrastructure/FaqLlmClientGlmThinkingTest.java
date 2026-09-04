package com.guoyu.platform.ai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * GLM（bigmodel.cn）thinking 分支回归（方案 v1.2 / C4，2026-09-04）：
 * thinking=enabled → {type:enabled} + 顶层 reasoning_effort；
 * thinking=disabled → 仅 {type:disabled}，不带 reasoning_effort；thinking 为空 → 整个节点不发。
 *
 * <p>本地 HttpServer 捕获实际请求体断言，不依赖真实 LLM。
 */
class FaqLlmClientGlmThinkingTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String CANNED_RESPONSE =
      "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"ok\"}}]}";

  private HttpServer server;
  private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
  private String glmBase;

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
    glmBase = "http://127.0.0.1:" + server.getAddress().getPort() + "/bigmodel";
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private JsonNode lastBody() throws Exception {
    return MAPPER.readTree(lastRequestBody.get());
  }

  @Test
  void glmEnabledSendsTopLevelReasoningEffort() throws Exception {
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    String content = client.chatCompletion(glmBase, "k", "GLM-5.3-Flash",
        null, "prompt", 100, "enabled", "low", false, 1L);
    assertEquals("ok", content);
    JsonNode body = lastBody();
    assertEquals("enabled", body.path("thinking").path("type").asText());
    assertEquals("low", body.path("reasoning_effort").asText(),
        "GLM 口径：reasoning_effort 为顶层字段");
  }

  @Test
  void glmDisabledOmitsReasoningEffort() throws Exception {
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    client.chatCompletion(glmBase, "k", "GLM-5.3-Flash",
        null, "prompt", 100, "disabled", "low", false, 1L);
    JsonNode body = lastBody();
    assertEquals("disabled", body.path("thinking").path("type").asText());
    assertFalse(body.has("reasoning_effort"), "disabled 时不发顶层 reasoning_effort");
  }

  @Test
  void glmBlankThinkingOmitsWholeNode() throws Exception {
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    client.chatCompletion(glmBase, "k", "GLM-5.3-Flash",
        null, "prompt", 100, null, "low", false, 1L);
    JsonNode body = lastBody();
    assertTrue(body.path("thinking").isMissingNode(), "thinking 为空时整个节点不发");
    assertFalse(body.has("reasoning_effort"));
  }

  @Test
  void glmNeverSendsResponseFormat() throws Exception {
    // 坑 #13：OpenAI 兼容 ≠ 完全兼容；理论生成链路不启用 response_format
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    client.chatCompletion(glmBase, "k", "GLM-5.3-Flash",
        null, "prompt", 100, "enabled", "low", false, 1L);
    assertFalse(lastBody().has("response_format"));
  }
}
