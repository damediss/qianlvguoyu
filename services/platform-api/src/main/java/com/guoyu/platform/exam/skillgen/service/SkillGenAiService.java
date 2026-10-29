package com.guoyu.platform.exam.skillgen.service;

import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem;
import java.util.List;

/**
 * 操作技能试题 LLM 生成领域接口（LLM 接入规范 #1：业务只依赖此接口，client 可 mock）。
 */
public interface SkillGenAiService {

  /**
   * 单次 LLM 调用（响应超时 180s）。单题拆分链路下每次只产出一题。
   *
   * @param tenantId        租户（Worker 线程参数传递，禁止调 SecurityContext）
   * @param baseUrl         模型 base_url
   * @param apiKey          模型 api_key
   * @param modelId         模型 ID
   * @param thinking        thinking 档位（批次启动时从配置表读取固化）
   * @param reasoningEffort 推理强度（批次启动时从配置表读取固化）
   * @param maxTokens       max_tokens
   * @param systemPrompt    system 提示词
   * @param userPrompt      user 提示词
   * @return content + usage 明细
   */
  LlmChatCompletionResult generateForPoint(Long tenantId, String baseUrl, String apiKey, String modelId,
      String thinking, String reasoningEffort, int maxTokens,
      String systemPrompt, String userPrompt);

  /**
   * 生成整点题组（单题拆分，2026-10-29）：对第 1..expectedCount 题**串行**各调 1 次 LLM，
   * 每次只要求模型输出 1 个 JSON 对象（GLM-5.3-flash 最稳定形态），从根上规避数组塌缩。
   *
   * <p>每题即时解析 + 规则校验 + 与已生成题查重；失败则该题回喂失败输出重试，
   * 最多 2 次（该题含首次共 3 次调用）；任一题耗尽即抛
   * BusinessException（message 指明第几题），由调用方记失败并计入熔断。
   *
   * <p><b>userPrompt 语义</b>：作为「考评点基础信息模板」传入（{@code SkillGenPromptBuilder.buildBaseUserPrompt}），
   * 实现内部按题序追加「第 n 题 + 已生成题名」指令（{@code buildSingleUserPrompt}）。
   * 传入旧的 5 题数组版 prompt 会与单对象契约冲突，调用方不要再传。
   *
   * @param expectedCount 要求试题道数（当前固定 5）
   * @return items（已逐题校验通过）+ content（末次成功输出，仅审计）+ attempts（**总 LLM 调用次数**）+ usage 累计
   */
  SkillGenGeneration generateForPointWithRepair(Long tenantId, String baseUrl, String apiKey, String modelId,
      String thinking, String reasoningEffort, int maxTokens,
      String systemPrompt, String userPrompt, int expectedCount);

  /** 解析模型输出 JSON 数组 → 试题列表（数组模式，保留兼容；解析失败抛 BusinessException） */
  List<SkillQuestionItem> parseQuestions(String rawContent);

  /**
   * 解析模型输出 JSON → 单道试题（单题拆分主路径；解析失败抛 BusinessException）。
   * 接受：单对象（主形态）、1 元素数组、白名单包装对象（questions/data/items/result 取第 1 个）。
   */
  SkillQuestionItem parseSingleQuestion(String rawContent);

  /** 生成结果：items + content + attempts（总调用次数）+ usage 累计（含全部重试） */
  record SkillGenGeneration(List<SkillQuestionItem> items, String content, int attempts,
      long promptTokens, long completionTokens, long totalTokens, long cachedTokens) {
  }
}
