package com.guoyu.platform.exam.enums;

/**
 * 审核任务状态机 —— 与 PHP 端 {@code status} 字段对齐。
 * 0 待审核 → 1 初审中 → 2 终审中 → 3 完成
 */
public enum TaskAuditStatus {
  PENDING(0, "待审核"),
  FIRST_AUDIT(1, "初审中"),
  FINAL_AUDIT(2, "终审中"),
  COMPLETED(3, "完成");

  private final int code;
  private final String cn;

  TaskAuditStatus(int code, String cn) {
    this.code = code;
    this.cn = cn;
  }

  public int code() { return code; }
  public String cn() { return cn; }

  public static TaskAuditStatus fromCode(Integer code) {
    if (code == null) return null;
    for (TaskAuditStatus s : values()) {
      if (s.code == code) return s;
    }
    return null;
  }

  public static String cnOf(Integer code) {
    TaskAuditStatus s = fromCode(code);
    return s == null ? "-" : s.cn;
  }
}
