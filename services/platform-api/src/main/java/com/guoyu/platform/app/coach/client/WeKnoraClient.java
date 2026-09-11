package com.guoyu.platform.app.coach.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.app.coach.config.WeKnoraProperties;
import com.guoyu.platform.app.sms.AppSmsException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** WeKnora HTTP 客户端（会话创建 + 知识库对话 SSE 聚合）。 */
@Component
@Slf4j
public class WeKnoraClient {

  private final WeKnoraProperties properties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final HttpClient httpClient;
  /** 知识库名称 → ID 缓存（小培按名称解析「AI助教_智能问答」等） */
  private final ConcurrentHashMap<String, List<String>> kbNameCache = new ConcurrentHashMap<>();

  public WeKnoraClient(WeKnoraProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.restClient = RestClient.builder()
        .baseUrl(properties.baseUrl())
        .defaultHeader("X-API-Key", properties.apiKey() == null ? "" : properties.apiKey())
        .build();
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
        .build();
  }

  public void ensureConfigured() {
    if (!properties.enabled()) {
      throw new AppSmsException(-1, "AI 陪练服务未启用");
    }
    if (!StringUtils.hasText(properties.apiKey())) {
      throw new AppSmsException(-1, "WeKnora API Key 未配置，请联系管理员");
    }
  }

  /** POST /sessions */
  public String createSession(String title, String description) {
    ensureConfigured();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", StringUtils.hasText(title) ? title : "AI陪练");
    if (StringUtils.hasText(description)) {
      body.put("description", description);
    }
    try {
      JsonNode resp = restClient.post()
          .uri("/sessions")
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(JsonNode.class);
      if (resp == null || !resp.path("success").asBoolean(false)) {
        throw new AppSmsException(-1, "创建 WeKnora 会话失败");
      }
      String sessionId = resp.path("data").path("id").asText(null);
      if (!StringUtils.hasText(sessionId)) {
        throw new AppSmsException(-1, "WeKnora 未返回 sessionId");
      }
      return sessionId;
    } catch (AppSmsException e) {
      throw e;
    } catch (Exception e) {
      log.error("WeKnora createSession failed", e);
      throw new AppSmsException(-1, "连接知识库服务失败：" + e.getMessage());
    }
  }

  /** POST /agent-chat 或 /knowledge-chat，聚合 SSE 为完整回答。 */
  public WeKnoraChatResult knowledgeChat(
      String sessionId,
      String query,
      List<String> knowledgeBaseIds,
      String agentId,
      List<String> imageDataUrls) {
    ensureConfigured();
    if (!StringUtils.hasText(sessionId)) {
      throw new AppSmsException(-1, "sessionId 不能为空");
    }
    if (!StringUtils.hasText(query)) {
      throw new AppSmsException(-1, "问题不能为空");
    }

    List<String> kbIds = knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()
        ? properties.defaultKnowledgeBaseIdList()
        : knowledgeBaseIds;
    String agent = StringUtils.hasText(agentId) ? agentId : properties.defaultAgentId();
    boolean hasImages = imageDataUrls != null && !imageDataUrls.isEmpty();
    // 带图时走 knowledge-chat（VLM 识图 + RAG）；agent-chat 智能推理模式常出现「看不到图片」
    boolean useAgentChat = !hasImages
        && (properties.useAgentChat()
        || (StringUtils.hasText(agent) && !agent.startsWith("builtin-")));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("query", query.trim());
    body.put("channel", "api");
    body.put("disable_title", true);
    if (useAgentChat) {
      body.put("agent_enabled", true);
    }
    if (!kbIds.isEmpty()) {
      body.put("knowledge_base_ids", kbIds);
    }
    if (StringUtils.hasText(agent)) {
      body.put("agent_id", agent);
    }
    if (imageDataUrls != null && !imageDataUrls.isEmpty()) {
      List<Map<String, String>> images = new ArrayList<>();
      for (String dataUrl : imageDataUrls) {
        String normalized = normalizeImageDataUrl(dataUrl);
        if (StringUtils.hasText(normalized)) {
          images.add(Map.of("data", normalized));
        }
      }
      if (!images.isEmpty()) {
        body.put("images", images);
      }
    }

    String path = useAgentChat
        ? "/agent-chat/" + sessionId
        : "/knowledge-chat/" + sessionId;
    String url = properties.baseUrl() + path;
    try {
      String payload = objectMapper.writeValueAsString(body);
      int imageCount = body.get("images") instanceof List<?> list ? list.size() : 0;
      log.info("WeKnora chat path={} agentId={} images={} payloadKB={}",
          path, agent, imageCount, payload.length() / 1024);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(properties.readTimeoutSeconds()))
          .header("Content-Type", "application/json")
          .header("Accept", "text/event-stream")
          .header("X-API-Key", properties.apiKey())
          .POST(HttpRequest.BodyPublishers.ofString(payload))
          .build();

      HttpResponse<java.io.InputStream> response = httpClient.send(
          request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() >= 400) {
        String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
        log.warn("WeKnora knowledge-chat HTTP {}: {}", response.statusCode(), errBody);
        throw new AppSmsException(-1, "知识库问答失败(HTTP " + response.statusCode() + ")");
      }
      return parseSseStream(response.body());
    } catch (AppSmsException e) {
      throw e;
    } catch (Exception e) {
      log.error("WeKnora knowledgeChat failed sessionId={}", sessionId, e);
      throw new AppSmsException(-1, "知识库问答失败：" + e.getMessage());
    }
  }

  private WeKnoraChatResult parseSseStream(java.io.InputStream inputStream) throws Exception {
    StringBuilder answer = new StringBuilder();
    StringBuilder thinking = new StringBuilder();
    List<Map<String, Object>> references = new ArrayList<>();
    String sessionTitle = null;

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.startsWith("data:")) {
          continue;
        }
        String json = line.substring(5).trim();
        if (json.isEmpty()) {
          continue;
        }
        JsonNode node = objectMapper.readTree(json);
        String responseType = node.path("response_type").asText("");
        if ("error".equals(responseType)) {
          String msg = node.path("content").asText("知识库服务异常");
          if (msg.contains("invalid image detail") || msg.contains("LLM call failed")) {
            msg = "图片识别服务配置异常，请管理员配置 COACH_VISION_API_KEY 或更换 WeKnora 视觉模型";
          }
          throw new AppSmsException(-1, msg);
        }
        if ("answer".equals(responseType) && !node.path("done").asBoolean(false)) {
          answer.append(node.path("content").asText(""));
        }
        if ("thinking".equals(responseType) && !node.path("done").asBoolean(false)) {
          thinking.append(node.path("content").asText(""));
        }
        // agent-chat 可能把最终文本放在 tool_result 等事件，answer 为空时兜底
        if ("tool_result".equals(responseType)) {
          String toolContent = node.path("content").asText("");
          if (StringUtils.hasText(toolContent) && !StringUtils.hasText(answer)) {
            answer.append(toolContent);
          }
        }
        if ("references".equals(responseType)) {
          JsonNode refs = node.path("knowledge_references");
          if (refs.isArray()) {
            refs.forEach(ref -> {
              Map<String, Object> item = new LinkedHashMap<>();
              item.put("title", ref.path("knowledge_title").asText(""));
              item.put("filename", ref.path("knowledge_filename").asText(""));
              item.put("content", ref.path("content").asText(""));
              item.put("score", ref.path("score").asDouble(0));
              references.add(item);
            });
          }
        }
        if ("session_title".equals(responseType)) {
          sessionTitle = node.path("content").asText(null);
        }
      }
    }

    String finalAnswer = answer.toString().trim();
    if (!StringUtils.hasText(finalAnswer)) {
      finalAnswer = thinking.toString().trim();
    }
    if (!StringUtils.hasText(finalAnswer)) {
      finalAnswer = "未从知识库检索到足够信息，请换个问法或联系管理员补充资料。";
    }
    return new WeKnoraChatResult(finalAnswer, references, sessionTitle);
  }

  /**
   * 按知识库展示名解析 ID 列表（精确匹配优先，其次包含匹配）。
   * 结果有缓存；解析失败返回空列表（调用方可回退到智能体已绑定 KB）。
   */
  public List<String> resolveKnowledgeBaseIdsByName(String name) {
    if (!StringUtils.hasText(name)) {
      return List.of();
    }
    String key = name.trim();
    List<String> cached = kbNameCache.get(key);
    if (cached != null) {
      return cached;
    }
    try {
      ensureConfigured();
      List<Map<String, String>> all = listKnowledgeBases();
      List<String> exact = new ArrayList<>();
      List<String> contains = new ArrayList<>();
      for (Map<String, String> row : all) {
        String id = row.get("id");
        String n = row.get("name");
        if (!StringUtils.hasText(id) || !StringUtils.hasText(n)) {
          continue;
        }
        if (n.equals(key)) {
          exact.add(id);
        } else if (n.contains(key) || key.contains(n)) {
          contains.add(id);
        }
      }
      List<String> resolved = !exact.isEmpty() ? List.copyOf(exact) : List.copyOf(contains);
      if (!resolved.isEmpty()) {
        kbNameCache.put(key, resolved);
      }
      log.info("WeKnora resolve KB name='{}' -> {}", key, resolved);
      return resolved;
    } catch (Exception e) {
      log.warn("WeKnora resolve KB by name failed name={}: {}", key, e.getMessage());
      return List.of();
    }
  }

  /** GET /knowledge-bases —— 返回 [{id,name}, ...] */
  public List<Map<String, String>> listKnowledgeBases() {
    ensureConfigured();
    try {
      JsonNode resp = restClient.get()
          .uri(uriBuilder -> uriBuilder.path("/knowledge-bases")
              .queryParam("page", 1)
              .queryParam("page_size", 100)
              .build())
          .retrieve()
          .body(JsonNode.class);
      if (resp == null) {
        return List.of();
      }
      JsonNode data = resp.path("data");
      JsonNode items = data;
      if (data.isObject()) {
        if (data.has("items")) {
          items = data.path("items");
        } else if (data.has("list")) {
          items = data.path("list");
        } else if (data.has("knowledge_bases")) {
          items = data.path("knowledge_bases");
        }
      }
      if (!items.isArray()) {
        return List.of();
      }
      List<Map<String, String>> out = new ArrayList<>();
      items.forEach(node -> {
        String id = firstText(node, "id", "knowledge_base_id");
        String name = firstText(node, "name", "title", "knowledge_base_name");
        if (StringUtils.hasText(id)) {
          Map<String, String> row = new LinkedHashMap<>();
          row.put("id", id);
          row.put("name", name == null ? "" : name);
          out.add(row);
        }
      });
      return out;
    } catch (Exception e) {
      log.warn("WeKnora listKnowledgeBases failed: {}", e.getMessage());
      throw e instanceof RuntimeException re ? re : new RuntimeException(e);
    }
  }

  private static String firstText(JsonNode node, String... fields) {
    for (String f : fields) {
      String v = node.path(f).asText(null);
      if (StringUtils.hasText(v)) {
        return v.trim();
      }
    }
    return null;
  }

  /** 确保为 WeKnora 要求的 data:image/...;base64,... 格式。 */
  static String normalizeImageDataUrl(String raw) {
    if (!StringUtils.hasText(raw)) {
      return raw;
    }
    String s = raw.trim();
    if (s.startsWith("data:")) {
      return s;
    }
    return "data:image/jpeg;base64," + s;
  }

  public record WeKnoraChatResult(
      String answer,
      List<Map<String, Object>> references,
      String sessionTitle
  ) {}
}
