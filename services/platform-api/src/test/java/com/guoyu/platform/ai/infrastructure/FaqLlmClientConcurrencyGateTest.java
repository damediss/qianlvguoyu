package com.guoyu.platform.ai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * glm-5.3-flash 单模型并发闸口（方案 A，2026-09-08 需求方确认口径）：
 * bigmodel 域 + modelId=glm-5.3-flash 的调用受公平信号量封顶，超出排队等待、不丢不拒；
 * 同账户其它 GLM 模型与非 bigmodel 平台不受限。
 *
 * <p>本地 HttpServer 桩记录同时在飞请求数峰值，不依赖真实 LLM。
 * 注意：HttpServer 必须显式 setExecutor，否则默认单线程串行处理请求，测不出并发。
 */
class FaqLlmClientConcurrencyGateTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String CANNED_RESPONSE =
      "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"ok\"}}]}";

  private HttpServer server;
  /** 桩端当前在飞请求数与峰值 */
  private final AtomicInteger inFlight = new AtomicInteger();
  private final AtomicInteger peak = new AtomicInteger();
  private String glmBase;
  private ExecutorService serverExecutor;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    serverExecutor = Executors.newFixedThreadPool(8);
    server.setExecutor(serverExecutor);
    server.createContext("/", exchange -> {
      int now = inFlight.incrementAndGet();
      peak.accumulateAndGet(now, Math::max);
      try {
        Thread.sleep(150); // 拉长单调用时长，确保并发重叠可观测
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        inFlight.decrementAndGet();
      }
      byte[] body = CANNED_RESPONSE.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(body);
      }
    });
    server.start();
    // 路径带 /bigmodel 使 isGlm(baseUrl) 命中（桩仍走本机端口）
    glmBase = "http://127.0.0.1:" + server.getAddress().getPort() + "/bigmodel";
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
    serverExecutor.shutdownNow();
  }

  /** 直接设闸口上限字段，模拟 Spring @Value 注入（信号量首次使用时才按该值创建） */
  private FaqLlmClient clientWithGate(int permits) {
    FaqLlmClient client = new FaqLlmClient(MAPPER);
    ReflectionTestUtils.setField(client, "glmMaxConcurrency", permits);
    return client;
  }

  @Test
  void gatedModelCapsConcurrentCallsAndQueuesRest() throws Exception {
    FaqLlmClient client = clientWithGate(2);
    ExecutorService pool = Executors.newFixedThreadPool(6);
    try {
      List<Future<String>> futures = new ArrayList<>();
      for (int i = 0; i < 6; i++) {
        final int idx = i;
        futures.add(pool.submit(() ->
            client.chatCompletion(glmBase, "k", "glm-5.3-flash",
                null, "p" + idx, 100, null, null, false, 1L)));
      }
      for (Future<String> f : futures) {
        assertEquals("ok", f.get(30, TimeUnit.SECONDS), "排队调用最终都应成功，不丢不拒");
      }
    } finally {
      pool.shutdownNow();
    }
    assertTrue(peak.get() <= 2, "在飞调用不得超过闸口上限，实测峰值=" + peak.get());
    assertEquals(2, peak.get(), "6 个调用应实际跑满 2 并发（证明排队后继续执行）");
  }

  @Test
  void otherGlmModelNotGated() throws Exception {
    FaqLlmClient client = clientWithGate(1);
    assertNull(client.gateFor(glmBase, "glm-4.7"),
        "同账户其它 GLM 模型不占 glm-5.3-flash 额度");
    assertNull(client.gateFor("https://api.deepseek.com", "glm-5.3-flash"),
        "非 bigmodel 域不套闸口");
    assertNull(client.gateFor(glmBase, null), "modelId 缺失不套闸口");
  }

  @Test
  void gateSelectionTrimsAndIgnoresCase() {
    FaqLlmClient client = clientWithGate(50);
    assertNotNull(client.gateFor(glmBase, "GLM-5.3-Flash"), "modelId 大小写不敏感");
    assertNotNull(client.gateFor(glmBase, " glm-5.3-flash "), "modelId 首尾空白容忍");
    assertEquals(50, client.gateFor(glmBase, "glm-5.3-flash").availablePermits(),
        "闸口名额 = 配置上限（默认 50）");
  }
}
