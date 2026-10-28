package com.guoyu.platform.ai.infrastructure;

/** 422 token 预算超限：明细 failed/retryable=0，不参与系统性连续失败熔断（方案 §9.5）。 */
public class JevBudgetExceededException extends JevValidationException {

  public JevBudgetExceededException(String message) {
    super(message);
  }
}
