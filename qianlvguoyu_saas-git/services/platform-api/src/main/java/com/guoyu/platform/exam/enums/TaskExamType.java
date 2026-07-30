package com.guoyu.platform.exam.enums;

/**
 * 题型枚举 —— 与 PHP 端 {@code task_exam / task_examshicao / task_examsub} 三套任务主表对应。
 */
public enum TaskExamType {
  THEORY("exam", "理论"),
  PRACTICE("examshicao", "实操"),
  SUBJECTIVE("examsub", "主观");

  private final String code;
  private final String cn;

  TaskExamType(String code, String cn) {
    this.code = code;
    this.cn = cn;
  }

  public String code() { return code; }
  public String cn()   { return cn; }

  public static TaskExamType fromCode(String code) {
    if (code == null) return null;
    for (TaskExamType t : values()) {
      if (t.code.equalsIgnoreCase(code)) return t;
    }
    return null;
  }
}
