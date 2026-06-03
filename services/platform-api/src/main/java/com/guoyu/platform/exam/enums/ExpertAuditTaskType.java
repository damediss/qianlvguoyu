package com.guoyu.platform.exam.enums;

/**
 * 专家任务类型 —— 对应 {@code exam_task_expert.task_type} 字段。
 * 沿用现有 {@code ExpertAuditApplicationService} 的 tag 命名 (ximu/exam/examshicao/examsub)。
 */
public enum ExpertAuditTaskType {
  XIMU("ximu", "细目表审核"),
  EXAM("exam", "题库理论审核"),
  EXAMSHICAO("examshicao", "题库实操审核"),
  EXAMSUB("examsub", "题库主观审核");

  private final String code;
  private final String cn;

  ExpertAuditTaskType(String code, String cn) {
    this.code = code;
    this.cn = cn;
  }

  public String code() { return code; }
  public String cn()   { return cn; }

  public static ExpertAuditTaskType fromCode(String code) {
    if (code == null) return null;
    for (ExpertAuditTaskType t : values()) {
      if (t.code.equalsIgnoreCase(code)) return t;
    }
    return null;
  }
}
