package com.guoyu.platform.competition.exam.formal;

public final class FormalExamErrorCodes {

  public static final String ANSWERS_NOT_SYNCED = "ANSWERS_NOT_SYNCED";
  public static final String UNANSWERED_CONFIRM_REQUIRED = "UNANSWERED_CONFIRM_REQUIRED";

  /**
   * CandidateExam.submit_type 与表约束 chk_comp_formal_candidate_exam_submit 对齐：
   * submitted / auto_submitted / forced_submitted。主动交卷写 submitted。
   */
  public static final String SUBMIT_TYPE_MANUAL = "submitted";
  public static final String SUBMIT_TYPE_AUTO = "auto_submitted";
  public static final String SUBMIT_TYPE_FORCED = "forced_submitted";

  public static final String ABSENT_REASON_DEADLINE_NOT_ENTERED = "考试截止前未进入本场考试";

  private FormalExamErrorCodes() {
  }
}
