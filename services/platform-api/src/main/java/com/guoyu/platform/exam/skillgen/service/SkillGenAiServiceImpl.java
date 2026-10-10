package com.guoyu.platform.exam.skillgen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作技能试题生成 AI 服务实现。
 * 复用 FaqLlmClient（OpenAI 兼容、显式超时、平台分支传参、jsonMode）；输出零信任：解析失败按失败处理。
 * 超时口径：GLM thinking（effort=high）长调用 + 三件套大输出，响应超时 180s。
 */
@Service
@Slf4j
public class SkillGenAiServiceImpl implements SkillGenAiService {

  /** 单块响应超时（秒）：thinking=high + max_tokens=16384 下单块输出体量大 */
  static final int RESPONSE_TIMEOUT_SECONDS = 180;

  private final FaqLlmClient llmClient;
  private final ObjectMapper objectMapper;

  public SkillGenAiServiceImpl(FaqLlmClient llmClient, ObjectMapper objectMapper) {
    this.llmClient = llmClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public LlmChatCompletionResult generateForPoint(Long tenantId, String baseUrl, String apiKey, String modelId,
      String thinking, String reasoningEffort, int maxTokens,
      String systemPrompt, String userPrompt) {
    // jsonMode=true：智谱官方支持 response_format json_object（理论链路 2026-09-08 核对并生产验证）
    return llmClient.chatCompletionDetailed(baseUrl, apiKey, modelId,
        systemPrompt, userPrompt, maxTokens, thinking, reasoningEffort, true, tenantId,
        RESPONSE_TIMEOUT_SECONDS);
  }

  @Override
  public List<SkillQuestionItem> parseQuestions(String rawContent) {
    if (rawContent == null || rawContent.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型返回空内容");
    }
    String json = extractJson(rawContent);
    List<SkillQuestionItem> items;
    try {
      items = new ArrayList<>();
      JsonNode node = objectMapper.readTree(json);
      if (!node.isArray()) {
        // 兼容 {"questions":[...]} 包裹
        JsonNode inner = node.get("questions");
        if (inner == null || !inner.isArray()) {
          throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出不是试题数组");
        }
        node = inner;
      }
      for (JsonNode n : node) {
        items.add(objectMapper.treeToValue(n, SkillQuestionItem.class));
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

  /** 从模型输出中提取 JSON 文本（剥 ```json 围栏/前后杂文字） */
  private String extractJson(String raw) {
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
