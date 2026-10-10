package com.guoyu.platform.exam.theorygen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.theorygen.dto.TheoryQuestionItem;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 理论试题生成 AI 服务实现。
 * 复用 FaqLlmClient（OpenAI 兼容、显式超时、平台分支传参）；输出零信任：解析失败按失败处理。
 * 超时口径（优化方案 v0.3 决策 #2）：GLM thinking 长调用，响应超时 180s。
 */
@Service
@Slf4j
public class TheoryGenAiServiceImpl implements TheoryGenAiService {

  /** 理论生成单块响应超时（秒）：thinking=enabled + max_tokens=8192 下 90s 偏紧（R5） */
  static final int RESPONSE_TIMEOUT_SECONDS = 180;

  private final FaqLlmClient llmClient;
  private final ObjectMapper objectMapper;

  public TheoryGenAiServiceImpl(FaqLlmClient llmClient, ObjectMapper objectMapper) {
    this.llmClient = llmClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public LlmChatCompletionResult generateForPoint(Long tenantId, String baseUrl, String apiKey, String modelId,
      String thinking, String reasoningEffort, int maxTokens,
      String systemPrompt, String userPrompt) {
    // GLM 的 reasoning_effort 传参口径在 FaqLlmClient.putThinkingNode 的 GLM 分支中处理；
    // 此处透传 thinking/reasoningEffort（批次启动时从配置表读取固化，非提交时快照）。
    // jsonMode=true：智谱官方支持 response_format json_object（struct-output 文档 2026-09-08 核对），
    // 从结构上保证合法 JSON，消除约 4% 的输出语法滑步；若 GLM 对 thinking+json_object 组合报错，
    // 块会显式失败暴露于试产，回退只需改回 false。
    return llmClient.chatCompletionDetailed(baseUrl, apiKey, modelId,
        systemPrompt, userPrompt, maxTokens, thinking, reasoningEffort, true, tenantId,
        RESPONSE_TIMEOUT_SECONDS);
  }

  @Override
  public List<TheoryQuestionItem> parseQuestions(String rawContent) {
    if (rawContent == null || rawContent.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型返回空内容");
    }
    String json = extractJsonArray(rawContent);
    List<TheoryQuestionItem> items;
    try {
      items = new ArrayList<>();
      JsonNode arr = objectMapper.readTree(json);
      if (!arr.isArray()) {
        // 兼容 {"questions":[...]} 包裹
        JsonNode inner = arr.get("questions");
        if (inner == null || !inner.isArray()) {
          throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出不是试题数组");
        }
        arr = inner;
      }
      for (JsonNode n : arr) {
        items.add(objectMapper.treeToValue(n, TheoryQuestionItem.class));
      }
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出 JSON 解析失败: " + e.getMessage());
    }
    if (items.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出试题数组为空");
    }
    return items;
  }

  /** 从模型输出中提取 JSON 数组文本（剥 ```json 围栏/前后杂文字） */
  private String extractJsonArray(String raw) {
    String t = raw.trim();
    if (t.startsWith("```")) {
      int firstNl = t.indexOf('\n');
      if (firstNl > 0) {
        t = t.substring(firstNl + 1);
      }
      int fenceEnd = t.lastIndexOf("```");
      if (fenceEnd >= 0) {
        t = t.substring(0, fenceEnd);
      }
      t = t.trim();
    }
    int arrStart = t.indexOf('[');
    int objStart = t.indexOf('{');
    int start = (arrStart >= 0 && (objStart < 0 || arrStart < objStart)) ? arrStart : objStart;
    if (start < 0) {
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出中未找到 JSON");
    }
    int arrEnd = t.lastIndexOf(']');
    int objEnd = t.lastIndexOf('}');
    int end = Math.max(arrEnd, objEnd);
    if (end <= start) {
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出 JSON 不完整");
    }
    return t.substring(start, end + 1);
  }
}
