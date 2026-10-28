package com.guoyu.platform.exam.answercheck.parser;

/**
 * 解析结果：要么得到可检查题目，要么得到确定性 invalidReason（此时零 AI 调用）。
 */
public record ParseOutcome(NormalizedObjectiveQuestion question, String invalidReason) {

  public static ParseOutcome ok(NormalizedObjectiveQuestion question) {
    return new ParseOutcome(question, null);
  }

  public static ParseOutcome invalid(String reason) {
    return new ParseOutcome(null, reason);
  }

  public boolean valid() {
    return question != null;
  }
}
