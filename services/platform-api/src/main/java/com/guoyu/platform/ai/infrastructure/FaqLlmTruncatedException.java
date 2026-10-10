package com.guoyu.platform.ai.infrastructure;

/**
 * LLM 返回 finish_reason='length'（输出被 max_tokens 截断）— 瞬时，可退避重试。
 * 在解析 JSON 之前判定，避免截断内容被误判为 JSON 解析失败。
 */
public class FaqLlmTruncatedException extends FaqLlmException {

  public FaqLlmTruncatedException(String message) {
    super(message);
  }
}
