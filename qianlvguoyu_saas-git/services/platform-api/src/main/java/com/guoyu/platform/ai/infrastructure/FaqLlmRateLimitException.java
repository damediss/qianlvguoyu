package com.guoyu.platform.ai.infrastructure;

/** LLM 返回 HTTP 429（限流）— 瞬时，可退避重试。 */
public class FaqLlmRateLimitException extends FaqLlmException {

  public FaqLlmRateLimitException(String message) {
    super(message);
  }
}
