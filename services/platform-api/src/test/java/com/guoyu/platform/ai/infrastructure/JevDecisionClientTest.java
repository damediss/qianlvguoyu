package com.guoyu.platform.ai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.JevDecisionResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JevDecisionClient 单测（方案 §11.1）：HttpServer 桩响应（须 setExecutor，踩坑库 #24），
 * 覆盖正常解析/缺 usage/401/422/429 重试/5xx 超限/前置拦截/零信任结构校验。
 */
class JevDecisionClientTest {

  private HttpServer server;
  private JevDecisionClient client;
  private final AtomicInteger hits = new AtomicInteger();

  @BeforeEach
  void setUp() throws IOException {
    client = new JevDecisionClient(new ObjectMapper());
    client.maxRetries = 1;
    client.retryBackoffMillis = 10;
    hits.set(0);
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/gateway";
  }

  private void startServer(java.util.function.BiConsumer<HttpExchange, AtomicInteger> handler)
      throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    // 踩坑库 #24：HttpServer 并发桩必须 setExecutor，否则默认 executor 行为受限
    server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(2));
    server.createContext("/gateway/typesafe/v1/systemone", exchange -> {
      handler.accept(exchange, hits);
      exchange.close();
    });
    server.start();
  }

  private void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  private Map<String, String> criteria() {
    Map<String, String> c = new LinkedHashMap<>();
    c.put("301", "蒸发器跑碱处理");
    c.put("302", "蒸发站真空调整");
    c.put("4192", "工匠精神培养");
    return c;
  }

  private static final String OK_BODY = """
      {"model":"bocha-jev-v1","answers":{"l3":{"type":"choice",
        "probabilities":{"301":0.7,"302":0.2,"4192":0.1},
        "confidence":0.7,"choice":"301"}},
       "usage":{"input_tokens":70,"output_tokens":0},
       "metadata":{"inference_ms":6.1,"calibrated":true,"checkpoint_status":"trained",
         "checkpoint_id":"bocha-jev-v02",
         "confidence_definition":"maximum candidate probability; not guaranteed correctness"}}
      """;

  /** 正常返回：解析出 choice（=criteria key）、confidence、probabilities、usage、confidence_definition、响应 model */
  @Test
  void okResponseParsesAllFields() throws IOException {
    startServer((ex, n) -> {
      n.incrementAndGet();
      try {
        respond(ex, 200, OK_BODY);
      } catch (IOException ignored) {
        // 桩内忽略
      }
    });

    JevDecisionResult r = client.decide(baseUrl(), "key", "bocha-jev-v1", "【试题】题干",
        "l3", "该试题真正归属的鉴定点", criteria(), 30);

    assertEquals("301", r.choice());
    assertEquals(0.7, r.confidence(), 1e-9);
    assertEquals(3, r.probabilities().size());
    assertEquals(70, r.inputTokens());
    assertEquals(0, r.outputTokens(), "实测输出 token 恒 0");
    assertEquals("maximum candidate probability; not guaranteed correctness",
        r.confidenceDefinition());
    assertEquals("bocha-jev-v1", r.responseModel());
    assertFalse(r.usageEstimated());
    assertTrue(r.durationMs() >= 0);
  }

  /** 响应缺 usage → 按字符估算并标记 estimated（防御分支，§11.1） */
  @Test
  void missingUsageFallsBackToEstimate() throws IOException {
    String body = """
        {"model":"bocha-jev-v1","answers":{"l3":{"probabilities":{"301":0.7,"302":0.3},
          "confidence":0.7,"choice":"301"}}}
        """;
    startServer((ex, n) -> {
      n.incrementAndGet();
      try {
        respond(ex, 200, body);
      } catch (IOException ignored) {
        // 桩内忽略
      }
    });

    JevDecisionResult r = client.decide(baseUrl(), "key", "bocha-jev-v1", "题干文本",
        "l3", "instr", criteria(), 30);

    assertTrue(r.usageEstimated());
    assertTrue(r.inputTokens() > 0);
  }

  /** 401 → JevUnauthorizedException（不重试） */
  @Test
  void unauthorizedMapsToDedicatedException() throws IOException {
    startServer((ex, n) -> {
      n.incrementAndGet();
      try {
        respond(ex, 401, "{\"code\":\"unauthorized\",\"message\":\"API 密钥不存在\"}");
      } catch (IOException ignored) {
        // 桩内忽略
      }
    });

    assertThrows(JevUnauthorizedException.class, () -> client.decide(baseUrl(), "bad-key",
        "bocha-jev-v1", "state", "l3", "instr", criteria(), 30));
    assertEquals(1, hits.get(), "401 不得重试");
  }

  /** 422 → JevValidationException（不重试） */
  @Test
  void validationErrorMapsToDedicatedException() throws IOException {
    startServer((ex, n) -> {
      n.incrementAndGet();
      try {
        respond(ex, 422, "{\"detail\":[{\"type\":\"too_long\",\"loc\":[\"body\",\"questions\"]}]}");
      } catch (IOException ignored) {
        // 桩内忽略
      }
    });

    assertThrows(JevValidationException.class, () -> client.decide(baseUrl(), "key",
        "bocha-jev-v1", "state", "l3", "instr", criteria(), 30));
    assertEquals(1, hits.get(), "422 不得重试");
  }

  /** 429 → 按 Retry-After 退避重试，第二次 200 成功 */
  @Test
  void rateLimitRetriesThenSucceeds() throws IOException {
    startServer((ex, n) -> {
      int hit = n.incrementAndGet();
      try {
        if (hit == 1) {
          ex.getResponseHeaders().set("Retry-After", "0");
          respond(ex, 429, "{\"detail\":\"rate limited\"}");
        } else {
          respond(ex, 200, OK_BODY);
        }
      } catch (IOException ignored) {
        // 桩内忽略
      }
    });

    JevDecisionResult r = client.decide(baseUrl(), "key", "bocha-jev-v1", "state", "l3",
        "instr", criteria(), 30);

    assertEquals("301", r.choice());
    assertEquals(2, hits.get());
  }

  /** 500 持续 → 重试超限后 JevServerException */
  @Test
  void serverErrorExhaustsRetries() throws IOException {
    startServer((ex, n) -> {
      n.incrementAndGet();
      try {
        respond(ex, 500, "{\"error\":\"boom\"}");
      } catch (IOException ignored) {
        // 桩内忽略
      }
    });

    assertThrows(JevServerException.class, () -> client.decide(baseUrl(), "key", "bocha-jev-v1",
        "state", "l3", "instr", criteria(), 30));
    assertEquals(2, hits.get(), "首次 + maxRetries=1 次重试");
  }

  /** 读超时（readTimeoutSeconds 生效）→ JevServerException，行降级 LLM（§11.1 超时项） */
  @Test
  void readTimeoutMapsToServerException() throws IOException {
    client.maxRetries = 0;
    startServer((ex, n) -> {
      n.incrementAndGet();
      try {
        Thread.sleep(3000);
        respond(ex, 200, OK_BODY);
      } catch (Exception ignored) {
        // 桩内忽略（客户端已超时断开）
      }
    });

    assertThrows(JevServerException.class, () -> client.decide(baseUrl(), "key", "bocha-jev-v1",
        "state", "l3", "instr", criteria(), 1));
    assertEquals(1, hits.get(), "maxRetries=0 时仅 1 次调用");
  }

  /** criteria < 2 → 客户端前置拦截（实测 min_length=2），不发 HTTP 请求 */
  @Test
  void fewerThanTwoCriteriaRejectedWithoutHttpCall() throws IOException {
    startServer((ex, n) -> {
      n.incrementAndGet();
      try {
        respond(ex, 200, OK_BODY);
      } catch (IOException ignored) {
        // 桩内忽略
      }
    });
    Map<String, String> single = new LinkedHashMap<>();
    single.put("301", "蒸发器跑碱处理");

    assertThrows(JevValidationException.class, () -> client.decide(baseUrl(), "key",
        "bocha-jev-v1", "state", "l3", "instr", single, 30));
    assertThrows(JevValidationException.class, () -> client.decide(baseUrl(), "key",
        "bocha-jev-v1", "state", "l3", "instr", null, 30));
    assertEquals(0, hits.get(), "前置拦截不得发起 HTTP 请求");
  }

  /** HTTP 200 但 body 非法 JSON → 重试后 JevServerException */
  @Test
  void malformedBodyTreatedAsServerFailure() throws IOException {
    startServer((ex, n) -> {
      n.incrementAndGet();
      try {
        respond(ex, 200, "这不是 JSON");
      } catch (IOException ignored) {
        // 桩内忽略
      }
    });

    assertThrows(JevServerException.class, () -> client.decide(baseUrl(), "key", "bocha-jev-v1",
        "state", "l3", "instr", criteria(), 30));
    assertEquals(2, hits.get());
  }

  /** 零信任：answers 缺失 / confidence 非数值 / probabilities 缺失 → JevParseException */
  @Test
  void structuralDefectsFailWithoutGuessing() throws IOException {
    String missingAnswers = "{\"usage\":{\"input_tokens\":1,\"output_tokens\":0}}";
    String nonNumericConf = """
        {"answers":{"l3":{"probabilities":{"301":1.0},"confidence":"high","choice":"301"}}}
        """;
    String missingProbs = """
        {"answers":{"l3":{"confidence":0.7,"choice":"301"}},
         "usage":{"input_tokens":1,"output_tokens":0}}
        """;
    for (String body : new String[] {missingAnswers, nonNumericConf, missingProbs}) {
      startServer((ex, n) -> {
        n.incrementAndGet();
        try {
          respond(ex, 200, body);
        } catch (IOException ignored) {
          // 桩内忽略
        }
      });
      assertThrows(JevParseException.class, () -> client.decide(baseUrl(), "key", "bocha-jev-v1",
          "state", "l3", "instr", criteria(), 30), "body=" + body);
      server.stop(0);
      server = null;
      hits.set(0);
    }
  }
}
