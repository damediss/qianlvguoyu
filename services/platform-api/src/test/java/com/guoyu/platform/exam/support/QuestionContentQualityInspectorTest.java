package com.guoyu.platform.exam.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 内容质量识别（方案 §8.4 / §16.1-B）：确定性规则，与 AI 置信度无关；
 * suggestedText 只供人工参考，原文不得被修改。
 */
class QuestionContentQualityInspectorTest {

  private List<QuestionContentQualityWarning> inspect(String text) {
    return QuestionContentQualityInspector.inspect(text, "option", "opt_2");
  }

  @Test
  void asciiAndFullWidthScientificNotationAreSuspectWithSuggestion() {
    List<QuestionContentQualityWarning> ascii = inspect("1.2E+3");
    assertEquals(1, ascii.size());
    assertEquals(QuestionContentQualityWarning.RULE_SCIENTIFIC, ascii.get(0).ruleCode());
    assertEquals("1200", ascii.get(0).suggestedText());
    assertTrue(ascii.get(0).isSuspect());

    List<QuestionContentQualityWarning> fullWidth = inspect("1.2ｅ＋3");
    assertTrue(fullWidth.stream()
        .anyMatch(w -> QuestionContentQualityWarning.RULE_SCIENTIFIC.equals(w.ruleCode())));
    assertEquals("1200", fullWidth.get(0).suggestedText());
  }

  @Test
  void hugeExponentsReturnNullSuggestionQuickly() {
    assertNull(QuestionContentQualityInspector.suggestedText("1E+1000000000"));
    assertNull(QuestionContentQualityInspector.suggestedText("1E-1000000000"));
    assertNull(QuestionContentQualityInspector.suggestedText("1E+" + "9".repeat(200)));
    assertEquals("0.001", QuestionContentQualityInspector.suggestedText("1E-3"));
  }

  @Test
  void excelErrorLiteralsAreInvalid() {
    for (String text : List.of("#REF!", "#DIV/0!", "#VALUE!", "#N/A", "NaN", "Infinity",
        "-Infinity")) {
      List<QuestionContentQualityWarning> warnings = inspect(text);
      assertTrue(warnings.stream().anyMatch(QuestionContentQualityWarning::isInvalid),
          "应判 invalid: " + text);
      assertEquals(QuestionContentQualityWarning.SEVERITY_INVALID,
          QuestionContentQualityInspector.summarizeSeverity(warnings));
    }
  }

  @Test
  void hashMarksAndReplacementCharStaySuspect() {
    assertTrue(inspect("####").stream()
        .anyMatch(w -> QuestionContentQualityWarning.RULE_HASH_MARKS.equals(w.ruleCode())));
    assertTrue(inspect("编码\uFFFD异常").stream().anyMatch(
        w -> QuestionContentQualityWarning.RULE_REPLACEMENT_CHAR.equals(w.ruleCode())));
    assertEquals(QuestionContentQualityWarning.SEVERITY_SUSPECT,
        QuestionContentQualityInspector.summarizeSeverity(inspect("####")));
  }

  @Test
  void normalScientificNotationAndUnitsAreNotMistaken() {
    for (String text : List.of("1.6×10^-19", "公式 E=mc^2", "单位 25 mm", "职业编码 4-07-01-01",
        "0.5", "1/3")) {
      assertFalse(inspect(text).stream()
              .anyMatch(w -> QuestionContentQualityWarning.RULE_SCIENTIFIC.equals(w.ruleCode())),
          "不应命中科学计数法: " + text);
    }
  }

  @Test
  void longDecimalAndFloatTailAreOnlySuspect() {
    List<QuestionContentQualityWarning> tail = inspect("0.30000000000000004");
    assertTrue(tail.stream().anyMatch(
        w -> QuestionContentQualityWarning.RULE_DECIMAL_PRECISION.equals(w.ruleCode())));
    assertTrue(tail.stream().allMatch(w -> !w.isInvalid()));
    assertEquals(0, inspect("0.3333333333").size(), "普通循环小数不自动判错");
  }

  @Test
  void severityTakesWorstAndFieldsAreComplete() {
    List<QuestionContentQualityWarning> warnings =
        QuestionContentQualityInspector.inspect("#REF! 1.2E+3", "stem", null);
    assertEquals(QuestionContentQualityWarning.SEVERITY_INVALID,
        QuestionContentQualityInspector.summarizeSeverity(warnings));
    QuestionContentQualityWarning scientific = warnings.stream()
        .filter(w -> QuestionContentQualityWarning.RULE_SCIENTIFIC.equals(w.ruleCode())).findFirst()
        .orElseThrow();
    assertEquals("stem", scientific.location());
    assertNull(scientific.optionId());
    assertEquals("1.2E+3", scientific.matchedText());
    assertFalse(scientific.message().isBlank());
    assertEquals(QuestionContentQualityWarning.SEVERITY_CLEAN,
        QuestionContentQualityInspector.summarizeSeverity(List.of()));
  }
}
