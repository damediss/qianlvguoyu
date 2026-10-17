package com.guoyu.platform.ai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 压缩协商回归（2026-10-17 DeepSeek Brotli 事故）：
 * 类路径含 Tika 传递依赖 org.brotli:dec 0.1.2 时，HttpClient 会自动广告 br；
 * 该版本无参 read() 读 1 字节后流即耗尽，而 Spring 转换前会先 read() 一字节探测空体，
 * 导致 br 响应只解出 "{"。FaqLlmClient 必须禁用内容压缩，请求头不得带 Accept-Encoding。
 */
class FaqLlmClientCompressionTest {

  @Test
  @DisplayName("LLM 请求不协商内容压缩（不得携带 Accept-Encoding/br）")
  void doesNotAdvertiseContentCompression() throws Exception {
    AtomicReference<String> acceptEncoding = new AtomicReference<>("<absent>");
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/chat/completions", exchange -> {
      String header = exchange.getRequestHeaders().getFirst("Accept-Encoding");
      acceptEncoding.set(header == null ? "<absent>" : header);
      byte[] body = ("{\"choices\":[{\"finish_reason\":\"stop\","
          + "\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}],\"usage\":{}}")
          .getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.setExecutor(Runnable::run);
    server.start();
    try {
      FaqLlmClient client = new FaqLlmClient(new ObjectMapper());
      String content = client.chatCompletion(
          "http://127.0.0.1:" + server.getAddress().getPort(),
          "test-key", "deepseek-flash", "只回复ok", 32, null, 10159L);
      assertEquals("ok", content);
      assertEquals("<absent>", acceptEncoding.get(),
          "禁用内容压缩后不应发送 Accept-Encoding；出现 br 会触发 org.brotli:dec 0.1.2 单字节读截断");
    } finally {
      server.stop(0);
    }
  }
}
