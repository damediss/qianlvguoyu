package com.guoyu.platform.ai.infrastructure;

/** 网络超时 / SocketTimeoutException — 瞬时，可退避重试。 */
public class FaqLlmTimeoutException extends FaqLlmException {

  public FaqLlmTimeoutException(String message) {
    super(message);
  }
}
