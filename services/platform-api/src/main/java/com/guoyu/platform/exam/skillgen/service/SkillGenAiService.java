package com.guoyu.platform.exam.skillgen.service;

import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem;
import java.util.List;

/**
 * 操作技能试题 LLM 生成领域接口（LLM 接入规范 #1：业务只依赖此接口，client 可 mock）。
 */
public interface SkillGenAiService {

  /**
   * 按考评点生成一批试题（1 考评点 = 1 次调用，产出该点全部 5 题；响应超时 180s）。
   *
   * @param tenantId        租户（Worker 线程参数传递，禁止调 SecurityContext）
   * @param baseUrl         模型 base_url
   * @param apiKey          模型 api_key
   * @param modelId         模型 ID
   * @param thinking        thinking 档位（批次启动时从配置表读取固化）
   * @param reasoningEffort 推理强度（批次启动时从配置表读取固化）
   * @param maxTokens       max_tokens
   * @param systemPrompt    system 提示词
   * @param userPrompt      user 提示词（含考评点与题量）
   * @return content + usage 明细（由调用方解析校验并落库）
   */
  LlmChatCompletionResult generateForPoint(Long tenantId, String baseUrl, String apiKey, String modelId,
      String thinking, String reasoningEffort, int maxTokens,
      String systemPrompt, String userPrompt);

  /**
   * 生成 + 解析 + 修正重试（GLM 数组塌缩对策，2026-10-29）：
   * json_object 只保证输出合法 JSON、不约束根为数组（GLM 官方文档无 json_schema/strict），
   * 模型可能把 5 题塌缩成 1 个试题对象。首次输出形态不符时把失败输出回喂做修正重试，
   * 最多 {@value #MAX_REPAIR_ATTEMPTS} 次（含首次共 3 次调用）；usage 按全部尝试累计。
   * 最终仍失败抛 BusinessException，由调用方记失败并计入熔断。
   *
   * @param expectedCount 要求试题道数（当前固定 5）
   * @return items（已解析）+ content（末次成功输出）+ attempts + usage 累计
   */
  SkillGenGeneration generateForPointWithRepair(Long tenantId, String baseUrl, String apiKey, String modelId,
      String thinking, String reasoningEffort, int maxTokens,
      String systemPrompt, String userPrompt, int expectedCount);

  /** 解析模型输出 JSON → 试题列表（解析失败抛 BusinessException，由调用方记失败） */
  List<SkillQuestionItem> parseQuestions(String rawContent);

  /** 生成结果：items + content + attempts + usage 累计（usage 含全部尝试） */
  record SkillGenGeneration(List<SkillQuestionItem> items, String content, int attempts,
      long promptTokens, long completionTokens, long totalTokens, long cachedTokens) {
  }
}
