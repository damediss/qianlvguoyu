package com.guoyu.platform.exam.theorygen.service;

import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.exam.theorygen.dto.TheoryQuestionItem;
import java.util.List;

/**
 * 理论试题 LLM 生成领域接口（LLM 接入规范 #1：业务只依赖此接口，client 可 mock）。
 */
public interface TheoryGenAiService {

  /**
   * 按考评点生成一批试题（并发与成本优化方案 v0.3 P2：返回 usage 明细；响应超时 180s）。
   *
   * @param tenantId         租户（Worker 线程参数传递，禁止调 SecurityContext）
   * @param baseUrl          模型 base_url
   * @param apiKey           模型 api_key
   * @param modelId          模型 ID
   * @param thinking         thinking 快照（enabled/disabled/null）
   * @param reasoningEffort  推理强度快照（low/high/max/null）
   * @param maxTokens        max_tokens
   * @param systemPrompt     system 提示词
   * @param userPrompt       user 提示词（含考评点与配额）
   * @return content + usage 明细（由调用方解析校验并落库）
   */
  LlmChatCompletionResult generateForPoint(Long tenantId, String baseUrl, String apiKey, String modelId,
      String thinking, String reasoningEffort, int maxTokens,
      String systemPrompt, String userPrompt);

  /** 解析模型输出 JSON → 试题列表（解析失败抛 BusinessException，由调用方记失败） */
  List<TheoryQuestionItem> parseQuestions(String rawContent);
}
