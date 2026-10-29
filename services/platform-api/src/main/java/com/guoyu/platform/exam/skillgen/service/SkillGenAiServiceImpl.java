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
 *
 * <p>GLM 数组塌缩对策（2026-10-29）：json_object 只保证合法 JSON、不约束根为数组（官方无
 * json_schema/strict），模型可能把 5 题塌缩成 1 个试题对象；见 {@link #generateForPointWithRepair}。
 */
@Service
@Slf4j
public class SkillGenAiServiceImpl implements SkillGenAiService {

  /** 单块响应超时（秒）：thinking=high + max_tokens=16384 下单块输出体量大 */
  static final int RESPONSE_TIMEOUT_SECONDS = 180;

  /** 修正重试上限（不含首次调用，即最多 3 次 LLM 调用/块） */
  static final int MAX_REPAIR_ATTEMPTS = 2;

  /** 修正重试回喂失败输出的截断长度（控制 prompt 体积与成本） */
  static final int REPAIR_ECHO_MAX_CHARS = 4000;

  /** 对象包装数组的白名单键（仅识别明确字段，不扫描任意数组、不裸包单对象） */
  private static final List<String> ARRAY_WRAPPER_KEYS = List.of("questions", "data", "items", "result");

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
  public SkillGenGeneration generateForPointWithRepair(Long tenantId, String baseUrl, String apiKey, String modelId,
      String thinking, String reasoningEffort, int maxTokens,
      String systemPrompt, String userPrompt, int expectedCount) {
    String repairPrompt = null;
    long promptTokens = 0;
    long completionTokens = 0;
    long totalTokens = 0;
    long cachedTokens = 0;
    String lastError = "未知";
    for (int attempt = 1; attempt <= MAX_REPAIR_ATTEMPTS + 1; attempt++) {
      LlmChatCompletionResult result = generateForPoint(tenantId, baseUrl, apiKey, modelId,
          thinking, reasoningEffort, maxTokens, systemPrompt,
          repairPrompt == null ? userPrompt : repairPrompt);
      promptTokens += result.promptTokens();
      completionTokens += result.completionTokens();
      totalTokens += result.totalTokens();
      cachedTokens += result.cachedTokens();
      try {
        List<SkillQuestionItem> items = parseQuestions(result.content());
        if (items.size() == expectedCount) {
          return new SkillGenGeneration(items, result.content(), attempt,
              promptTokens, completionTokens, totalTokens, cachedTokens);
        }
        lastError = "数组元素个数 " + items.size() + " 与要求 " + expectedCount + " 不一致";
      } catch (BusinessException e) {
        lastError = e.getMessage();
      }
      if (attempt <= MAX_REPAIR_ATTEMPTS) {
        log.warn("模型输出形态不符（{}），触发修正重试：attempt={}, point 要求 {} 题",
            lastError, attempt, expectedCount);
        repairPrompt = buildRepairPrompt(userPrompt, result.content(), expectedCount, lastError);
      }
    }
    throw new BusinessException(ErrorCode.BAD_GATEWAY,
        "模型输出不是试题数组（已调用 " + (MAX_REPAIR_ATTEMPTS + 1) + " 次，最后错误：" + lastError + "）");
  }

  /** 修正重试 prompt：保留原始任务 + 回喂失败输出（截断）+ 明确输出契约 */
  private String buildRepairPrompt(String userPrompt, String failedContent, int expectedCount, String lastError) {
    return userPrompt
        + "\n\n【上次输出有误】" + lastError + "。\n"
        + "上次输出如下（超长已截断，仅作参考，不要照抄）：\n"
        + abbreviate(failedContent == null ? "(空)" : failedContent, REPAIR_ECHO_MAX_CHARS) + "\n"
        + "请重新完整输出：JSON 数组，恰好 " + expectedCount + " 个元素，每个元素一道完整试题；"
        + "第一个字符是 [，最后一个字符是 ]，不要输出任何其他文字、解释或 markdown 围栏。";
  }

  private static String abbreviate(String s, int maxChars) {
    if (s == null || s.length() <= maxChars) {
      return s == null ? "" : s;
    }
    return s.substring(0, maxChars) + "…(截断)";
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
        // 兼容对象包装数组：仅白名单键（questions/data/items/result），不扫描任意数组、不裸包单对象
        JsonNode inner = null;
        for (String key : ARRAY_WRAPPER_KEYS) {
          JsonNode candidate = node.get(key);
          if (candidate != null && candidate.isArray()) {
            inner = candidate;
            break;
          }
        }
        if (inner == null) {
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
