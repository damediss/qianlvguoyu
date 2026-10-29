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
 * 复用 FaqLlmClient（OpenAI 兼容、显式超时、平台分支传参、jsonMode）；输出零信任：解析/校验失败按失败处理。
 * 超时口径：GLM thinking 长调用 + 三件套输出，单题响应超时 180s。
 *
 * <p>单题拆分（2026-10-29 真实探针定案）：GLM-5.3-flash 对「一次输出 N 题 JSON 数组」的任务结构理解不稳定，
 * 会间歇性把 5 题塌缩成 1 个试题对象（json_object 只保证合法 JSON、不约束根为数组，官方无 json_schema/strict），
 * 而单题模式 100% 成功。故 {@link #generateForPointWithRepair} 改为逐题串行生成：
 * 每次只要求 1 个 JSON 对象，逐题解析 + 规则校验 + 查重，失败仅重试该题。
 */
@Service
@Slf4j
public class SkillGenAiServiceImpl implements SkillGenAiService {

  /** 单次响应超时（秒）：thinking + 大 max_tokens 下单题输出体量仍可达数千 token */
  static final int RESPONSE_TIMEOUT_SECONDS = 180;

  /** 单题修正重试上限（不含首次调用，即每题最多 3 次 LLM 调用） */
  static final int MAX_REPAIR_ATTEMPTS = 2;

  /** 修正重试回喂失败输出的截断长度（控制 prompt 体积与成本） */
  static final int REPAIR_ECHO_MAX_CHARS = 4000;

  /** 对象包装数组的白名单键（仅识别明确字段，不扫描任意数组） */
  private static final List<String> ARRAY_WRAPPER_KEYS = List.of("questions", "data", "items", "result");

  private final FaqLlmClient llmClient;
  private final ObjectMapper objectMapper;
  private final SkillQuestionValidator validator;

  public SkillGenAiServiceImpl(FaqLlmClient llmClient, ObjectMapper objectMapper,
      SkillQuestionValidator validator) {
    this.llmClient = llmClient;
    this.objectMapper = objectMapper;
    this.validator = validator;
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
    if (expectedCount <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "要求生成的试题道数必须大于 0");
    }
    List<SkillQuestionItem> items = new ArrayList<>(expectedCount);
    Usage usage = new Usage();
    String lastContent = null;
    for (int index = 1; index <= expectedCount; index++) {
      SingleOutcome outcome = generateSingleQuestion(tenantId, baseUrl, apiKey, modelId, thinking,
          reasoningEffort, maxTokens, systemPrompt, userPrompt, items, index, expectedCount, usage);
      items.add(outcome.item());
      lastContent = outcome.content();
    }
    return new SkillGenGeneration(List.copyOf(items), lastContent, usage.calls,
        usage.promptTokens, usage.completionTokens, usage.totalTokens, usage.cachedTokens);
  }

  /**
   * 生成第 index 题：解析 + 单题规则校验 + 与已生成题查重，失败回喂重试（该题最多 3 次调用）。
   *
   * @param existing 已生成并通过校验的题目（用于拼「已生成题名」提示与查重）
   */
  private SingleOutcome generateSingleQuestion(Long tenantId, String baseUrl, String apiKey, String modelId,
      String thinking, String reasoningEffort, int maxTokens, String systemPrompt,
      String baseInfoPrompt, List<SkillQuestionItem> existing, int index, int total, Usage usage) {
    List<String> generatedNames = new ArrayList<>(existing.size());
    for (SkillQuestionItem e : existing) {
      if (e.getName() != null && !e.getName().isBlank()) {
        generatedNames.add(e.getName().trim());
      }
    }
    String singlePrompt = SkillGenPromptBuilder.buildSingleUserPrompt(
        baseInfoPrompt, index, total, generatedNames);
    String prompt = singlePrompt;
    String lastError = "未知";
    String failedContent = null;
    for (int attempt = 1; attempt <= MAX_REPAIR_ATTEMPTS + 1; attempt++) {
      LlmChatCompletionResult result = generateForPoint(tenantId, baseUrl, apiKey, modelId,
          thinking, reasoningEffort, maxTokens, systemPrompt, prompt);
      usage.calls++;
      usage.promptTokens += result.promptTokens();
      usage.completionTokens += result.completionTokens();
      usage.totalTokens += result.totalTokens();
      usage.cachedTokens += result.cachedTokens();
      try {
        SkillQuestionItem item = parseSingleQuestion(result.content());
        List<String> errors = new ArrayList<>(validator.validate(item));
        List<SkillQuestionItem> probe = new ArrayList<>(existing);
        probe.add(item);
        errors.addAll(validator.checkDuplicates(probe));
        if (errors.isEmpty()) {
          return new SingleOutcome(item, result.content());
        }
        lastError = String.join("；", errors);
      } catch (BusinessException e) {
        lastError = e.getMessage();
      }
      failedContent = result.content();
      if (attempt <= MAX_REPAIR_ATTEMPTS) {
        log.warn("第 {} 题输出不合规（{}），触发修正重试：attempt={}, point 总题数 {}",
            index, lastError, attempt, total);
        prompt = buildRepairPrompt(singlePrompt, failedContent, lastError, generatedNames);
      }
    }
    throw new BusinessException(ErrorCode.BAD_GATEWAY,
        "第 " + index + " 题生成失败：" + lastError + "（该题已调用 " + (MAX_REPAIR_ATTEMPTS + 1) + " 次）");
  }

  /** 单题修正重试 prompt：原题任务 + 失败原因 + 回喂失败输出（截断）+ 单对象契约 */
  private String buildRepairPrompt(String singlePrompt, String failedContent, String lastError,
      List<String> generatedNames) {
    StringBuilder sb = new StringBuilder(singlePrompt);
    sb.append("\n\n【上次输出有误】").append(lastError).append("。\n")
        .append("上次输出如下（超长已截断，仅作参考，不要照抄）：\n")
        .append(abbreviate(failedContent == null ? "(空)" : failedContent, REPAIR_ECHO_MAX_CHARS)).append("\n");
    if (generatedNames != null && !generatedNames.isEmpty()) {
      sb.append("已生成试题名称（禁止重复或同义改述）：")
          .append(String.join("、", generatedNames)).append("\n");
    }
    sb.append("请重新输出 1 个 JSON 对象（该题本体），不要数组、不要其他文字、不要 markdown 围栏。");
    return sb.toString();
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

  @Override
  public SkillQuestionItem parseSingleQuestion(String rawContent) {
    if (rawContent == null || rawContent.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型返回空内容");
    }
    String json = extractJson(rawContent);
    try {
      JsonNode node = objectMapper.readTree(json);
      if (node.isArray()) {
        if (node.size() != 1) {
          throw new BusinessException(ErrorCode.BAD_GATEWAY,
              "模型输出不是单题对象（数组元素个数 " + node.size() + "）");
        }
        node = node.get(0);
      } else if (node.isObject()) {
        // 白名单包装对象：取数组第 1 个；无白名单数组键则视为单题本体
        JsonNode inner = null;
        for (String key : ARRAY_WRAPPER_KEYS) {
          JsonNode candidate = node.get(key);
          if (candidate != null && candidate.isArray()) {
            inner = candidate;
            break;
          }
        }
        if (inner != null) {
          if (inner.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_GATEWAY,
                "模型输出不是单题对象（包装数组为空）");
          }
          node = inner.get(0);
        }
      } else {
        throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出不是单题对象");
      }
      if (!node.isObject()) {
        throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出不是单题对象");
      }
      return objectMapper.treeToValue(node, SkillQuestionItem.class);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出 JSON 解析失败: " + e.getMessage());
    }
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

  /** 单题生成结果（题目 + 该题末次成功输出，供审计） */
  private record SingleOutcome(SkillQuestionItem item, String content) {
  }

  /** 全点调用与 usage 累计（跨题、跨重试） */
  private static final class Usage {
    private int calls;
    private long promptTokens;
    private long completionTokens;
    private long totalTokens;
    private long cachedTokens;
  }
}
