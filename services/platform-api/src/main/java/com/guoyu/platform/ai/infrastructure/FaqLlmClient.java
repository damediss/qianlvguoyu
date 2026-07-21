package com.guoyu.platform.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.SocketTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * LLM 客户端 — 走 OpenAI 兼容的 /chat/completions 端点。
 * 支持 "openai_compatible" 格式（DeepSeek、LongCat 等）。
 *
 * <p>改造说明（2026-07-20）：
 * <ul>
 *   <li>连接池复用：Apache HttpClient 5 单例，替代原来每次 new JDK HttpClient</li>
 *   <li>异常分类：根据 HTTP status code 和 finish_reason 抛出具类型异常</li>
 *   <li>finish_reason 检测：length → Truncated；stop + content 为空 → Server</li>
 * </ul>
 *
 * <p>注意：tenantId 由调用方通过参数传入，整个调用链不调 SecurityContext。
 */
@Slf4j
@Component
public class FaqLlmClient {

  private final ObjectMapper objectMapper;
  private final RestClient client;

  public FaqLlmClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;

    // 连接池配置：maxTotal=60, perRoute=30, keepAlive=60s, validateAfterInactivity=5s
    PoolingHttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(60)
            .setMaxConnPerRoute(30)
            .setValidateAfterInactivity(TimeValue.ofSeconds(5))
            .build();

    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(70))
        .setResponseTimeout(Timeout.ofSeconds(90))
        .build();

    CloseableHttpClient httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .setKeepAliveStrategy((response, context) -> TimeValue.ofSeconds(60))
        .build();

    // onStatus：将 4xx/5xx 转为 RestClientResponseException，保留 response body
    this.client = RestClient.builder()
        .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
        .defaultStatusHandler(
            org.springframework.http.HttpStatusCode::isError,
            (request, response) -> {
              throw new RestClientResponseException(
                  "HTTP " + response.getStatusCode(),
                  response.getStatusCode().value(),
                  response.getStatusText(),
                  response.getHeaders(),
                  null,
                  null);
            })
        .build();
  }

  /**
   * 调用 LLM 生成 similar_questions + negative_questions（合并后单次调用）。
   *
   * @param baseUrl     API Base URL（不含 /chat/completions 后缀）
   * @param apiKey      API Key
   * @param modelId     模型 ID
   * @param fullPrompt  完整 prompt（含 system + user，由 FaqLlmPromptBuilder 组装）
   * @param maxTokens   最大输出 token
   * @param thinking    思考模式（enabled/disabled），仅 LongCat 模型生效，非 LongCat 传 null
   * @param tenantId    租户 ID（仅用于日志）
   * @return LLM 返回的 JSON 字符串（raw content）
   * @throws FaqLlmRateLimitException  429 限流
   * @throws FaqLlmServerException     5xx 或服务端异常（含 stop + content 为空）
   * @throws FaqLlmTimeoutException    网络超时
   * @throws FaqLlmTruncatedException  finish_reason=length 截断
   * @throws FaqLlmParseException      finish_reason=stop 但 JSON 解析失败
   * @throws FaqLlmClientException     4xx 非 429
   */
  public String chatCompletion(String baseUrl, String apiKey, String modelId,
                               String fullPrompt, int maxTokens, String thinking, Long tenantId) {
    String url = trimTrailingSlash(baseUrl) + "/chat/completions";

    // 构造 OpenAI 兼容请求体
    // 注意：LongCat 不支持 response_format 字段，不要传
    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("model", modelId);
    requestBody.put("max_tokens", maxTokens);
    requestBody.put("temperature", 0.7);

    // LongCat 思考模式：baseURL 含 longcat 且 thinking 非空时传
    if (isLongCat(baseUrl) && thinking != null && !thinking.isBlank()) {
      ObjectNode thinkingNode = objectMapper.createObjectNode();
      thinkingNode.put("type", thinking);
      requestBody.set("thinking", thinkingNode);
    }

    // fullPrompt 已包含完整的 system + user 上下文，直接作为 user 消息发送
    ArrayNode messages = requestBody.putArray("messages");
    ObjectNode userMsg = messages.addObject();
    userMsg.put("role", "user");
    userMsg.put("content", fullPrompt);

    String requestBodyStr;
    try {
      requestBodyStr = objectMapper.writeValueAsString(requestBody);
    } catch (Exception e) {
      requestBodyStr = requestBody.toString();
    }

    log.info("FAQ LLM 请求：tenantId={}, model={}, url={}, body={}", tenantId, modelId, url, requestBodyStr);

    try {
      JsonNode response = client.post()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .body(requestBody)
          .retrieve()
          .body(JsonNode.class);

      if (response == null) {
        log.error("FAQ LLM 返回 null：tenantId={}, url={}", tenantId, url);
        throw new FaqLlmServerException("LLM 返回空响应");
      }

      // 记录完整响应
      String responseStr;
      try {
        responseStr = objectMapper.writeValueAsString(response);
      } catch (Exception e) {
        responseStr = response.toString();
      }
      if (responseStr.length() > 200) {
        responseStr = responseStr.substring(0, 200) + "...(truncated)";
      }
      log.info("FAQ LLM 响应：tenantId={}, model={}, body={}", tenantId, modelId, responseStr);

      // 提取 choices[0]
      JsonNode choices = response.path("choices");
      if (!choices.isArray() || choices.isEmpty()) {
        log.error("FAQ LLM 响应缺少 choices：tenantId={}, body={}", tenantId, responseStr);
        throw new FaqLlmServerException("LLM 响应缺少 choices");
      }
      JsonNode firstChoice = choices.get(0);

      // finish_reason 检测（在解析 JSON 之前判定）
      String finishReason = firstChoice.path("finish_reason").asText(null);
      if ("length".equals(finishReason)) {
        log.warn("FAQ LLM 输出被截断（finish_reason=length）：tenantId={}, body={}", tenantId, responseStr);
        throw new FaqLlmTruncatedException("LLM 输出被 max_tokens 截断（finish_reason=length）");
      }

      // 提取 content
      String content = firstChoice.path("message").path("content").asText(null);
      if (content == null || content.isBlank()) {
        // finish_reason=stop 但 content 为空 → 瞬时，可重试
        log.warn("FAQ LLM 响应 content 为空（finish_reason={}）：tenantId={}, body={}",
            finishReason, tenantId, responseStr);
        throw new FaqLlmServerException(
            "LLM 响应 content 为空（finish_reason=" + finishReason + "）");
      }
      return content;
    } catch (FaqLlmException ex) {
      // 已分类的 LLM 异常直接向上抛
      throw ex;
    } catch (RestClientResponseException ex) {
      // HTTP 4xx / 5xx（由 defaultStatusHandler 抛出）
      throw classifyHttpException(ex.getStatusCode().value(), ex.getMessage(), tenantId, url);
    } catch (RestClientException ex) {
      // 网络超时 / 连接异常
      Throwable cause = ex.getCause();
      if (cause instanceof SocketTimeoutException) {
        log.error("FAQ LLM 网络超时：tenantId={}, url={}, error={}", tenantId, url, ex.getMessage());
        throw new FaqLlmTimeoutException("LLM 网络超时：" + ex.getMessage());
      }
      log.error("FAQ LLM 请求异常：tenantId={}, url={}, error={}", tenantId, url, ex.getMessage());
      throw new FaqLlmTimeoutException("LLM 请求异常：" + ex.getMessage());
    }
  }

  /**
   * 根据 HTTP 状态码分类异常。
   * 429 → RateLimit（瞬时），4xx → Client（确定性），5xx → Server（瞬时）
   */
  private FaqLlmException classifyHttpException(int statusCode, String message, Long tenantId, String url) {
    log.error("FAQ LLM HTTP 错误：tenantId={}, status={}, url={}, message={}",
        tenantId, statusCode, url, message);
    if (statusCode == 429) {
      return new FaqLlmRateLimitException("LLM 限流（HTTP 429）");
    }
    if (statusCode >= 400 && statusCode < 500) {
      return new FaqLlmClientException("LLM 客户端错误（HTTP " + statusCode + "）：" + message);
    }
    return new FaqLlmServerException("LLM 服务端错误（HTTP " + statusCode + "）：" + message);
  }

  private String trimTrailingSlash(String value) {
    String result = value.trim();
    while (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  /** 判断是否为 LongCat 模型（baseURL 含 longcat 关键词） */
  private boolean isLongCat(String baseUrl) {
    return baseUrl != null && baseUrl.toLowerCase().contains("longcat");
  }
}
