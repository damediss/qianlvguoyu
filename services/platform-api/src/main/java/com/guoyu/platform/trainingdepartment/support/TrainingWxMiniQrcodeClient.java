package com.guoyu.platform.trainingdepartment.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 微信小程序二维码（对齐 PHP WxminiController::signupselfqrcodeAction） */
@Component
@Slf4j
public class TrainingWxMiniQrcodeClient {

  private static final String ACCESS_TOKEN_KEY = "wxmini_accesstoken";
  private static final String CREATE_QRCODE_URL =
      "https://api.weixin.qq.com/cgi-bin/wxaapp/createwxaqrcode?access_token=%s";
  private static final String TOKEN_URL =
      "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";

  private final String appId;
  private final String appSecret;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  public TrainingWxMiniQrcodeClient(
      @Value("${platform.wechat.mini.app-id:}") String appId,
      @Value("${platform.wechat.mini.app-secret:}") String appSecret,
      @Autowired(required = false) StringRedisTemplate redisTemplate,
      ObjectMapper objectMapper) {
    this.appId = appId;
    this.appSecret = appSecret;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  public byte[] createSignUpSelfQrcode(Long signUpId) {
    return createPageQrcode("pages/registration/detail?id=" + signUpId);
  }

  /** 企业调研人员采集：扫码进入小程序 pages/research/collect */
  public byte[] createResearchCollectQrcode(String token) {
    if (!StringUtils.hasText(token)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少采集 token");
    }
    return createPageQrcode("pages/research/collect?token=" + token.trim());
  }

  public byte[] createPageQrcode(String page) {
    ensureConfigured();
    if (!StringUtils.hasText(page)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少小程序页面路径");
    }
    if (page.getBytes(StandardCharsets.UTF_8).length > 128) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "小程序码 path 超过 128 字节");
    }
    byte[] lastData = null;
    for (int attempt = 0; attempt < 2; attempt++) {
      if (attempt > 0) {
        clearCachedToken();
      }
      String accessToken = getAccessToken(attempt > 0);
      QrcodeResponse response = requestQrcode(accessToken, page);
      lastData = response.body();
      if (isImage(lastData)) {
        return lastData;
      }
      log.warn(
          "微信二维码生成失败 attempt={} status={} page={} err={}",
          attempt,
          response.statusCode(),
          page,
          previewError(lastData));
      if (attempt == 0 && isTokenError(lastData)) {
        continue;
      }
      break;
    }
    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "微信二维码生成失败: " + parseWeChatError(lastData));
  }

  private void ensureConfigured() {
    if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请配置微信小程序 app-id 与 app-secret");
    }
  }

  private String getAccessToken(boolean refresh) {
    if (!refresh) {
      String cached = readCachedToken();
      if (StringUtils.hasText(cached)) {
        return cached.trim();
      }
    }
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(String.format(TOKEN_URL, appId, appSecret)))
          .timeout(Duration.ofSeconds(15))
          .GET()
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException(
            ErrorCode.INTERNAL_ERROR,
            "获取微信 access_token 失败: HTTP " + response.statusCode());
      }
      JsonNode json = objectMapper.readTree(response.body());
      if (json.hasNonNull("access_token")) {
        String token = json.get("access_token").asText();
        long expiresIn = json.has("expires_in") ? json.get("expires_in").asLong(7200) : 7200;
        cacheToken(token, expiresIn);
        return token;
      }
      String errMsg = formatWeChatJsonError(json, response.body());
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取微信 access_token 失败: " + errMsg);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.warn("获取微信 access_token 异常", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取微信 access_token 失败: " + e.getMessage());
    }
  }

  private String readCachedToken() {
    if (redisTemplate == null) {
      return null;
    }
    try {
      return redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
    } catch (Exception e) {
      log.warn("读取微信 access_token 缓存失败，将直接请求微信 API", e);
      return null;
    }
  }

  private void cacheToken(String token, long expiresIn) {
    if (redisTemplate == null) {
      return;
    }
    try {
      redisTemplate.opsForValue().set(
          ACCESS_TOKEN_KEY, token, Duration.ofSeconds(Math.max(60, expiresIn - 120)));
    } catch (Exception e) {
      log.warn("写入微信 access_token 缓存失败", e);
    }
  }

  private void clearCachedToken() {
    if (redisTemplate == null) {
      return;
    }
    try {
      redisTemplate.delete(ACCESS_TOKEN_KEY);
    } catch (Exception e) {
      log.warn("清除微信 access_token 缓存失败", e);
    }
  }

  private QrcodeResponse requestQrcode(String accessToken, String page) {
    try {
      String body = objectMapper.writeValueAsString(java.util.Map.of("path", page));
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(String.format(CREATE_QRCODE_URL, accessToken)))
          .timeout(Duration.ofSeconds(30))
          .header("Content-Type", "application/json; charset=utf-8")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
      return new QrcodeResponse(response.statusCode(), response.body());
    } catch (Exception e) {
      log.warn("请求微信二维码异常: page={}", page, e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "请求微信二维码失败: " + e.getMessage());
    }
  }

  private static boolean isImage(byte[] data) {
    return isPng(data) || isJpeg(data);
  }

  private static boolean isPng(byte[] data) {
    return data != null
        && data.length >= 8
        && data[0] == (byte) 0x89
        && data[1] == 0x50
        && data[2] == 0x4E
        && data[3] == 0x47;
  }

  private static boolean isJpeg(byte[] data) {
    return data != null
        && data.length >= 2
        && (data[0] & 0xFF) == 0xFF
        && (data[1] & 0xFF) == 0xD8;
  }

  private boolean isTokenError(byte[] data) {
    Integer errcode = extractErrcode(data);
    return errcode != null && (errcode == 40001 || errcode == 40014 || errcode == 42001 || errcode == 42007);
  }

  private Integer extractErrcode(byte[] data) {
    if (data == null || data.length == 0) {
      return null;
    }
    try {
      JsonNode json = objectMapper.readTree(new String(data, StandardCharsets.UTF_8));
      if (json.has("errcode")) {
        return json.get("errcode").asInt();
      }
    } catch (Exception ignored) {
      // ignore
    }
    return null;
  }

  private String parseWeChatError(byte[] data) {
    if (data == null || data.length == 0) {
      return "空响应（请检查服务器能否访问 api.weixin.qq.com）";
    }
    String text = new String(data, StandardCharsets.UTF_8).trim();
    if (text.isEmpty()) {
      return "空响应（请检查服务器能否访问 api.weixin.qq.com）";
    }
    try {
      JsonNode json = objectMapper.readTree(text);
      return formatWeChatJsonError(json, text);
    } catch (Exception e) {
      return previewError(data);
    }
  }

  private String formatWeChatJsonError(JsonNode json, String fallback) {
    if (json == null) {
      return fallback;
    }
    String errmsg = json.has("errmsg") ? json.get("errmsg").asText() : fallback;
    if (json.has("errcode")) {
      return json.get("errcode").asInt() + " " + errmsg;
    }
    return errmsg;
  }

  private String previewError(byte[] data) {
    if (data == null || data.length == 0) {
      return "空响应";
    }
    if (isImage(data)) {
      return "返回图片但格式未识别";
    }
    String text = new String(data, StandardCharsets.UTF_8).trim();
    if (text.startsWith("<")) {
      return "网络返回 HTML 而非微信 JSON（可能被防火墙/代理拦截）";
    }
    return text.length() > 200 ? text.substring(0, 200) + "..." : text;
  }

  private record QrcodeResponse(int statusCode, byte[] body) {}
}
