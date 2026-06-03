package com.guoyu.platform.exam.enums;

/**
 * 专家审核角色 —— 0 初审 / 1 终审。
 */
public enum ExpertAuditRole {
  FIRST(0, "初审"),
  FINAL(1, "终审");

  private final int code;
  private final String cn;

  ExpertAuditRole(int code, String cn) {
    this.code = code;
    this.cn = cn;
  }

  public int code() { return code; }
  public String cn() { return cn; }

  public static ExpertAuditRole fromCode(Integer code) {
    if (code == null) return null;
    for (ExpertAuditRole r : values()) {
      if (r.code == code) return r;
    }
    return null;
  }
}
