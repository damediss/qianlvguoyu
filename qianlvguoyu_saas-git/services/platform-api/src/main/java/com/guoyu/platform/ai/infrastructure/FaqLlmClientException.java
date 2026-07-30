package com.guoyu.platform.ai.infrastructure;

/**
 * HTTP 4xx（非 429，如 401/400）— 确定性，不重试。
 * 通常由无效 API Key 或请求体格式错误导致，重试无法解决。
 */
public class FaqLlmClientException extends FaqLlmException {

  public FaqLlmClientException(String message) {
    super(message);
  }
}
