package com.guoyu.platform.exam.support;

/**
 * XLSX 导入行列级问题（方案 §14.4.2）。
 *
 * <p>severity=error 为硬错误（整批拒绝）；warning 为软提示（可继续导入但必须可见）。
 * matchedText 必须可非空，reader 产生的单元格级问题用 rawValue 回填，避免去重键出现 null。
 */
public record QuestionImportIssue(
    String severity,
    String ruleCode,
    String sheet,
    int row,
    int column,
    String field,
    String rawValue,
    String matchedText,
    String message) {

  public static final String SEVERITY_ERROR = "error";
  public static final String SEVERITY_WARNING = "warning";

  public QuestionImportIssue {
    rawValue = rawValue == null ? "" : rawValue;
    matchedText = matchedText == null || matchedText.isEmpty() ? rawValue : matchedText;
    message = message == null ? "" : message;
  }

  public boolean isError() {
    return SEVERITY_ERROR.equals(severity);
  }

  /** 去重键：sheet+row+column+ruleCode+matchedText（方案 §14.4.2）。 */
  public String dedupKey() {
    return sheet + '|' + row + '|' + column + '|' + ruleCode + '|' + matchedText;
  }
}
