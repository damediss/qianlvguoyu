package com.guoyu.platform.exam.reviewsync.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 题型归一与中文标签（《试题解析Markdown表格化_一期实施方案》§5.2 全映射）。 */
class QuestionTypesTest {

  @Test
  void normalizeMapsAllHistoricalForms() {
    assertEquals("single", QuestionTypes.normalize("single"));
    assertEquals("single", QuestionTypes.normalize("danxuan"));
    assertEquals("single", QuestionTypes.normalize("单选"));
    assertEquals("single", QuestionTypes.normalize("单选题"));
    assertEquals("single", QuestionTypes.normalize("1"));
    assertEquals("multi", QuestionTypes.normalize("multi"));
    assertEquals("multi", QuestionTypes.normalize("duoxuan"));
    assertEquals("multi", QuestionTypes.normalize("多选"));
    assertEquals("multi", QuestionTypes.normalize("多选题"));
    assertEquals("multi", QuestionTypes.normalize("2"));
    assertEquals("judge", QuestionTypes.normalize("judge"));
    assertEquals("judge", QuestionTypes.normalize("pandan"));
    assertEquals("judge", QuestionTypes.normalize("panduan"));
    assertEquals("judge", QuestionTypes.normalize("判断"));
    assertEquals("judge", QuestionTypes.normalize("判断题"));
    assertEquals("judge", QuestionTypes.normalize("3"));
    assertEquals("short", QuestionTypes.normalize("short"));
    assertEquals("short", QuestionTypes.normalize("jianda"));
    assertEquals("short", QuestionTypes.normalize("简答"));
    assertEquals("short", QuestionTypes.normalize("简答题"));
    assertEquals("short", QuestionTypes.normalize("4"));
    assertEquals("answer", QuestionTypes.normalize("answer"));
    assertEquals("answer", QuestionTypes.normalize("问答"));
    assertEquals("answer", QuestionTypes.normalize("问答题"));
    assertEquals("cal", QuestionTypes.normalize("cal"));
    assertEquals("cal", QuestionTypes.normalize("计算"));
    assertEquals("cal", QuestionTypes.normalize("计算题"));
    assertEquals("essay", QuestionTypes.normalize("essay"));
    assertEquals("essay", QuestionTypes.normalize("论述"));
    assertEquals("essay", QuestionTypes.normalize("论述题"));
    assertEquals("analysis", QuestionTypes.normalize("analysis"));
    assertEquals("analysis", QuestionTypes.normalize("分析"));
    assertEquals("analysis", QuestionTypes.normalize("分析题"));
    assertEquals("explain", QuestionTypes.normalize("explain"));
    assertEquals("explain", QuestionTypes.normalize("解释"));
    assertEquals("explain", QuestionTypes.normalize("解释题"));
    assertEquals("fill", QuestionTypes.normalize("fill"));
    assertEquals("fill", QuestionTypes.normalize("填空"));
    assertEquals("fill", QuestionTypes.normalize("填空题"));
    assertEquals("drafting", QuestionTypes.normalize("drafting"));
    assertEquals("drafting", QuestionTypes.normalize("绘图"));
    assertEquals("drafting", QuestionTypes.normalize("绘图题"));
  }

  @Test
  void normalizeToleratesCaseAndWhitespace() {
    assertEquals("single", QuestionTypes.normalize("  Single "));
    assertEquals("judge", QuestionTypes.normalize(" PanDaN "));
    assertEquals("essay", QuestionTypes.normalize(" 论述 "));
  }

  @Test
  void normalizeNullBlankAndUnknown() {
    assertNull(QuestionTypes.normalize(null));
    assertNull(QuestionTypes.normalize(""));
    assertNull(QuestionTypes.normalize("   "));
    assertEquals("case-study", QuestionTypes.normalize(" Case-Study "));
  }

  @Test
  void toLabelMapsKnownTypes() {
    assertEquals("单选题", QuestionTypes.toLabel("single"));
    assertEquals("多选题", QuestionTypes.toLabel("多选"));
    assertEquals("判断题", QuestionTypes.toLabel("panduan"));
    assertEquals("简答题", QuestionTypes.toLabel("short"));
    assertEquals("问答题", QuestionTypes.toLabel("问答"));
    assertEquals("计算题", QuestionTypes.toLabel("cal"));
    assertEquals("论述题", QuestionTypes.toLabel("论述"));
    assertEquals("分析题", QuestionTypes.toLabel("analysis"));
    assertEquals("解释题", QuestionTypes.toLabel("解释"));
    assertEquals("填空题", QuestionTypes.toLabel("fill"));
    assertEquals("绘图题", QuestionTypes.toLabel("绘图"));
  }

  @Test
  void toLabelUnknownAndBlank() {
    assertEquals("未标注题型", QuestionTypes.toLabel(null));
    assertEquals("未标注题型", QuestionTypes.toLabel(""));
    assertEquals("未标注题型", QuestionTypes.toLabel("case-study"));
  }

  @Test
  void existingObjectiveAndSubjectiveContractsUnchanged() {
    assertEquals(List.of("single", "multi", "judge"), QuestionTypes.OBJECTIVE_TYPES);
    assertTrue(QuestionTypes.isSubjective("essay"));
    assertTrue(QuestionTypes.isSubjective(" Essay "));
    assertFalse(QuestionTypes.isSubjective("single"));
    assertFalse(QuestionTypes.isSubjective(null));
    assertFalse(QuestionTypes.isSubjective("unknown"));
  }
}
