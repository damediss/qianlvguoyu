package com.guoyu.platform.app.coach.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.app.coach.config.WeKnoraProperties;
import com.guoyu.platform.app.sms.AppSmsException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 拍照搜题视觉识别：直接调用 OpenAI 兼容视觉 API（不传 detail=auto，避免 LongCat 等模型 400）。
 * 识别结果作为文本再交给 WeKnora 知识库问答。
 */
@Component
@Slf4j
public class CoachVisionClient {

  private static final String DEFAULT_PROMPT =
      "请识别图片中的题目、选项和关键文字，逐行输出；若看不清请说明。";

  private final WeKnoraProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public CoachVisionClient(WeKnoraProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
        .build();
  }

  public boolean isConfigured() {
    return properties.visionPreprocessEnabled()
        && StringUtils.hasText(properties.visionApiUrl())
        && StringUtils.hasText(properties.visionApiKey());
  }

  public String describeImage(String dataUrl, String userHint) {
    if (!isConfigured()) {
      throw new AppSmsException(-1, "视觉识别未配置，请联系管理员设置 COACH_VISION_API_KEY");
    }
    String prompt = StringUtils.hasText(userHint) ? userHint : DEFAULT_PROMPT;
    try {
      Map<String, Object> textPart = Map.of("type", "text", "text", prompt);
      Map<String, Object> imagePart = Map.of(
          "type", "image_url",
          "image_url", Map.of("url", dataUrl));
      Map<String, Object> message = Map.of(
          "role", "user",
          "content", List.of(textPart, imagePart));
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("model", properties.visionModel());
      body.put("messages", List.of(message));
      body.put("max_tokens", 2048);

      String payload = objectMapper.writeValueAsString(body);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(properties.visionApiUrl()))
          .timeout(Duration.ofSeconds(properties.readTimeoutSeconds()))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + properties.visionApiKey())
          .POST(HttpRequest.BodyPublishers.ofString(payload))
          .build();

      HttpResponse<String> response = httpClient.send(
          request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() >= 400) {
        log.warn("Vision API HTTP {}: {}", response.statusCode(), response.body());
        throw new AppSmsException(-1, "题目图片识别失败(HTTP " + response.statusCode() + ")");
      }
      JsonNode root = objectMapper.readTree(response.body());
      String content = root.path("choices").path(0).path("message").path("content").asText("");
      if (!StringUtils.hasText(content)) {
        throw new AppSmsException(-1, "未能从图片中识别出文字，请换一张更清晰的题目照片");
      }
      log.info("[appcoach] vision OCR done, chars={}", content.length());
      return content.trim();
    } catch (AppSmsException e) {
      throw e;
    } catch (Exception e) {
      log.error("[appcoach] vision describe failed", e);
      throw new AppSmsException(-1, "题目图片识别失败：" + e.getMessage());
    }
  }
}
