package com.guoyu.platform.exam.support;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * 题干/选项内容质量唯一实现（方案 §8.4）。
 *
 * <p>导入预检与 AI 批次共用本类，禁止在两处各写一套正则。检测前只对“检测副本”做 NFKC，
 * 原始文本、sourceHash 与发送给 AI 的文本均不因此改变。
 *
 * <p>边界：格式规则不能还原已经丢失的信息（0.5→0），只能提示，不得自动改文本/答案。
 */
public final class QuestionContentQualityInspector {

  private QuestionContentQualityInspector() {
  }

  static final Pattern SCIENTIFIC = Pattern.compile(
      "(?<![A-Za-z0-9_])[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)[Ee][+-]?\\d+(?![A-Za-z0-9_])");
  static final Pattern EXCEL_ERRORS = Pattern.compile(
      "(?<![A-Za-z0-9_])(#DIV/0!|#VALUE!|#REF!|#N/A|#NAME\\?|#NUM!|#NULL!|NaN|Infinity|-Infinity)"
          + "(?![A-Za-z0-9_])");
  static final Pattern HASH_MARKS = Pattern.compile("#{3,}");
  static final Pattern LONG_DECIMAL = Pattern.compile("\\d+\\.\\d{16,}");
  static final Pattern FLOAT_TAIL = Pattern.compile("\\d+\\.\\d*?(?:0{12,}|9{12,})\\d*");
  static final Pattern REPLACEMENT_CHAR = Pattern.compile("\uFFFD");

  /** 科学计数法候选转换的安全上限（方案 §8.4 补充约束 2）。 */
  static final int MAX_TOKEN_LENGTH = 128;
  static final int MAX_SUGGESTED_CHARS = 4096;

  public static List<QuestionContentQualityWarning> inspect(String text, String location, String optionId) {
    if (!StringUtils.hasText(text)) {
      return List.of();
    }
    String detectionCopy = Normalizer.normalize(text, Normalizer.Form.NFKC);
    List<QuestionContentQualityWarning> warnings = new ArrayList<>();

    Matcher error = EXCEL_ERRORS.matcher(detectionCopy);
    while (error.find()) {
      // matchedText 用原文切片会因 NFKC 长度变化错位；错误字面量本身是 ASCII，直接用匹配值
      String token = error.group();
      warnings.add(new QuestionContentQualityWarning(
          QuestionContentQualityWarning.RULE_EXCEL_ERROR_LITERAL,
          QuestionContentQualityWarning.SEVERITY_INVALID, location, optionId, token, null,
          "该处包含表格错误值 " + token + "，无法用于答案检查，请人工修正"));
    }

    Matcher scientific = SCIENTIFIC.matcher(detectionCopy);
    while (scientific.find()) {
      String token = scientific.group();
      warnings.add(new QuestionContentQualityWarning(
          QuestionContentQualityWarning.RULE_SCIENTIFIC,
          QuestionContentQualityWarning.SEVERITY_SUSPECT, location, optionId, token,
          suggestedText(token),
          "该处疑似被 Excel 转成了科学计数法，请核对原始数值"));
    }

    Matcher hash = HASH_MARKS.matcher(detectionCopy);
    while (hash.find()) {
      warnings.add(new QuestionContentQualityWarning(
          QuestionContentQualityWarning.RULE_HASH_MARKS,
          QuestionContentQualityWarning.SEVERITY_SUSPECT, location, optionId, hash.group(), null,
          "该处出现连续 # 号，可能是表格列宽不足或内容异常，请人工核对"));
    }

    Matcher decimal = LONG_DECIMAL.matcher(detectionCopy);
    while (decimal.find()) {
      addPrecisionWarning(warnings, location, optionId, decimal.group());
    }
    Matcher floatTail = FLOAT_TAIL.matcher(detectionCopy);
    while (floatTail.find()) {
      addPrecisionWarning(warnings, location, optionId, floatTail.group());
    }

    Matcher replacement = REPLACEMENT_CHAR.matcher(detectionCopy);
    while (replacement.find()) {
      warnings.add(new QuestionContentQualityWarning(
          QuestionContentQualityWarning.RULE_REPLACEMENT_CHAR,
          QuestionContentQualityWarning.SEVERITY_SUSPECT, location, optionId, replacement.group(),
          null, "该处含无法识别的替换字符，可能存在编码问题，请人工核对"));
    }
    return List.copyOf(warnings);
  }

  private static void addPrecisionWarning(List<QuestionContentQualityWarning> warnings, String location,
      String optionId, String token) {
    if (warnings.stream().anyMatch(w -> w.ruleCode()
        .equals(QuestionContentQualityWarning.RULE_DECIMAL_PRECISION)
        && token.equals(w.matchedText()))) {
      return;
    }
    warnings.add(new QuestionContentQualityWarning(
        QuestionContentQualityWarning.RULE_DECIMAL_PRECISION,
        QuestionContentQualityWarning.SEVERITY_SUSPECT, location, optionId, token, null,
        "该数值小数位异常长，可能是二进制浮点误差或导入精度问题；系统不会自动截断"));
  }

  /** 多 warning 取最高严重度：invalid &gt; suspect &gt; clean。 */
  public static String summarizeSeverity(List<QuestionContentQualityWarning> warnings) {
    if (warnings == null || warnings.isEmpty()) {
      return QuestionContentQualityWarning.SEVERITY_CLEAN;
    }
    if (warnings.stream().anyMatch(QuestionContentQualityWarning::isInvalid)) {
      return QuestionContentQualityWarning.SEVERITY_INVALID;
    }
    return QuestionContentQualityWarning.SEVERITY_SUSPECT;
  }

  /**
   * 科学计数法 → 数值等价文本。先做长度和规模估算再调用 toPlainString，
   * 禁止先展开再截断（1E+1000000000 会直接撑爆内存）。
   */
  public static String suggestedText(String token) {
    if (token == null || token.length() > MAX_TOKEN_LENGTH) {
      return null;
    }
    try {
      BigDecimal value = new BigDecimal(token);
      int integerDigits = Math.max(1, value.precision() - value.scale());
      int estimated = (value.signum() < 0 ? 1 : 0) + integerDigits
          + (value.scale() > 0 ? 1 + value.scale() : 0);
      if (estimated > MAX_SUGGESTED_CHARS) {
        return null;
      }
      return value.toPlainString();
    } catch (NumberFormatException | ArithmeticException e) {
      return null;
    }
  }
}
