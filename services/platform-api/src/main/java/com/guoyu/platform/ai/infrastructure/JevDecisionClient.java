package com.guoyu.platform.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.ai.dto.JevBatchDecisionResult;
import com.guoyu.platform.ai.dto.JevDecisionResult;
import com.guoyu.platform.ai.dto.JevQuestion;
import com.guoyu.platform.ai.dto.JevSubDecision;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Jev 决策客户端 — TokenDance 的 TypeSafe SystemOne 端点（POST {base_url}/typesafe/v1/systemone，
 * base_url 含 /gateway 前缀，实测锁定 https://tokendance.space/gateway）。
 *
 * <p>非 OpenAI 协议（方案 §4-4 实测确认），独立 client，不与 FaqLlmClient 共用超时/并发口径。
 * 请求：state + questions{key:{type:"choice",instructions,criteria}}；criteria 为 id→标题映射，
 * key 原样回传（实测）。响应：answers.key.{choice,confidence,probabilities} + usage + metadata。
 *
 * <p>错误分类（§5，401/422 已实测）：401 → {@link JevUnauthorizedException}（不重试）；
 * 422 → {@link JevValidationException}（不重试）；429/529/5xx/网络异常 → 限次退避重试
 * （遵循 Retry-After），超限 → {@link JevServerException}。criteria &lt; 2 项客户端前置拦截
 * （实测 min_length=2，空/单选项必 422）。
 *
 * <p>tenantId 由调用方经参数/上下文传入，本类不调 SecurityContext。
 */
@Slf4j
@Component
public class JevDecisionClient {

  private final ObjectMapper objectMapper;
  private final PoolingHttpClientConnectionManager connectionManager;
  /** readTimeout 秒数 → 独立 RequestConfig 的 RestClient（共享连接池，同 FaqLlmClient 分档模式） */
  private final Map<Integer, RestClient> timeoutClients = new ConcurrentHashMap<>();

  /** 连接超时（秒）。读超时按配置行走 jev_read_timeout_seconds 列（默认 30s：实测 e2e 5.6s@255 选项 + 网关余量） */
  @Value("${platform.knowledgefix.jev-connect-timeout-seconds:5}")
  int connectTimeoutSeconds = 5;
  /** 429/529/5xx 重试次数（不含首次） */
  @Value("${platform.knowledgefix.jev-max-retries:2}")
  int maxRetries = 2;
  /** 重试退避基数（毫秒），指数递增；有 Retry-After 时取二者较大值 */
  @Value("${platform.knowledgefix.jev-retry-backoff-millis:1000}")
  long retryBackoffMillis = 1000;

  public JevDecisionClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(20)
        .setMaxConnPerRoute(20)
        .build();
  }

  /**
   * 单问题 choice 判定。
   *
   * @param baseUrl           如 https://tokendance.space/gateway（含 /gateway 前缀）
   * @param questionKey       问题键（审计 stage 对应；如 l2/l3）
   * @param criteria          选项映射（key=业务 id 字符串如 nodeId，value=标题）；&lt;2 项前置拦截
   * @param readTimeoutSeconds 读超时（配置行 jev_read_timeout_seconds）
   */
  public JevDecisionResult decide(String baseUrl, String apiKey, String model, String state,
      String questionKey, String instructions, Map<String, String> criteria,
      int readTimeoutSeconds) {
    if (criteria == null || criteria.size() < 2) {
      // 实测（2026-09-27）：空 criteria → 422 "Dictionary should have at least 2 items"
      throw new JevValidationException("criteria 至少 2 个选项（接口实测 min_length=2），实际 "
          + (criteria == null ? 0 : criteria.size()));
    }
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", model);
    body.put("state", state);
    ObjectNode question = body.putObject("questions").putObject(questionKey);
    question.put("type", "choice");
    question.put("instructions", instructions);
    ObjectNode criteriaNode = question.putObject("criteria");
    criteria.forEach(criteriaNode::put);

    RestClient client = clientFor(readTimeoutSeconds);
    long t0 = System.currentTimeMillis();
    RuntimeException lastFailure = null;
    for (int attempt = 0; attempt <= Math.max(0, maxRetries); attempt++) {
      if (attempt > 0) {
        sleepBeforeRetry(attempt, lastFailure);
      }
      try {
        String resp = client.post()
            .uri(baseUrl + "/typesafe/v1/systemone")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body.toString())
            .retrieve()
            .body(String.class);
        JsonNode root = objectMapper.readTree(resp == null ? "" : resp);
        return parseResult(root, questionKey, state, (int) (System.currentTimeMillis() - t0));
      } catch (RestClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 401) {
          throw new JevUnauthorizedException("Jev 401 鉴权失败（API 密钥无效）: " + tail(e.getResponseBodyAsString()));
        }
        if (status == 422) {
          throw new JevValidationException("Jev 422 请求校验失败: " + tail(e.getResponseBodyAsString()));
        }
        if (status == 429 || status == 529 || status >= 500) {
          lastFailure = e;
          log.warn("[Jev] HTTP {} 第 {} 次失败，准备重试: {}", status, attempt + 1, tail(e.getResponseBodyAsString()));
          continue;
        }
        throw new JevServerException("Jev HTTP " + status + ": " + tail(e.getResponseBodyAsString()), e);
      } catch (RestClientException e) {
        lastFailure = e;
        log.warn("[Jev] 网络/超时失败（第 {} 次）: {}", attempt + 1, e.getMessage());
      } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        // HTTP 200 但 body 非合法 JSON：服务端故障，按可重试处理
        lastFailure = new JevServerException("Jev 响应非法 JSON: " + e.getMessage(), e);
        log.warn("[Jev] 响应非法 JSON（第 {} 次）: {}", attempt + 1, e.getMessage());
      }
    }
    throw new JevServerException("Jev 调用重试超限（" + (maxRetries + 1) + " 次）: "
        + (lastFailure == null ? "未知" : lastFailure.getMessage()), lastFailure);
  }

  /**
   * 多子问题 choice 判定（方案 §9）。questions 的 key 原样作为响应 answers 的 key。
   *
   * <p>响应零信任校验：缺子答案、choice 越界、confidence 非法、概率 key 集不一致、概率和异常、
   * choice 不在最高概率集合，均整批失败，不使用部分结果。
   */
  public JevBatchDecisionResult decideBatch(String baseUrl, String apiKey, String model,
      String state, Map<String, JevQuestion> questions, int readTimeoutSeconds) {
    if (questions == null || questions.isEmpty()) {
      throw new JevValidationException("questions 不能为空");
    }
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", model);
    body.put("state", state == null ? "" : state);
    ObjectNode questionsNode = body.putObject("questions");
    for (Map.Entry<String, JevQuestion> entry : questions.entrySet()) {
      JevQuestion question = entry.getValue();
      if (question == null || question.criteria() == null || question.criteria().size() < 2) {
        throw new JevValidationException(
            "子问题 " + entry.getKey() + " 的 criteria 至少 2 个选项");
      }
      if (question.criteria().size() > 255) {
        throw new JevValidationException("子问题 " + entry.getKey() + " 的 criteria 超过 255 项");
      }
      ObjectNode node = questionsNode.putObject(entry.getKey());
      node.put("type", "choice");
      node.put("instructions", question.instructions() == null ? "" : question.instructions());
      ObjectNode criteriaNode = node.putObject("criteria");
      question.criteria().forEach(criteriaNode::put);
    }
    String requestBody = body.toString();
    RestClient client = clientFor(readTimeoutSeconds);
    long t0 = System.currentTimeMillis();
    RuntimeException lastFailure = null;
    for (int attempt = 0; attempt <= Math.max(0, maxRetries); attempt++) {
      if (attempt > 0) {
        sleepBeforeRetry(attempt, lastFailure);
      }
      try {
        String resp = client.post()
            .uri(baseUrl + "/typesafe/v1/systemone")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .body(String.class);
        JsonNode root = objectMapper.readTree(resp == null ? "" : resp);
        return parseBatch(root, questions, requestBody, (int) (System.currentTimeMillis() - t0));
      } catch (RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();
        if (status == 401) {
          throw new JevUnauthorizedException("Jev 401 鉴权失败（API 密钥无效）: " + tail(responseBody));
        }
        if (status == 422) {
          if (responseBody != null && (responseBody.contains("token_budget")
              || responseBody.contains("budget_exceeded") || responseBody.contains("input_tokens"))) {
            throw new JevBudgetExceededException("内容超出模型输入上限: " + tail(responseBody));
          }
          throw new JevValidationException("Jev 422 请求校验失败: " + tail(responseBody));
        }
        if (status == 429 || status == 529 || status >= 500) {
          lastFailure = e;
          log.warn("[Jev] HTTP {} 第 {} 次失败，准备重试: {}", status, attempt + 1, tail(responseBody));
          continue;
        }
        throw new JevServerException("Jev HTTP " + status + ": " + tail(responseBody), e);
      } catch (RestClientException e) {
        lastFailure = e;
        log.warn("[Jev] 网络/超时失败（第 {} 次）: {}", attempt + 1, e.getMessage());
      } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        lastFailure = new JevServerException("Jev 响应非法 JSON: " + e.getMessage(), e);
        log.warn("[Jev] 响应非法 JSON（第 {} 次）: {}", attempt + 1, e.getMessage());
      }
    }
    throw new JevServerException("Jev 调用重试超限（" + (maxRetries + 1) + " 次）: "
        + (lastFailure == null ? "未知" : lastFailure.getMessage()), lastFailure);
  }

  private JevBatchDecisionResult parseBatch(JsonNode root, Map<String, JevQuestion> questions,
      String requestBody, int durationMs) {
    JsonNode answers = root.path("answers");
    if (!answers.isObject()) {
      throw new JevParseException("Jev 响应缺少 answers 对象");
    }
    Map<String, JevSubDecision> parsed = new LinkedHashMap<>();
    for (Map.Entry<String, JevQuestion> entry : questions.entrySet()) {
      String key = entry.getKey();
      JsonNode answer = answers.path(key);
      if (!answer.isObject()) {
        throw new JevParseException("Jev 响应缺少子答案 " + key);
      }
      String choice = answer.path("choice").asText("");
      if (choice.isBlank()) {
        throw new JevParseException("Jev 子答案 " + key + " 的 choice 为空");
      }
      Map<String, String> criteria = entry.getValue().criteria();
      if (!criteria.containsKey(choice)) {
        throw new JevParseException("Jev 子答案 " + key + " 的 choice 越界: " + choice);
      }
      JsonNode confNode = answer.path("confidence");
      if (!confNode.isNumber()) {
        throw new JevParseException("Jev 子答案 " + key + " 的 confidence 非数值");
      }
      double confidence = confNode.asDouble();
      if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
        throw new JevParseException("Jev 子答案 " + key + " 的 confidence 越界: " + confidence);
      }
      JsonNode probsNode = answer.path("probabilities");
      if (!probsNode.isObject() || probsNode.isEmpty()) {
        throw new JevParseException("Jev 子答案 " + key + " 的 probabilities 缺失");
      }
      Map<String, Double> probabilities = new LinkedHashMap<>();
      Iterator<Map.Entry<String, JsonNode>> it = probsNode.fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> en = it.next();
        if (!en.getValue().isNumber()) {
          throw new JevParseException("Jev 子答案 " + key + " 的概率非数值: " + en.getKey());
        }
        double value = en.getValue().asDouble();
        if (!Double.isFinite(value) || value < 0 || value > 1) {
          throw new JevParseException("Jev 子答案 " + key + " 的概率越界: " + en.getKey());
        }
        probabilities.put(en.getKey(), value);
      }
      if (!probabilities.keySet().equals(criteria.keySet())) {
        throw new JevParseException("Jev 子答案 " + key + " 的概率 key 集与 criteria 不一致");
      }
      double sum = probabilities.values().stream().mapToDouble(Double::doubleValue).sum();
      if (Math.abs(sum - 1.0) > 0.01) {
        throw new JevParseException("Jev 子答案 " + key + " 的概率和异常: " + sum);
      }
      List<Map.Entry<String, Double>> sorted = new ArrayList<>(probabilities.entrySet());
      sorted.sort(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()));
      double top1 = sorted.get(0).getValue();
      double top2 = sorted.size() > 1 ? sorted.get(1).getValue() : 0.0;
      double choiceProb = probabilities.get(choice);
      if (choiceProb + 1e-9 < top1) {
        throw new JevParseException(
            "Jev 子答案 " + key + " 的 choice 不在最高概率集合（choice=" + choice + "）");
      }
      double margin = Math.max(0.0, choiceProb - top2);
      parsed.put(key, new JevSubDecision(choice, confidence, probabilities, margin, top1, top2));
    }

    JsonNode usage = root.path("usage");
    long inputTokens;
    long outputTokens;
    boolean usageEstimated = false;
    JsonNode inputNode = usage.path("input_tokens");
    JsonNode outputNode = usage.path("output_tokens");
    if (inputNode.isNumber() && outputNode.isNumber() && inputNode.asDouble() >= 0
        && outputNode.asDouble() >= 0 && inputNode.asDouble() == Math.rint(inputNode.asDouble())
        && outputNode.asDouble() == Math.rint(outputNode.asDouble())) {
      inputTokens = inputNode.asLong();
      outputTokens = outputNode.asLong();
    } else if (!inputNode.isMissingNode() || !outputNode.isMissingNode()) {
      throw new JevParseException("Jev usage token 非法");
    } else {
      usageEstimated = true;
      inputTokens = Math.max(1, (int) Math.ceil(requestBody.length() / 3.0));
      outputTokens = 0;
      log.warn("[Jev] 响应缺 usage，按完整请求体估算 inputTokens={}（estimated）", inputTokens);
    }
    String responseModel = root.path("model").isTextual() ? root.path("model").asText() : null;
    String confidenceDefinition = root.path("metadata").path("confidence_definition").asText(null);
    String requestId = root.path("request_id").isTextual() ? root.path("request_id").asText() : null;
    return new JevBatchDecisionResult(parsed, inputTokens, outputTokens, usageEstimated, responseModel,
        confidenceDefinition, requestId, durationMs);
  }

  /** 解析响应（零信任，规范 #5）：answers.key.{choice,confidence,probabilities} + usage 必需；metadata 容缺失 */
  private JevDecisionResult parseResult(JsonNode root, String questionKey, String state,
      int durationMs) {
    JsonNode answer = root.path("answers").path(questionKey);
    if (!answer.isObject()) {
      throw new JevParseException("Jev 响应缺少 answers." + questionKey);
    }
    String choice = answer.path("choice").asText("");
    if (choice.isBlank()) {
      throw new JevParseException("Jev 响应 choice 为空");
    }
    JsonNode confNode = answer.path("confidence");
    if (!confNode.isNumber()) {
      throw new JevParseException("Jev 响应 confidence 非数值");
    }
    double confidence = confNode.asDouble();
    if (confidence < 0 || confidence > 1) {
      throw new JevParseException("Jev confidence 越界: " + confidence);
    }
    JsonNode probsNode = answer.path("probabilities");
    if (!probsNode.isObject() || probsNode.isEmpty()) {
      throw new JevParseException("Jev 响应 probabilities 缺失或为空");
    }
    Map<String, Double> probabilities = new LinkedHashMap<>();
    Iterator<Map.Entry<String, JsonNode>> it = probsNode.fields();
    while (it.hasNext()) {
      Map.Entry<String, JsonNode> en = it.next();
      if (!en.getValue().isNumber()) {
        throw new JevParseException("Jev probabilities[" + en.getKey() + "] 非数值");
      }
      probabilities.put(en.getKey(), en.getValue().asDouble());
    }
    JsonNode usage = root.path("usage");
    long inputTokens;
    long outputTokens;
    boolean usageEstimated = false;
    if (usage.path("input_tokens").isNumber()) {
      inputTokens = usage.path("input_tokens").asLong();
      outputTokens = usage.path("output_tokens").asLong(0);
    } else {
      // 防御分支（§11.1）：实测响应含 usage，此处兜底按字符估算并标记
      usageEstimated = true;
      inputTokens = Math.max(1, state.length() / 3);
      outputTokens = 0;
      log.warn("[Jev] 响应缺 usage，按字符估算 inputTokens={}（estimated）", inputTokens);
    }
    String confidenceDefinition = root.path("metadata").path("confidence_definition").asText(null);
    String responseModel = root.path("model").asText(null);
    return new JevDecisionResult(choice, confidence, probabilities, inputTokens, outputTokens,
        confidenceDefinition, responseModel, durationMs, usageEstimated);
  }

  private RestClient clientFor(int readTimeoutSeconds) {
    int seconds = Math.max(1, readTimeoutSeconds);
    return timeoutClients.computeIfAbsent(seconds, s -> {
      // 同 FaqLlmClient 分档模式：各档共享连接池，仅 RequestConfig 不同
      org.apache.hc.client5.http.config.RequestConfig requestConfig =
          org.apache.hc.client5.http.config.RequestConfig.custom()
              .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(connectTimeoutSeconds))
              .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(s))
              .build();
      org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient =
          org.apache.hc.client5.http.impl.classic.HttpClients.custom()
              .setConnectionManager(connectionManager)
              .setDefaultRequestConfig(requestConfig)
              // 禁用内容压缩：避免 Brotli/压缩响应单字节读取缺陷（方案 §9.4）
              .disableContentCompression()
              .build();
      return RestClient.builder()
          .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
          .build();
    });
  }

  private void sleepBeforeRetry(int attempt, RuntimeException lastFailure) {
    long millis = retryBackoffMillis * attempt;
    if (lastFailure instanceof RestClientResponseException e && e.getResponseHeaders() != null) {
      String retryAfter = e.getResponseHeaders().getFirst("Retry-After");
      if (retryAfter != null && !retryAfter.isBlank()) {
        try {
          millis = Math.max(millis, Long.parseLong(retryAfter.trim()) * 1000L);
        } catch (NumberFormatException ignore) {
          // 非数字 Retry-After（HTTP-date 形态）退回指数退避
        }
      }
    }
    millis = Math.min(millis, 10_000L);
    try {
      Thread.sleep(Math.max(0, millis));
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new JevServerException("Jev 重试等待被中断", ie);
    }
  }

  private String tail(String s) {
    if (s == null) {
      return "";
    }
    return s.length() > 300 ? s.substring(s.length() - 300) : s;
  }
}
