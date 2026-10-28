package com.guoyu.platform.ai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.JevBatchDecisionResult;
import com.guoyu.platform.ai.dto.JevQuestion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 多子问题客户端（方案 §9.4 / §16.1-F）：响应零信任、Brotli 防护、预算 422 分类。
 */
class JevDecisionClientBatchTest {

  private HttpServer server;
  private JevDecisionClient client;
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private final AtomicReference<String> acceptEncoding = new AtomicReference<>();

  @BeforeEach
  void setUp() {
    client = new JevDecisionClient(new ObjectMapper());
    client.maxRetries = 0;
    client.retryBackoffMillis = 0;
    requestBody.set(null);
    acceptEncoding.set(null);
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

  private void startServer(int status, String body) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(2));
    server.createContext("/gateway/typesafe/v1/systemone", exchange -> {
      requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      acceptEncoding.set(exchange.getRequestHeaders().getFirst("Accept-Encoding"));
      respond(exchange, status, body);
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

  private Map<String, JevQuestion> questions() {
    Map<String, JevQuestion> questions = new LinkedHashMap<>();
    questions.put("opt_0", new JevQuestion("判断 A 是否应选", Map.of("Y", "应选", "N", "不应选")));
    questions.put("opt_1", new JevQuestion("判断 B 是否应选", Map.of("Y", "应选", "N", "不应选")));
    return questions;
  }

  private static final String OK_BODY = """
      {"model":"ateve-jev-v1","request_id":"req-1",
       "answers":{
         "opt_0":{"choice":"Y","confidence":0.9,"probabilities":{"Y":0.9,"N":0.1}},
         "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}},
       "usage":{"input_tokens":120,"output_tokens":0},
       "metadata":{"confidence_definition":"maximum candidate probability"}}
      """;

  @Test
  void parsesMultipleSubAnswersWithMarginAndUsage() throws IOException {
    startServer(200, OK_BODY);

    JevBatchDecisionResult result = client.decideBatch(baseUrl(), "key", "ateve-jev-v1", "state",
        questions(), 30);

    assertEquals(2, result.answers().size());
    assertEquals("Y", result.answers().get("opt_0").choice());
    assertEquals(0.8, result.answers().get("opt_0").margin(), 1e-9);
    assertEquals(0.9, result.answers().get("opt_0").top1(), 1e-9);
    assertEquals(0.1, result.answers().get("opt_0").top2(), 1e-9);
    assertEquals(120, result.inputTokens());
    assertFalse(result.usageEstimated());
    assertEquals("ateve-jev-v1", result.responseModel());
    assertEquals("maximum candidate probability", result.confidenceDefinition());
    assertEquals("req-1", result.requestId());
    assertTrue(requestBody.get().contains("opt_0"));
    assertTrue(requestBody.get().contains("\"criteria\""));
    assertFalse(requestBody.get().contains("isCorrect"), "请求不得包含原答案标记");
  }

  @Test
  void requestHasNoAcceptEncodingHeader() throws IOException {
    startServer(200, OK_BODY);
    client.decideBatch(baseUrl(), "key", "ateve-jev-v1", "state", questions(), 30);
    assertNull(acceptEncoding.get(), "禁用内容压缩后不得再发 Accept-Encoding");
  }

  @Test
  void missingSubAnswerFailsWholeBatch() throws IOException {
    startServer(200, """
        {"answers":{"opt_0":{"choice":"Y","confidence":0.9,"probabilities":{"Y":0.9,"N":0.1}}},
         "usage":{"input_tokens":1,"output_tokens":0}}
        """);
    JevParseException e = assertThrows(JevParseException.class,
        () -> client.decideBatch(baseUrl(), "key", "m", "state", questions(), 30));
    assertTrue(e.getMessage().contains("opt_1"));
  }

  @Test
  void choiceOutOfCriteriaFails() throws IOException {
    startServer(200, """
        {"answers":{
          "opt_0":{"choice":"X","confidence":0.9,"probabilities":{"Y":0.9,"N":0.1}},
          "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}},
         "usage":{"input_tokens":1,"output_tokens":0}}
        """);
    assertThrows(JevParseException.class,
        () -> client.decideBatch(baseUrl(), "key", "m", "state", questions(), 30));
  }

  @Test
  void invalidConfidenceOrProbabilitySetFails() throws IOException {
    String[] bodies = {
        // confidence 越界
        """
        {"answers":{
          "opt_0":{"choice":"Y","confidence":1.2,"probabilities":{"Y":0.9,"N":0.1}},
          "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}}}
        """,
        // 概率 key 缺失/多余
        """
        {"answers":{
          "opt_0":{"choice":"Y","confidence":0.9,"probabilities":{"Y":1.0}},
          "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}}}
        """,
        // 概率和异常
        """
        {"answers":{
          "opt_0":{"choice":"Y","confidence":0.9,"probabilities":{"Y":0.5,"N":0.2}},
          "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}}}
        """};
    for (String body : bodies) {
      startServer(200, body);
      assertThrows(JevParseException.class,
          () -> client.decideBatch(baseUrl(), "key", "m", "state", questions(), 30));
      server.stop(0);
      server = null;
    }
  }

  @Test
  void choiceMustBeAmongHighestProbability() throws IOException {
    startServer(200, """
        {"answers":{
          "opt_0":{"choice":"N","confidence":0.9,"probabilities":{"Y":0.9,"N":0.1}},
          "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}},
         "usage":{"input_tokens":1,"output_tokens":0}}
        """);
    JevParseException e = assertThrows(JevParseException.class,
        () -> client.decideBatch(baseUrl(), "key", "m", "state", questions(), 30));
    assertTrue(e.getMessage().contains("最高概率"));
  }

  @Test
  void tiedHighestProducesZeroMargin() throws IOException {
    startServer(200, """
        {"answers":{
          "opt_0":{"choice":"Y","confidence":0.5,"probabilities":{"Y":0.5,"N":0.5}},
          "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}},
         "usage":{"input_tokens":1,"output_tokens":0}}
        """);
    JevBatchDecisionResult result = client.decideBatch(baseUrl(), "key", "m", "state", questions(),
        30);
    assertEquals(0.0, result.answers().get("opt_0").margin(), 1e-9);
  }

  @Test
  void missingModelAndMetadataAreAllowed() throws IOException {
    startServer(200, """
        {"answers":{
          "opt_0":{"choice":"Y","confidence":0.9,"probabilities":{"Y":0.9,"N":0.1}},
          "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}},
         "usage":{"input_tokens":1,"output_tokens":0}}
        """);
    JevBatchDecisionResult result = client.decideBatch(baseUrl(), "key", "m", "state", questions(),
        30);
    assertNull(result.responseModel());
    assertNull(result.confidenceDefinition());
    assertNull(result.requestId());
  }

  @Test
  void missingUsageEstimatesFromFullRequestBody() throws IOException {
    startServer(200, """
        {"answers":{
          "opt_0":{"choice":"Y","confidence":0.9,"probabilities":{"Y":0.9,"N":0.1}},
          "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}}}
        """);
    JevBatchDecisionResult result = client.decideBatch(baseUrl(), "key", "m", "state", questions(),
        30);
    assertTrue(result.usageEstimated());
    assertTrue(result.inputTokens() > 0);
    assertTrue(result.inputTokens() * 3 >= requestBody.get().length() - 10,
        "估算应基于完整请求体");
  }

  @Test
  void negativeOrFractionalTokensRejected() throws IOException {
    String[] bodies = {
        """
        {"answers":{
          "opt_0":{"choice":"Y","confidence":0.9,"probabilities":{"Y":0.9,"N":0.1}},
          "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}},
         "usage":{"input_tokens":-1,"output_tokens":0}}
        """,
        """
        {"answers":{
          "opt_0":{"choice":"Y","confidence":0.9,"probabilities":{"Y":0.9,"N":0.1}},
          "opt_1":{"choice":"N","confidence":0.8,"probabilities":{"Y":0.2,"N":0.8}}},
         "usage":{"input_tokens":1.5,"output_tokens":0}}
        """};
    for (String body : bodies) {
      startServer(200, body);
      assertThrows(JevParseException.class,
          () -> client.decideBatch(baseUrl(), "key", "m", "state", questions(), 30));
      server.stop(0);
      server = null;
    }
  }

  @Test
  void unauthorizedAndBudgetErrorsAreClassified() throws IOException {
    startServer(401, "{\"message\":\"unauthorized\"}");
    assertThrows(JevUnauthorizedException.class,
        () -> client.decideBatch(baseUrl(), "key", "m", "state", questions(), 30));
    server.stop(0);
    server = null;

    startServer(422, "{\"detail\":\"token_budget_exceeded\"}");
    assertThrows(JevBudgetExceededException.class,
        () -> client.decideBatch(baseUrl(), "key", "m", "state", questions(), 30));
    server.stop(0);
    server = null;

    startServer(422, "{\"detail\":[{\"loc\":[\"body\"]}]}");
    assertThrows(JevValidationException.class,
        () -> client.decideBatch(baseUrl(), "key", "m", "state", questions(), 30));
  }

  @Test
  void criteriaBelowTwoRejectedBeforeHttp() {
    Map<String, JevQuestion> bad = new LinkedHashMap<>();
    bad.put("only", new JevQuestion("x", Map.of("Y", "是")));
    assertThrows(JevValidationException.class,
        () -> client.decideBatch("http://127.0.0.1:1/gateway", "key", "m", "state", bad, 1));
    assertNotNull(bad);
  }
}
