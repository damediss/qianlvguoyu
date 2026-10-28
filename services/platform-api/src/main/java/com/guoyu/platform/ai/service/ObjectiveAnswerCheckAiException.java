package com.guoyu.platform.ai.service;

/** AI 调用失败：区分可重试与不可重试；budget 422 不计入系统性连续失败。 */
public class ObjectiveAnswerCheckAiException extends RuntimeException {

  private final String errorCode;
  private final boolean retryable;
  private final boolean systemic;

  public ObjectiveAnswerCheckAiException(String message, String errorCode, boolean retryable,
      boolean systemic, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.retryable = retryable;
    this.systemic = systemic;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public boolean isRetryable() {
    return retryable;
  }

  public boolean isSystemic() {
    return systemic;
  }
}
