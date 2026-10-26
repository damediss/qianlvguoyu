package com.guoyu.platform.competition.integration.plantvr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlantVrCompetitionClient {
  private final PlantVrCompetitionProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public PlantVrCompetitionClient(
      PlantVrCompetitionProperties properties,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getConnectTimeoutSeconds())))
        .build();
  }

  public List<ScoreAttempt> fetchAllAttempts(String hostTenantId, String sessionId) {
    requireConfigured();
    int pageSize = Math.min(Math.max(properties.getPageSize(), 1), 200);
    int offset = 0;
    List<ScoreAttempt> attempts = new ArrayList<>();
    while (true) {
      ExportPage page = fetchPage(hostTenantId, sessionId, offset, pageSize);
      if (page.session() == null || !sessionId.equals(page.session().id())) {
        throw new BusinessException(ErrorCode.CONFLICT, "PlantVR返回的竞赛场次与绑定不一致");
      }
      if (!hostTenantId.equals(page.session().hostTenantId())) {
        throw new BusinessException(ErrorCode.CONFLICT, "PlantVR返回的主办企业与绑定不一致");
      }
      if (!page.scoresAvailable()) {
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "PlantVR仿真成绩服务暂不可用");
      }
      if (page.attempts() != null) attempts.addAll(page.attempts());
      if (page.page() == null || !page.page().hasMore()
          || page.page().returned() == null || page.page().returned() <= 0) break;
      offset += page.page().returned();
      if (offset > 100000) {
        throw new BusinessException(ErrorCode.CONFLICT, "PlantVR成绩分页游标异常");
      }
    }
    return attempts;
  }

  public List<SessionInfo> fetchAllSessions() {
    requireConfigured();
    int pageSize = Math.min(Math.max(properties.getPageSize(), 1), 200);
    int offset = 0;
    List<SessionInfo> sessions = new ArrayList<>();
    while (true) {
      SessionDirectoryPage page = fetchSessionPage(offset, pageSize);
      if (page.items() != null) sessions.addAll(page.items());
      if (page.page() == null || !page.page().hasMore()
          || page.page().returned() == null || page.page().returned() <= 0) break;
      offset += page.page().returned();
      if (offset > 100000) {
        throw new BusinessException(ErrorCode.CONFLICT, "PlantVR场次目录分页游标异常");
      }
    }
    return sessions;
  }

  private SessionDirectoryPage fetchSessionPage(int offset, int limit) {
    String base = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
    String url = base.replaceAll("/+$", "")
        + "/internal/integrations/guoyu-cloud/competition-sessions?offset=" + offset
        + "&limit=" + limit;
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
        .header("Accept", "application/json")
        .header("X-Internal-Secret", properties.getInternalSecret().trim())
        .GET()
        .build();
    try {
      HttpResponse<String> response = httpClient.send(
          request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
            "PlantVR场次目录接口调用失败，HTTP " + response.statusCode());
      }
      SessionDirectoryPage result = objectMapper.readValue(
          response.body(), SessionDirectoryPage.class);
      if (result == null) {
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "PlantVR场次目录接口返回空响应");
      }
      return result;
    } catch (BusinessException ex) {
      throw ex;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "PlantVR场次目录查询被中断");
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
          "PlantVR场次目录接口不可用：" + safeMessage(ex));
    }
  }

  private ExportPage fetchPage(String hostTenantId, String sessionId, int offset, int limit) {
    String base = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
    String url = base.replaceAll("/+$", "")
        + "/internal/integrations/guoyu-cloud/competition-sessions/"
        + encode(sessionId) + "/scores?hostTenantId=" + encode(hostTenantId)
        + "&offset=" + offset + "&limit=" + limit;
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
        .header("Accept", "application/json")
        .header("X-Internal-Secret", properties.getInternalSecret().trim())
        .GET()
        .build();
    try {
      HttpResponse<String> response = httpClient.send(
          request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
            "PlantVR成绩接口调用失败，HTTP " + response.statusCode());
      }
      ExportPage result = objectMapper.readValue(response.body(), ExportPage.class);
      if (result == null) {
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "PlantVR成绩接口返回空响应");
      }
      return result;
    } catch (BusinessException ex) {
      throw ex;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "PlantVR成绩同步被中断");
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
          "PlantVR成绩接口不可用：" + safeMessage(ex));
    }
  }

  private void requireConfigured() {
    if (!properties.isEnabled()) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "PlantVR成绩对接未启用");
    }
    if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()
        || properties.getInternalSecret() == null || properties.getInternalSecret().isBlank()) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "PlantVR成绩对接配置不完整");
    }
  }

  private String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private String safeMessage(Exception ex) {
    String message = ex.getMessage();
    if (message == null || message.isBlank()) return ex.getClass().getSimpleName();
    return message.length() <= 200 ? message : message.substring(0, 200);
  }

  public record ExportPage(
      SessionInfo session,
      List<Participant> participants,
      List<ScoreAttempt> attempts,
      PageInfo page,
      boolean scoresAvailable) {}

  public record SessionDirectoryPage(List<SessionInfo> items, PageInfo page) {}

  public record SessionInfo(
      String id,
      String name,
      String hostTenantId,
      String hostTenantName,
      String status,
      OffsetDateTime startsAt,
      OffsetDateTime endsAt) {}

  public record Participant(
      String competitionParticipantId,
      String realName,
      String idCard,
      String competitionOrgId,
      String competitionOrgName) {}

  public record ScoreAttempt(
      String competitionParticipantId,
      String realName,
      String idCard,
      BigDecimal score,
      Integer totalDuration,
      OffsetDateTime recordedAt,
      String vrSessionId,
      String moduleTitle,
      String appDisplayName,
      boolean hasScoreDetail,
      JsonNode scoreDetail) {}

  public record PageInfo(Integer offset, Integer limit, Integer returned, boolean hasMore) {}
}
