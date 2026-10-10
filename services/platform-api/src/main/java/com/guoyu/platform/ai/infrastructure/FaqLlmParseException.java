package com.guoyu.platform.ai.infrastructure;

/**
 * LLM 返回 finish_reason='stop' 但 JSON 解析失败（如题干含 {} 干扰边界）— 确定性，不重试。
 * 同一 prompt 重试大概率同样失败，直接标 failed 并在 error_message 标注"确定性失败"。
 */
public class FaqLlmParseException extends FaqLlmException {

  public FaqLlmParseException(String message) {
    super(message);
  }
}
