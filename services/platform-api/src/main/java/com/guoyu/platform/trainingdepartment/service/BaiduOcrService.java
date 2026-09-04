package com.guoyu.platform.trainingdepartment.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 百度 OCR 身份证识别服务（对齐 PHP BaiduOrc 类）
 * API 文档：https://ai.baidu.com/ai-doc/OCR/rk3h7yckg
 */
@Service
@Slf4j
public class BaiduOcrService {

  private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
  private static final String IDCARD_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/idcard";
  /** 通用文字识别（标准版，更快） */
  private static final String GENERAL_BASIC_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";
  /** 通用文字识别（高精度，较慢，作兜底） */
  private static final String ACCURATE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic";

  private final String apiKey;
  private final String secretKey;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  private volatile String cachedToken;
  private volatile long tokenExpiresAt;

  public BaiduOcrService(
      @Value("${platform.baidu.api-key:}") String apiKey,
      @Value("${platform.baidu.secret-key:}") String secretKey,
      ObjectMapper objectMapper) {
    this.apiKey = apiKey;
    this.secretKey = secretKey;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  public boolean isEnabled() {
    return StringUtils.hasText(apiKey) && StringUtils.hasText(secretKey);
  }

  /**
   * 识别身份证
   * @param localFilePath 本地文件路径（优先使用）
   * @param imageUrl      图片公网 URL（本地文件不存在时回退）
   * @param side          "front" 或 "back"
   * @return 识别结果 map，与 PHP 旧系统返回结构对齐
   */
  public Map<String, Object> recognizeIdCard(String localFilePath, String imageUrl, String side) {
    if (!isEnabled()) {
      log.warn("百度 OCR 未配置 API Key，跳过识别");
      return null;
    }

    try {
      String token = getAccessToken();
      if (token == null) {
        log.error("获取百度 OCR access_token 失败");
        return null;
      }

      Map<String, String> params = new LinkedHashMap<>();
      params.put("id_card_side", side);
      params.put("detect_card", "true");

      java.nio.file.Path localFile = java.nio.file.Paths.get(localFilePath);
      if (java.nio.file.Files.exists(localFile)) {
        byte[] imageBytes = java.nio.file.Files.readAllBytes(localFile);
        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
        params.put("image", base64);
        log.info("百度 OCR 使用本地文件 base64 方式: {}", localFilePath);
      } else if (StringUtils.hasText(imageUrl)) {
        params.put("url", imageUrl);
        log.info("百度 OCR 使用 URL 方式: {}", imageUrl);
      } else {
        log.error("百度 OCR 无可用图片源");
        return null;
      }

      String body = buildFormBody(params);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(IDCARD_URL + "?access_token=" + token))
          .timeout(Duration.ofSeconds(15))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.error("百度 OCR 请求失败: status={}", response.statusCode());
        return null;
      }

      JsonNode root = objectMapper.readTree(response.body());

      if (root.has("error_code") && root.get("error_code").asInt() != 0) {
        log.error("百度 OCR 返回错误: {}", response.body());
        if (root.get("error_code").asInt() == 110 || root.get("error_code").asInt() == 111) {
          cachedToken = null;
        }
        return null;
      }

      return parseIdCardResult(root, side);

    } catch (Exception e) {
      log.error("百度 OCR 识别异常", e);
      return null;
    }
  }

  /**
   * 通用文字识别（高精度版）：返回拼接后的全文，失败返回 null。
   * 用于证书扫描件在基础版仍缺硬字段时的兜底。
   */
  public String recognizeGeneralText(byte[] imageBytes) {
    return recognizeOcrText(ACCURATE_URL, imageBytes, 30);
  }

  /**
   * 通用文字识别（标准版）：更快、配额更宽，优先用于证书预览匹配。
   */
  public String recognizeGeneralTextFast(byte[] imageBytes) {
    return recognizeOcrText(GENERAL_BASIC_URL, imageBytes, 15);
  }

  private String recognizeOcrText(String apiUrl, byte[] imageBytes, int timeoutSeconds) {
    if (!isEnabled() || imageBytes == null || imageBytes.length == 0) {
      return null;
    }
    try {
      String token = getAccessToken();
      if (token == null) {
        return null;
      }
      Map<String, String> params = new LinkedHashMap<>();
      params.put("image", java.util.Base64.getEncoder().encodeToString(imageBytes));
      params.put("detect_direction", "true");
      String body = buildFormBody(params);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(apiUrl + "?access_token=" + token))
          .timeout(Duration.ofSeconds(timeoutSeconds))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.error("百度 OCR 请求失败: status={} url={}", response.statusCode(), apiUrl);
        return null;
      }
      JsonNode root = objectMapper.readTree(response.body());
      if (root.has("error_code") && root.get("error_code").asInt() != 0) {
        log.error("百度 OCR 返回错误: {}", response.body());
        if (root.get("error_code").asInt() == 110 || root.get("error_code").asInt() == 111) {
          cachedToken = null;
        }
        return null;
      }
      JsonNode wordsResult = root.get("words_result");
      if (wordsResult == null || !wordsResult.isArray()) {
        return null;
      }
      StringBuilder sb = new StringBuilder();
      for (JsonNode item : wordsResult) {
        if (item != null && item.has("words")) {
          String w = item.get("words").asText("");
          if (StringUtils.hasText(w)) {
            if (sb.length() > 0) {
              sb.append('\n');
            }
            sb.append(w.trim());
          }
        }
      }
      return sb.length() > 0 ? sb.toString() : null;
    } catch (Exception e) {
      log.error("百度 OCR 识别异常 url={}", apiUrl, e);
      return null;
    }
  }

  private Map<String, Object> parseIdCardResult(JsonNode root, String side) {
    Map<String, Object> result = new LinkedHashMap<>();
    JsonNode wordsResult = root.get("words_result");
    if (wordsResult == null) {
      return result;
    }

    if ("front".equals(side)) {
      result.put("realname", getField(wordsResult, "姓名"));
      result.put("nation", getField(wordsResult, "民族"));
      result.put("address", getField(wordsResult, "住址"));
      result.put("id_card_number", getField(wordsResult, "公民身份号码"));
      result.put("birthday", getField(wordsResult, "出生"));
      result.put("gender", getField(wordsResult, "性别"));

      String birthday = (String) result.get("birthday");
      if (StringUtils.hasText(birthday) && birthday.length() >= 4) {
        try {
          int birthYear = Integer.parseInt(birthday.substring(0, 4));
          int currentYear = java.time.Year.now().getValue();
          result.put("age", currentYear - birthYear);
        } catch (NumberFormatException e) {
          result.put("age", null);
        }
      }
    } else {
      result.put("issue_authority", getField(wordsResult, "签发机关"));
      // 百度身份证反面字段：签发日期 / 失效日期；部分返回合并在「有效期限」
      result.put("valid_date_start", getField(wordsResult, "签发日期"));
      result.put("valid_date_end", getField(wordsResult, "失效日期"));
      if (!StringUtils.hasText((String) result.get("valid_date_start"))
          && wordsResult.has("有效期限")) {
        String period = getField(wordsResult, "有效期限");
        if (StringUtils.hasText(period) && period.contains("-")) {
          String[] parts = period.split("-", 2);
          result.put("valid_date_start", parts[0].trim());
          result.put("valid_date_end", parts.length > 1 ? parts[1].trim() : "");
        }
      }
    }

    // detect_card=true 时返回证件板裁剪图（base64，不含 data: 前缀）及位置
    if (root.has("card_image") && !root.get("card_image").isNull()) {
      String cardImage = root.get("card_image").asText("");
      if (StringUtils.hasText(cardImage)) {
        result.put("card_image_base64", cardImage);
      }
    }
    JsonNode cardLocation = root.get("card_location");
    if (cardLocation != null && !cardLocation.isNull()
        && cardLocation.has("left") && cardLocation.has("top")
        && cardLocation.has("width") && cardLocation.has("height")) {
      Map<String, Object> loc = new LinkedHashMap<>();
      loc.put("left", cardLocation.get("left").asInt());
      loc.put("top", cardLocation.get("top").asInt());
      loc.put("width", cardLocation.get("width").asInt());
      loc.put("height", cardLocation.get("height").asInt());
      result.put("card_location", loc);
    }

    return result;
  }

  private String getField(JsonNode wordsResult, String fieldName) {
    JsonNode field = wordsResult.get(fieldName);
    if (field != null && field.has("words")) {
      return field.get("words").asText("");
    }
    return "";
  }

  private synchronized String getAccessToken() {
    if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt) {
      return cachedToken;
    }

    try {
      StringJoiner params = new StringJoiner("&");
      params.add("grant_type=client_credentials");
      params.add("client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
      params.add("client_secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8));

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(TOKEN_URL))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(params.toString()))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JsonNode root = objectMapper.readTree(response.body());

      if (root.has("access_token")) {
        cachedToken = root.get("access_token").asText();
        long expiresIn = root.has("expires_in") ? root.get("expires_in").asLong() : 2592000;
        tokenExpiresAt = System.currentTimeMillis() + (expiresIn - 300) * 1000;
        log.info("百度 OCR access_token 获取成功，有效期 {}s", expiresIn);
        return cachedToken;
      } else {
        log.error("百度 OCR token 响应异常: {}", response.body());
        return null;
      }
    } catch (Exception e) {
      log.error("获取百度 OCR access_token 失败", e);
      return null;
    }
  }

  private String buildFormBody(Map<String, String> params) {
    StringJoiner joiner = new StringJoiner("&");
    for (Map.Entry<String, String> entry : params.entrySet()) {
      joiner.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
          + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }
    return joiner.toString();
  }
}
