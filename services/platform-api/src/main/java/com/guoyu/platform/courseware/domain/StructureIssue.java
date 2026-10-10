package com.guoyu.platform.courseware.domain;

/** 结构问题(方案 v1.2 §8.4):error 阻断导出;code 驱动修复分流。 */
public record StructureIssue(String severity, String code, String slotId, String message) {

  public static final String SEVERITY_ERROR = "error";
  public static final String SEVERITY_WARNING = "warning";

  public static final String CODE_STRUCTURE = "structure";
  public static final String CODE_CAPACITY = "capacity";
  public static final String CODE_OVERFLOW = "overflow";
  public static final String CODE_EMPTY_PHRASE = "empty_phrase";
  public static final String CODE_THIN_CONTENT = "thin_content";

  public static StructureIssue error(String slotId, String message) {
    return new StructureIssue(SEVERITY_ERROR, CODE_STRUCTURE, slotId, message);
  }

  public static StructureIssue warning(String code, String slotId, String message) {
    return new StructureIssue(SEVERITY_WARNING, code, slotId, message);
  }

  public boolean isError() {
    return SEVERITY_ERROR.equals(severity);
  }

  /** 生成回路是否应因该问题触发修复(方案 §8.4):仅 error + thin_content + empty_phrase */
  public static boolean isRepairWorthy(StructureIssue issue) {
    if (issue.isError()) {
      return true;
    }
    return CODE_THIN_CONTENT.equals(issue.code()) || CODE_EMPTY_PHRASE.equals(issue.code());
  }
}
