package com.guoyu.platform.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.net.http.HttpClient;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * LLM 客户端 — 走 OpenAI 兼容的 /chat/completions 端点。
 * 支持 "openai_compatible" 格式（DeepSeek、LongCat 等）。
 *
 * 注意：tenantId 由调用方通过参数传入，整个调用链不调 SecurityContext。
 */
@Slf4j
@Component
public class FaqLlmClient {

  private final ObjectMapper objectMapper;

  public FaqLlmClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * 调用 LLM 生成 similar_questions 或 negative_questions。
   *
   * @param baseUrl     API Base URL（不含 /chat/completions 后缀）
   * @param apiKey      API Key
   * @param modelId     模型 ID
   * @param fullPrompt  完整 prompt（含 system + user，由 FaqLlmPromptBuilder 组装）
   * @param maxTokens   最大输出 token
   * @param tenantId    租户 ID（仅用于日志）
   * @return LLM 返回的 JSON 字符串（raw content）
   */
  public String chatCompletion(String baseUrl, String apiKey, String modelId,
                               String fullPrompt, int maxTokens, Long tenantId) {
    String url = trimTrailingSlash(baseUrl) + "/chat/completions";

    // 构造 OpenAI 兼容请求体
    // 注意：LongCat 不支持 response_format 字段，不要传
    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("model", modelId);
    requestBody.put("max_tokens", maxTokens);
    requestBody.put("temperature", 0.7);

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

    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(40));
    RestClient client = RestClient.builder()
        .requestFactory(requestFactory)
        .build();

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
        throw new BusinessException(ErrorCode.BAD_GATEWAY, "LLM 返回空响应");
      }

      // 记录完整响应
      String responseStr;
      try {
        responseStr = objectMapper.writeValueAsString(response);
      } catch (Exception e) {
        responseStr = response.toString();
      }
      // 截断超长响应避免日志爆炸
      if (responseStr.length() > 2000) {
        responseStr = responseStr.substring(0, 2000) + "...(truncated)";
      }
      log.info("FAQ LLM 响应：tenantId={}, model={}, body={}", tenantId, modelId, responseStr);

      // 提取 choices[0].message.content
      JsonNode choices = response.path("choices");
      if (!choices.isArray() || choices.isEmpty()) {
        log.error("FAQ LLM 响应缺少 choices：tenantId={}, body={}", tenantId, responseStr);
        throw new BusinessException(ErrorCode.BAD_GATEWAY, "LLM 响应缺少 choices");
      }
      String content = choices.get(0).path("message").path("content").asText(null);
      if (content == null || content.isBlank()) {
        log.error("FAQ LLM 响应 content 为空：tenantId={}, body={}", tenantId, responseStr);
        throw new BusinessException(ErrorCode.BAD_GATEWAY, "LLM 响应 content 为空");
      }
      return content;
    } catch (RestClientResponseException ex) {
      log.error("FAQ LLM HTTP 错误：tenantId={}, status={}, url={}, body={}",
          tenantId, ex.getStatusCode().value(), url, ex.getResponseBodyAsString());
      throw new BusinessException(ErrorCode.BAD_GATEWAY,
          "LLM 调用失败（HTTP " + ex.getStatusCode().value() + "）");
    } catch (BusinessException ex) {
      throw ex;
    } catch (RestClientException ex) {
      log.error("FAQ LLM 请求异常：tenantId={}, url={}, error={}", tenantId, url, ex.getMessage());
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "LLM 请求异常：" + ex.getMessage());
    }
  }

  private String trimTrailingSlash(String value) {
    String result = value.trim();
    while (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }
}
