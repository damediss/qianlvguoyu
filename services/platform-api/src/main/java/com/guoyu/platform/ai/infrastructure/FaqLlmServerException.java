package com.guoyu.platform.ai.infrastructure;

/**
 * LLM 返回 HTTP 5xx（服务端错误），或 finish_reason='stop' 但 content 为空/null — 瞬时，可退避重试。
 */
public class FaqLlmServerException extends FaqLlmException {

  public FaqLlmServerException(String message) {
    super(message);
  }
}
