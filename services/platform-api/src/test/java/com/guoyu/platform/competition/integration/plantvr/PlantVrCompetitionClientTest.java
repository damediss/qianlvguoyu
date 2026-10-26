package com.guoyu.platform.competition.integration.plantvr;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PlantVrCompetitionClientTest {

  @Test
  void fetchesEverySessionDirectoryPageAndSendsInternalAuthorizationHeader() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicInteger requestCount = new AtomicInteger();
    AtomicReference<String> receivedSecret = new AtomicReference<>();
    server.createContext(
        "/api/internal/integrations/guoyu-cloud/competition-sessions",
        exchange -> {
          receivedSecret.set(exchange.getRequestHeaders().getFirst("X-Internal-Secret"));
          String query = exchange.getRequestURI().getRawQuery();
          int offset = Integer.parseInt(query.replaceFirst(".*(?:^|&)offset=(\\d+).*", "$1"));
          requestCount.incrementAndGet();
          String sessionId = offset == 0 ? "session-1" : "session-2";
          String body = """
              {
                "items": [{"id":"%s", "name":"测试场次", "hostTenantId":"host-1", "hostTenantName":"测试主办企业", "status":"ended"}],
                "page":{"offset":%d,"limit":1,"returned":1,"hasMore":%s}
              }
              """.formatted(sessionId, offset, offset == 0);
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
          exchange.sendResponseHeaders(200, bytes.length);
          try (var output = exchange.getResponseBody()) {
            output.write(bytes);
          }
        });
    server.start();

    try {
      PlantVrCompetitionProperties properties = new PlantVrCompetitionProperties();
      properties.setEnabled(true);
      properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
      properties.setInternalSecret("test-internal-secret");
      properties.setPageSize(1);
      PlantVrCompetitionClient client = new PlantVrCompetitionClient(properties, new ObjectMapper());

      List<PlantVrCompetitionClient.SessionInfo> sessions = client.fetchAllSessions();

      assertThat(requestCount).hasValue(2);
      assertThat(receivedSecret).hasValue("test-internal-secret");
      assertThat(sessions).extracting(PlantVrCompetitionClient.SessionInfo::id)
          .containsExactly("session-1", "session-2");
      assertThat(sessions).allSatisfy(session -> assertThat(session.hostTenantId()).isEqualTo("host-1"));
      assertThat(sessions).allSatisfy(session ->
          assertThat(session.hostTenantName()).isEqualTo("测试主办企业"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void fetchesEveryPageAndSendsInternalAuthorizationHeader() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicInteger requestCount = new AtomicInteger();
    AtomicReference<String> receivedSecret = new AtomicReference<>();
    server.createContext(
        "/api/internal/integrations/guoyu-cloud/competition-sessions/session-1/scores",
        exchange -> {
          receivedSecret.set(exchange.getRequestHeaders().getFirst("X-Internal-Secret"));
          int offset = Integer.parseInt(exchange.getRequestURI().getRawQuery()
              .replaceFirst(".*(?:^|&)offset=(\\d+).*", "$1"));
          requestCount.incrementAndGet();
          String body = pageJson(offset, offset == 0);
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
          exchange.sendResponseHeaders(200, bytes.length);
          try (var output = exchange.getResponseBody()) {
            output.write(bytes);
          }
        });
    server.start();

    try {
      PlantVrCompetitionProperties properties = new PlantVrCompetitionProperties();
      properties.setEnabled(true);
      properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
      properties.setInternalSecret("test-internal-secret");
      properties.setPageSize(1);
      PlantVrCompetitionClient client = new PlantVrCompetitionClient(properties, new ObjectMapper());

      List<PlantVrCompetitionClient.ScoreAttempt> attempts =
          client.fetchAllAttempts("host-1", "session-1");

      assertThat(requestCount).hasValue(2);
      assertThat(receivedSecret).hasValue("test-internal-secret");
      assertThat(attempts).extracting(PlantVrCompetitionClient.ScoreAttempt::vrSessionId)
          .containsExactly("attempt-0", "attempt-1");
      assertThat(attempts).extracting(PlantVrCompetitionClient.ScoreAttempt::score)
          .containsExactly(new java.math.BigDecimal("88.5"), new java.math.BigDecimal("89.5"));
    } finally {
      server.stop(0);
    }
  }

  private String pageJson(int offset, boolean hasMore) {
    return """
        {
          "session": {"id":"session-1", "name":"test", "hostTenantId":"host-1", "status":"ended"},
          "participants": [],
          "attempts": [{
            "competitionParticipantId":"participant-1",
            "realName":"张玉琦",
            "idCard":"",
            "score":%s,
            "totalDuration":125,
            "vrSessionId":"attempt-%d",
            "moduleTitle":"焦粒焙烧",
            "appDisplayName":"VR",
            "hasScoreDetail":true,
            "scoreDetail":{"totalScore":%s,"lines":[{"key":"Level1_1","score":%s}]}
          }],
          "page":{"offset":%d,"limit":1,"returned":1,"hasMore":%s},
          "scoresAvailable":true
        }
        """.formatted(88.5 + offset, offset, 88.5 + offset, 88.5 + offset,
            offset, hasMore);
  }
}
