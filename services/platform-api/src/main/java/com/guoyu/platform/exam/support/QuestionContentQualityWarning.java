package com.guoyu.platform.exam.support;

/**
 * 题库内容质量警告（方案 §8.4）。与 AI 置信度完全独立，不参与任何模型数值计算。
 *
 * @param location      stem / option
 * @param optionId      选项内部 key（opt_0…）；题干为 null
 * @param matchedText   实际命中的原始 token（不因 NFKC 检测副本改变原文）
 * @param suggestedText 仅科学计数法 token 的数值等价候选；只供人工参考，禁止替换原文或送入 AI
 */
public record QuestionContentQualityWarning(
    String ruleCode,
    String severity,
    String location,
    String optionId,
    String matchedText,
    String suggestedText,
    String message) {

  public static final String SEVERITY_CLEAN = "clean";
  public static final String SEVERITY_SUSPECT = "suspect";
  public static final String SEVERITY_INVALID = "invalid";

  public static final String RULE_EXCEL_ERROR_LITERAL = "EXCEL_ERROR_LITERAL";
  public static final String RULE_SCIENTIFIC = "EXCEL_SCIENTIFIC_NOTATION_SUSPECT";
  public static final String RULE_HASH_MARKS = "EXCEL_HASH_MARKS_SUSPECT";
  public static final String RULE_DECIMAL_PRECISION = "EXCESSIVE_DECIMAL_PRECISION_SUSPECT";
  public static final String RULE_REPLACEMENT_CHAR = "TEXT_REPLACEMENT_CHAR_SUSPECT";

  public boolean isInvalid() {
    return SEVERITY_INVALID.equals(severity);
  }

  public boolean isSuspect() {
    return SEVERITY_SUSPECT.equals(severity);
  }
}
