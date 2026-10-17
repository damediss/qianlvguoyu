package com.guoyu.platform.ai.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * <p>改造说明（2026-09-08）：
 * <ul>
 *   <li>glm-5.3-flash 单模型并发闸口：公平信号量封顶全局在飞调用（默认 50，可配
 *       platform.ai.glm-max-concurrency），超出排队等待；仅该模型受限，同账户其它 GLM
 *       模型与其它平台不受影响</li>
 *   <li>连接池扩容：maxTotal 60→120、perRoute 30→60（≥ 闸口上限，否则连接池先成为瓶颈）</li>
 * </ul>
 *
 * <p>注意：tenantId 由调用方通过参数传入，整个调用链不调 SecurityContext。
 */
@Slf4j
@Component
public class FaqLlmClient {

  private final ObjectMapper objectMapper;
  private final RestClient client;

  /** glm-5.3-flash 单模型并发闸口上限（方案 A，2026-09-08）：超出排队等待，仅该模型受限。
   * 字段内联默认值兜底单测直接 new（无 Spring 注入）的场景 */
  @Value("${platform.ai.glm-max-concurrency:50}")
  private int glmMaxConcurrency = 50;

  /** 模型并发闸口（key = modelId 小写）：公平信号量保证先到先得排队 */
  private final ConcurrentHashMap<String, Semaphore> modelGates = new ConcurrentHashMap<>();

  /** 连接池与底层 HttpClient 单例（各超时档位 RestClient 共享同一连接池） */
  private final PoolingHttpClientConnectionManager connectionManager;
  /** responseTimeout 分档缓存：秒数 → 独立 RequestConfig 的 RestClient（共享连接池） */
  private final Map<Integer, RestClient> timeoutClients = new ConcurrentHashMap<>();

  public FaqLlmClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;

    // 连接池配置：maxTotal=120, perRoute=60, keepAlive=60s, validateAfterInactivity=5s
    // perRoute 60 ≥ glm-5.3-flash 并发闸口上限 50，另给其它平台/模型留余量
    PoolingHttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(120)
            .setMaxConnPerRoute(60)
            .setValidateAfterInactivity(TimeValue.ofSeconds(5))
            .build();
    this.connectionManager = connectionManager;

    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(70))
        .setResponseTimeout(Timeout.ofSeconds(90))
        .build();

    CloseableHttpClient httpClient = buildHttpClient(requestConfig);

    // onStatus：将 4xx/5xx 转为 RestClientResponseException，保留 response body
    this.client = buildRestClient(httpClient);
  }

  /**
   * 按响应超时秒数取 RestClient（并发与成本优化方案 v0.3 P2/R5）。
   * 0 或负数返回默认实例（90s）；理论生成链路传 180。
   * 各档位共享同一连接池（connectionManager），仅 RequestConfig 不同。
   */
  private RestClient clientForResponseTimeout(int responseTimeoutSeconds) {
    if (responseTimeoutSeconds <= 0) {
      return client;
    }
    return timeoutClients.computeIfAbsent(responseTimeoutSeconds, seconds -> {
      RequestConfig requestConfig = RequestConfig.custom()
          .setConnectTimeout(Timeout.ofSeconds(70))
          .setResponseTimeout(Timeout.ofSeconds(seconds))
          .build();
      CloseableHttpClient httpClient = buildHttpClient(requestConfig);
      return buildRestClient(httpClient);
    });
  }

  /**
   * 统一构建 HttpClient（各响应超时档位共享连接池，仅 RequestConfig 不同）。
   *
   * <p>必须禁用内容压缩：类路径含 Tika 传递依赖 org.brotli:dec 0.1.2，HttpClient 会据此自动
   * 广告 Accept-Encoding: br；该版本无参 read() 读 1 字节后流即耗尽，而 Spring 转换响应前
   * 恰好会先 read() 一个字节探测空体，导致 br 响应只解出首个字符 "{"（2026-10-17 DeepSeek
   * 边缘启用 Brotli 后全线解析失败）。禁用后不协商压缩，响应为明文，LLM 文本量级可忽略。
   */
  private CloseableHttpClient buildHttpClient(RequestConfig requestConfig) {
    return HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .setKeepAliveStrategy((response, context) -> TimeValue.ofSeconds(60))
        .disableContentCompression()
        .build();
  }

  /** 统一构建 RestClient（连接超时等由 HttpClient 默认 RequestConfig 决定，状态处理一致） */
  private RestClient buildRestClient(CloseableHttpClient httpClient) {
    return RestClient.builder()
        .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
        .defaultStatusHandler(
            org.springframework.http.HttpStatusCode::isError,
            (request, response) -> {
              // 错误响应体必须带回：LLM 平台的错误类型（GLM 1302 限流/1305 过载等）只在
              // body 的 error.code 里，丢弃 body 会导致日志只见 "HTTP 400" 无法归类
              String body = null;
              try {
                body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
              } catch (IOException ignored) {
                // 读不到 body 时退回仅状态码
              }
              String detail = body == null || body.isBlank() ? "" : " body=" + abbreviateBody(body);
              throw new RestClientResponseException(
                  "HTTP " + response.getStatusCode() + detail,
                  response.getStatusCode().value(),
                  response.getStatusText(),
                  response.getHeaders(),
                  body == null ? null : body.getBytes(StandardCharsets.UTF_8),
                  null);
            })
        .build();
  }

  /** 错误体截断（日志可读性；完整 body 仍在异常的 responseBody 里） */
  private String abbreviateBody(String s) {
    return s.length() <= 500 ? s : s.substring(0, 500) + "...(truncated)";
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
   * @param jsonMode        是否请求 JSON Output（DeepSeek / 智谱 GLM 生效，其余平台忽略）
   * @return LLM 返回的 content 文本；DeepSeek JSON mode 重试后仍为空则返回 ""
   */
  public String chatCompletion(String baseUrl, String apiKey, String modelId,
                               String systemPrompt, String userPrompt, int maxTokens,
                               String thinking, String reasoningEffort, boolean jsonMode, Long tenantId) {
    // 既有调用方（FAQ 等）走默认 90s 响应超时，行为与改造前完全一致
    return chatCompletionDetailed(baseUrl, apiKey, modelId, systemPrompt, userPrompt, maxTokens,
        thinking, reasoningEffort, jsonMode, tenantId, 0).content();
  }

  /**
   * OpenAI 兼容 Chat Completion（详细版，并发与成本优化方案 v0.3 P2）。
   * 与 {@link #chatCompletion} 逻辑一致，另：
   * <ul>
   *   <li>返回 usage 明细（prompt/completion/total/cached，双口径归一见
   *       {@link LlmChatCompletionResult#from}）</li>
   *   <li>responseTimeoutSeconds 参数化响应超时（理论生成链路传 180，GLM thinking 长调用；
   *       0 或负数走默认 90s）</li>
   * </ul>
   *
   * @param responseTimeoutSeconds 响应超时秒数；0/负数 = 默认 90s
   */
  public LlmChatCompletionResult chatCompletionDetailed(String baseUrl, String apiKey, String modelId,
                               String systemPrompt, String userPrompt, int maxTokens,
                               String thinking, String reasoningEffort, boolean jsonMode, Long tenantId,
                               int responseTimeoutSeconds) {
    String url = trimTrailingSlash(baseUrl) + "/chat/completions";
    // JSON 模式平台分支：DeepSeek 与智谱 GLM（bigmodel.cn，官方 struct-output 文档）均支持
    // response_format: {"type": "json_object"}；其余平台不传（坑 #13）
    boolean jsonModeActive = jsonMode && (isDeepSeek(baseUrl) || isGlm(baseUrl));
    RestClient targetClient = clientForResponseTimeout(responseTimeoutSeconds);

    // 构造 OpenAI 兼容请求体
    ObjectNode requestBody = buildRequestBody(
        modelId, maxTokens, 0.7, thinking, reasoningEffort, baseUrl, jsonModeActive,
        systemPrompt, userPrompt);

    // 共享层：HTTP 调用 + HTTP 级异常分类
    JsonNode response = executeChatCompletion(requestBody, url, baseUrl, modelId, apiKey,
        tenantId, targetClient);

    // 内容级分类
    String content = extractContent(response, url, tenantId);

    // JSON mode 空 content 重试（最多 3 次；DeepSeek 已知问题，GLM 同路径防御）
    if ((content == null || content.isBlank()) && jsonModeActive) {
      int maxRetries = 3;
      for (int attempt = 1; attempt <= maxRetries; attempt++) {
        log.warn("DeepSeek JSON mode 返回空 content，开始第 {}/{} 次重试：tenantId={}, url={}",
            attempt, maxRetries, tenantId, url);
        response = executeChatCompletion(requestBody, url, baseUrl, modelId, apiKey,
            tenantId, targetClient);
        content = extractContent(response, url, tenantId);
        if (content != null && !content.isBlank()) {
          return LlmChatCompletionResult.from(response, content);
        }
      }
      log.warn("DeepSeek JSON mode 重试 {} 次后仍为空 content，返回空 usage：tenantId={}, url={}",
          maxRetries, tenantId, url);
      return LlmChatCompletionResult.empty("");
    }

    if (content == null || content.isBlank()) {
      log.warn("FAQ LLM 响应 content 为空：tenantId={}, url={}", tenantId, url);
      throw new FaqLlmServerException("LLM 响应 content 为空");
    }
    return LlmChatCompletionResult.from(response, content);
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
    // JSON 模式平台分支：DeepSeek / GLM 支持 response_format，其余平台不传（坑 #13）
    boolean deepSeekJsonMode = jsonMode && (isDeepSeek(baseUrl) || isGlm(baseUrl));

    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("model", modelId);
    requestBody.put("max_tokens", maxTokens);
    requestBody.put("temperature", temperature != null ? temperature.doubleValue() : 0.7);

    putThinkingNode(requestBody, baseUrl, thinking, reasoningEffort);

    // JSON mode（DeepSeek / GLM 官方支持）：response_format: {"type": "json_object"}
    if (deepSeekJsonMode) {
      ObjectNode responseFormat = objectMapper.createObjectNode();
      responseFormat.put("type", "json_object");
      requestBody.set("response_format", responseFormat);
    }

    // messages 原样透传（ArrayNode）
    requestBody.set("messages", messages);

    // 共享层：HTTP 调用 + HTTP 级异常分类，返回原始响应（messages 透传代理走默认超时）
    return executeChatCompletion(requestBody, url, baseUrl, modelId, apiKey, tenantId, client);
  }

  // ──────────────────────── 请求体 / 响应提取辅助方法 ────────────────────────

  /**
   * 构造 OpenAI 兼容请求体。
   */
  private ObjectNode buildRequestBody(String modelId, int maxTokens, double temperature,
                                      String thinking, String reasoningEffort, String baseUrl,
                                      boolean jsonModeActive,
                                      String systemPrompt, String userPrompt) {
    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("model", modelId);
    requestBody.put("max_tokens", maxTokens);
    requestBody.put("temperature", temperature);

    putThinkingNode(requestBody, baseUrl, thinking, reasoningEffort);

    // JSON mode（DeepSeek / GLM 官方支持）：response_format: {"type": "json_object"}
    if (jsonModeActive) {
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
   * 取模型并发闸口（package-private 便于单测直测选择逻辑）。
   * 当前仅智谱 bigmodel 域下的 glm-5.3-flash 限流（方案 A，2026-09-08 需求方确认口径：
   * 按"这一个模型"限 50，同账户其它 GLM 模型不占额度）；其它模型/平台返回 null（不限）。
   */
  Semaphore gateFor(String baseUrl, String modelId) {
    if (!isGlm(baseUrl) || modelId == null
        || !"glm-5.3-flash".equalsIgnoreCase(modelId.trim())) {
      return null;
    }
    return modelGates.computeIfAbsent("glm-5.3-flash",
        k -> new Semaphore(Math.max(1, glmMaxConcurrency), true));
  }

  /**
   * 共享 HTTP 调用层 — 处理模型并发闸口排队 + 请求发送 + HTTP 级异常分类。
   * 响应日志沿用 200 字符截断（仅影响日志可读性，响应体本身完整返回）。
   *
   * @return 完整 LLM 响应 JsonNode（非 null）
   * @throws FaqLlmException HTTP 级异常（429/4xx/5xx/超时）；闸口排队被中断也按瞬时异常处理
   */
  private JsonNode executeChatCompletion(ObjectNode requestBody, String url, String baseUrl,
                                         String modelId, String apiKey, Long tenantId,
                                         RestClient targetClient) {
    String requestBodyStr;
    try {
      requestBodyStr = objectMapper.writeValueAsString(requestBody);
    } catch (Exception e) {
      requestBodyStr = requestBody.toString();
    }
    log.info("LLM 请求：tenantId={}, url={}, body={}", tenantId, url, requestBodyStr);

    // 模型并发闸口：超过上限的调用在此阻塞排队（公平信号量，先到先得），不丢不拒
    Semaphore gate = gateFor(baseUrl, modelId);
    if (gate != null) {
      long waitStart = System.currentTimeMillis();
      try {
        gate.acquire();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("LLM 并发闸口排队等待被中断：tenantId={}, modelId={}", tenantId, modelId);
        throw new FaqLlmTimeoutException("LLM 并发闸口排队等待被中断（编排终止/应用关闭）");
      }
      long waitedMs = System.currentTimeMillis() - waitStart;
      if (waitedMs > 1000) {
        log.info("LLM 并发闸口排队后获得名额：tenantId={}, modelId={}, waitedMs={}",
            tenantId, modelId, waitedMs);
      }
    }
    try {
      // 先取原始报文再手动解析（原始响应留痕）：链路截断/网关畸形时日志能看到实际收到的字节，
      // 而不是只有 Jackson 一句 EOF。空体与非 JSON 在 parseResponseBody 归为服务端瞬时异常。
      String rawBody = targetClient.post()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .body(requestBody)
          .retrieve()
          .body(String.class);

      JsonNode response = parseResponseBody(rawBody, tenantId, url);

      // 记录响应（截断到 200 字符，仅日志）
      String responseStr;
      try {
        responseStr = objectMapper.writeValueAsString(response);
      } catch (Exception e) {
        responseStr = response.toString();
      }
      if (responseStr.length() > 3000) {
        responseStr = responseStr.substring(0, 3000) + "...(truncated)";
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
      // 网络超时 / 连接异常 / 响应体解析失败（提供商返回 2xx 但空 body 或畸形 JSON，
      // 2026-10-13 DeepSeek 网关事故即此形态；必须带 rootCause+堆栈，否则只剩一句
      // "Error while extracting response..." 看不到是空响应还是截断）
      Throwable cause = ex.getCause();
      if (cause instanceof SocketTimeoutException) {
        log.error("LLM 网络超时：tenantId={}, url={}, error={}", tenantId, url, ex.getMessage());
        throw new FaqLlmTimeoutException("LLM 网络超时：" + ex.getMessage());
      }
      log.error("LLM 请求异常：tenantId={}, url={}, error={}, rootCause={}",
          tenantId, url, ex.getMessage(), rootCauseSummary(ex), ex);
      throw new FaqLlmTimeoutException("LLM 请求异常：" + ex.getMessage());
    } finally {
      if (gate != null) {
        gate.release();
      }
    }
  }

  // ──────────────────────── private helpers ────────────────────────

  /**
   * 原始报文 → JsonNode 校验解析：空响应体与非 JSON（截断、网关畸形页）归为
   * FaqLlmServerException（瞬时），并把实际收到的内容记入日志——2026-10-13/10-16
   * DeepSeek 链路截断事故（响应体只吐一个 "{" 即 EOF）复盘依赖此留痕；
   * 此前 .body(JsonNode.class) 直解，日志里看不到原始字节。
   */
  JsonNode parseResponseBody(String rawBody, Long tenantId, String url) {
    if (rawBody == null || rawBody.isBlank()) {
      log.error("LLM 返回空响应体（HTTP 2xx）：tenantId={}, url={}", tenantId, url);
      throw new FaqLlmServerException("LLM 返回空响应体（HTTP 2xx）");
    }
    try {
      return objectMapper.readTree(rawBody);
    } catch (JsonProcessingException e) {
      log.error("LLM 响应体非 JSON（疑似截断/网关畸形）：tenantId={}, url={}, rawBody={}",
          tenantId, url, abbreviateBody(rawBody), e);
      throw new FaqLlmServerException("LLM 响应体非 JSON（截断或畸形）");
    }
  }

  /** 最深层根因的一句话摘要（如 Jackson "No content to map due to end-of-input"），定位畸形响应体用 */
  private static String rootCauseSummary(RestClientException ex) {
    Throwable root = ex.getMostSpecificCause();
    return root != null && root != ex
        ? root.getClass().getSimpleName() + ": " + root.getMessage()
        : null;
  }

  /**
   * 根据 HTTP 状态码分类异常。
   * 429 或 GLM 限流错误码（1302 账户限流 / 1305 平台过载，可能在 4xx body 里）→ RateLimit（瞬时）；
   * 4xx → Client（确定性）；5xx → Server（瞬时）。message 已含截断的错误响应体。
   */
  private FaqLlmException classifyHttpException(int statusCode, String message, Long tenantId, String url) {
    log.error("LLM HTTP 错误：tenantId={}, status={}, url={}, message={}",
        tenantId, statusCode, url, message);
    if (statusCode == 429) {
      return new FaqLlmRateLimitException("LLM 限流（HTTP 429）");
    }
    if (containsGlmRateLimitCode(message)) {
      return new FaqLlmRateLimitException("LLM 限流（GLM 错误码 1302/1305）：" + abbreviateBody(message));
    }
    if (statusCode >= 400 && statusCode < 500) {
      return new FaqLlmClientException("LLM 客户端错误（HTTP " + statusCode + "）：" + message);
    }
    return new FaqLlmServerException("LLM 服务端错误（HTTP " + statusCode + "）：" + message);
  }

  /** GLM 限流/过载错误码识别（1302 账户速率限制、1305 平台过载，官方文档 /cn/api/rate-limit） */
  private boolean containsGlmRateLimitCode(String message) {
    return message != null && (message.contains("1302") || message.contains("1305"));
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

  /** 判断是否为智谱 GLM 模型（baseURL 含 bigmodel 关键词） */
  private boolean isGlm(String baseUrl) {
    return baseUrl != null && baseUrl.toLowerCase().contains("bigmodel");
  }

  /**
   * 写入 thinking 节点（2026-08-11 新增 reasoning_effort 支持；2026-09-04 新增 GLM 分支）。
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
   *   <li>GLM（bigmodel.cn，2026-09-04 理论试题生成接入）：thinking=enabled →
   *       {type:enabled} + 顶层 {@code reasoning_effort}（智谱 GLM-5.x 口径，实施时逐字段核对官方
   *       文档，若文档字段结构不同以文档为准，坑 #13）；thinking=disabled → 仅 {type:disabled}</li>
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
      return;
    }
    if (isGlm(baseUrl)) {
      ObjectNode thinkingNode = objectMapper.createObjectNode();
      thinkingNode.put("type", thinking);
      requestBody.set("thinking", thinkingNode);
      if ("enabled".equals(thinking) && reasoningEffort != null && !reasoningEffort.isBlank()) {
        // 智谱 GLM-5.x：reasoning_effort 为顶层字段
        requestBody.put("reasoning_effort", reasoningEffort);
      }
    }
  }
}
