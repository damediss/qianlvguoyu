package com.guoyu.platform.ai.infrastructure;

/**
 * LLM 返回 JSON 合法但 similar_questions / negative_questions 任一为空数组 — 瞬时，可退避重试。
 * 由应用层（FaqConversionApplicationService）在 JSON 解析完成后判空数组抛出，不是 FaqLlmClient 抛出。
 */
public class FaqLlmPartialResultException extends FaqLlmException {

  public FaqLlmPartialResultException(String message) {
    super(message);
  }
}
