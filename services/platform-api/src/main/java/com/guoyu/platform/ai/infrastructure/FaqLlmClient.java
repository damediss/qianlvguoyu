package com.guoyu.platform.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
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
 * <p>改造说明（2026-07-22）：
 * <ul>
 *   <li>提取 {@link #executeChatCompletion} 共享层：HTTP 调用 + HTTP 级异常分类</li>
 *   <li>新增 {@link #chatCompletionMessages}：messages 透传代理模式，仅做 HTTP 级分类，内容级结果原样返回</li>
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
   * 执行 HTTP 级 + 内容级双重分类；返回 content 文本。
   *
   * <p>兼容旧调用：将整个 prompt 作为 user 消息发送，不启用 JSON mode。
   *
   * @return LLM 返回的 content 文本（finish_reason=stop 且 content 非空）
   */
  public String chatCompletion(String baseUrl, String apiKey, String modelId,
                               String fullPrompt, int maxTokens, String thinking, Long tenantId) {
    return chatCompletion(baseUrl, apiKey, modelId, null, fullPrompt, maxTokens, thinking,
        null, false, tenantId);
  }

  /**
   * 调用 LLM 生成 similar_questions + negative_questions（合并后单次调用）。
   * 支持 system/user 分离，以便 DeepSeek 上下文硬盘缓存命中稳定前缀。
   *
   * <p>DeepSeek 特殊处理：
   * <ul>
   *   <li>开启 jsonMode 时自动添加 {@code response_format: {"type": "json_object"}}</li>
   *   <li>DeepSeek JSON mode 有概率返回空 content，将自动重试最多 3 次；
   *       仍为空时返回空字符串，由调用方（解析器）按格式异常处理</li>
   * </ul>
   *
   * @param systemPrompt    system 消息（稳定前缀，可传 null）
   * @param userPrompt      user 消息（题目变量）
   * @param reasoningEffort DeepSeek 推理强度 low/high/max；仅 thinking=enabled 且为 DeepSeek 时随
   *                        thinking 节点发送（thinking 对象内字段），其它模型/场景一律不传
   * @param jsonMode        是否请求 JSON Output（仅 DeepSeek 生效）
   * @return LLM 返回的 content 文本；DeepSeek JSON mode 重试后仍为空则返回 ""
   */
  public String chatCompletion(String baseUrl, String apiKey, String modelId,
                               String systemPrompt, String userPrompt, int maxTokens,
                               String thinking, String reasoningEffort, boolean jsonMode, Long tenantId) {
    String url = trimTrailingSlash(baseUrl) + "/chat/completions";
    boolean deepSeekJsonMode = jsonMode && isDeepSeek(baseUrl);

    // 构造 OpenAI 兼容请求体
    ObjectNode requestBody = buildRequestBody(
        modelId, maxTokens, 0.7, thinking, reasoningEffort, baseUrl, deepSeekJsonMode,
        systemPrompt, userPrompt);

    // 共享层：HTTP 调用 + HTTP 级异常分类
    JsonNode response = executeChatCompletion(requestBody, url, apiKey, tenantId);

    // 内容级分类
    String content = extractContent(response, url, tenantId);

    // DeepSeek JSON mode 空 content 重试（最多 3 次）
    if ((content == null || content.isBlank()) && deepSeekJsonMode) {
      int maxRetries = 3;
      for (int attempt = 1; attempt <= maxRetries; attempt++) {
        log.warn("DeepSeek JSON mode 返回空 content，开始第 {}/{} 次重试：tenantId={}, url={}",
            attempt, maxRetries, tenantId, url);
        JsonNode retryResponse = executeChatCompletion(requestBody, url, apiKey, tenantId);
        content = extractContent(retryResponse, url, tenantId);
        if (content != null && !content.isBlank()) {
          return content;
        }
      }
      log.warn("DeepSeek JSON mode 重试 {} 次后仍为空 content，返回空字符串：tenantId={}, url={}",
          maxRetries, tenantId, url);
      return "";
    }

    if (content == null || content.isBlank()) {
      log.warn("FAQ LLM 响应 content 为空：tenantId={}, url={}", tenantId, url);
      throw new FaqLlmServerException("LLM 响应 content 为空");
    }
    return content;
  }

  /**
   * OpenAI 兼容 Chat Completion（messages 透传代理模式）。
   * 仅做 HTTP 级异常分类；finish_reason / 空 content 等内容级结果原样返回，由调用方解释。
   *
   * @param messages    OpenAI 标准 messages 数组（JsonNode 透传，Jackson 解析 JSON 数组为 ArrayNode）
   * @param temperature 采样温度（null 时使用模型配置默认值 0.7）
   * @param jsonMode    是否请求 JSON Output（仅 DeepSeek 生效）
   * @return 完整 LLM 响应 JsonNode（含 choices / usage / model / finish_reason）
   */
  public JsonNode chatCompletionMessages(String baseUrl, String apiKey, String modelId,
                                         JsonNode messages, int maxTokens,
                                         BigDecimal temperature, String thinking,
                                         String reasoningEffort, boolean jsonMode, Long tenantId) {
    String url = trimTrailingSlash(baseUrl) + "/chat/completions";
    boolean deepSeekJsonMode = jsonMode && isDeepSeek(baseUrl);

    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("model", modelId);
    requestBody.put("max_tokens", maxTokens);
    requestBody.put("temperature", temperature != null ? temperature.doubleValue() : 0.7);

    putThinkingNode(requestBody, baseUrl, thinking, reasoningEffort);

    // DeepSeek JSON mode：官方支持 response_format: {"type": "json_object"}
    if (deepSeekJsonMode) {
      ObjectNode responseFormat = objectMapper.createObjectNode();
      responseFormat.put("type", "json_object");
      requestBody.set("response_format", responseFormat);
    }

    // messages 原样透传（ArrayNode）
    requestBody.set("messages", messages);

    // 共享层：HTTP 调用 + HTTP 级异常分类，返回原始响应
    return executeChatCompletion(requestBody, url, apiKey, tenantId);
  }

  // ──────────────────────── 请求体 / 响应提取辅助方法 ────────────────────────

  /**
   * 构造 OpenAI 兼容请求体。
   */
  private ObjectNode buildRequestBody(String modelId, int maxTokens, double temperature,
                                      String thinking, String reasoningEffort, String baseUrl,
                                      boolean deepSeekJsonMode,
                                      String systemPrompt, String userPrompt) {
    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("model", modelId);
    requestBody.put("max_tokens", maxTokens);
    requestBody.put("temperature", temperature);

    putThinkingNode(requestBody, baseUrl, thinking, reasoningEffort);

    // DeepSeek JSON mode：官方支持 response_format: {"type": "json_object"}
    if (deepSeekJsonMode) {
      ObjectNode responseFormat = objectMapper.createObjectNode();
      responseFormat.put("type", "json_object");
      requestBody.set("response_format", responseFormat);
    }

    // messages：system 放稳定前缀，user 放题目变量
    ArrayNode messages = requestBody.putArray("messages");
    if (systemPrompt != null && !systemPrompt.isBlank()) {
      ObjectNode systemMsg = messages.addObject();
      systemMsg.put("role", "system");
      systemMsg.put("content", systemPrompt);
    }
    ObjectNode userMsg = messages.addObject();
    userMsg.put("role", "user");
    userMsg.put("content", userPrompt);

    return requestBody;
  }

  /**
   * 从 LLM 响应中提取 content，并检测 finish_reason=length 截断。
   *
   * @return content 文本；若 content 为空则返回 null
   * @throws FaqLlmServerException  响应缺少 choices
   * @throws FaqLlmTruncatedException finish_reason=length
   */
  private String extractContent(JsonNode response, String url, Long tenantId) {
    JsonNode choices = response.path("choices");
    if (!choices.isArray() || choices.isEmpty()) {
      log.error("FAQ LLM 响应缺少 choices：tenantId={}, url={}", tenantId, url);
      throw new FaqLlmServerException("LLM 响应缺少 choices");
    }
    JsonNode firstChoice = choices.get(0);

    String finishReason = firstChoice.path("finish_reason").asText(null);
    if ("length".equals(finishReason)) {
      log.warn("FAQ LLM 输出被截断（finish_reason=length）：tenantId={}, url={}", tenantId, url);
      throw new FaqLlmTruncatedException("LLM 输出被 max_tokens 截断（finish_reason=length）");
    }

    return firstChoice.path("message").path("content").asText(null);
  }

  // ──────────────────────── 共享层 ────────────────────────

  /**
   * 共享 HTTP 调用层 — 处理请求发送 + HTTP 级异常分类。
   * 响应日志沿用 200 字符截断（仅影响日志可读性，响应体本身完整返回）。
   *
   * @return 完整 LLM 响应 JsonNode（非 null）
   * @throws FaqLlmException HTTP 级异常（429/4xx/5xx/超时）
   */
  private JsonNode executeChatCompletion(ObjectNode requestBody, String url, String apiKey, Long tenantId) {
    String requestBodyStr;
    try {
      requestBodyStr = objectMapper.writeValueAsString(requestBody);
    } catch (Exception e) {
      requestBodyStr = requestBody.toString();
    }
    log.info("LLM 请求：tenantId={}, url={}, body={}", tenantId, url, requestBodyStr);

    try {
      JsonNode response = client.post()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .body(requestBody)
          .retrieve()
          .body(JsonNode.class);

      if (response == null) {
        log.error("LLM 返回 null：tenantId={}, url={}", tenantId, url);
        throw new FaqLlmServerException("LLM 返回空响应");
      }

      // 记录响应（截断到 200 字符，仅日志）
      String responseStr;
      try {
        responseStr = objectMapper.writeValueAsString(response);
      } catch (Exception e) {
        responseStr = response.toString();
      }
      if (responseStr.length() > 300) {
        responseStr = responseStr.substring(0, 300) + "...(truncated)";
      }
      log.info("LLM 响应：tenantId={}, url={}, body={}", tenantId, url, responseStr);

      return response;
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
        log.error("LLM 网络超时：tenantId={}, url={}, error={}", tenantId, url, ex.getMessage());
        throw new FaqLlmTimeoutException("LLM 网络超时：" + ex.getMessage());
      }
      log.error("LLM 请求异常：tenantId={}, url={}, error={}", tenantId, url, ex.getMessage());
      throw new FaqLlmTimeoutException("LLM 请求异常：" + ex.getMessage());
    }
  }

  // ──────────────────────── private helpers ────────────────────────

  /**
   * 根据 HTTP 状态码分类异常。
   * 429 → RateLimit（瞬时），4xx → Client（确定性），5xx → Server（瞬时）
   */
  private FaqLlmException classifyHttpException(int statusCode, String message, Long tenantId, String url) {
    log.error("LLM HTTP 错误：tenantId={}, status={}, url={}, message={}",
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

  /** 判断是否为 DeepSeek 模型（baseURL 含 deepseek 关键词） */
  private boolean isDeepSeek(String baseUrl) {
    return baseUrl != null && baseUrl.toLowerCase().contains("deepseek");
  }

  /**
   * 写入 thinking 节点（2026-08-11 新增 reasoning_effort 支持）。
   *
   * <p>DeepSeek 官方文档（/api/create-chat-completion）：{@code reasoning_effort} 是
   * {@code thinking} 对象内字段（low/high/max，默认 high），仅 {@code type=enabled} 时生效；
   * 仅 deepseek-v4-flash 支持三档，v4-pro 的 low 按 high 处理。
   *
   * <p>发送口径（需求方拍板）：
   * <ul>
   *   <li>DeepSeek：thinking=enabled 且 reasoningEffort 非空 → {type:enabled, reasoning_effort:值}；
   *       thinking=disabled → 仅 {type:disabled}；thinking 为空 → 不发 thinking 节点</li>
   *   <li>LongCat：维持现状仅发 {type:值}，不发 reasoning_effort（其文档不支持，AGENTS.md 坑 #13）</li>
   *   <li>其它模型：一律不发 thinking 节点</li>
   * </ul>
   */
  private void putThinkingNode(ObjectNode requestBody, String baseUrl, String thinking,
      String reasoningEffort) {
    if (thinking == null || thinking.isBlank()) {
      return;
    }
    if (isLongCat(baseUrl)) {
      ObjectNode thinkingNode = objectMapper.createObjectNode();
      thinkingNode.put("type", thinking);
      requestBody.set("thinking", thinkingNode);
      return;
    }
    if (isDeepSeek(baseUrl)) {
      ObjectNode thinkingNode = objectMapper.createObjectNode();
      thinkingNode.put("type", thinking);
      if ("enabled".equals(thinking) && reasoningEffort != null && !reasoningEffort.isBlank()) {
        thinkingNode.put("reasoning_effort", reasoningEffort);
      }
      requestBody.set("thinking", thinkingNode);
    }
  }
}
