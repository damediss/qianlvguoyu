package com.guoyu.platform.exam.theorygen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.theorygen.dto.TheoryQuestionItem;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 指引 §2.3 确定性校验规则单测（方案 GEN-6） */
class TheoryQuestionValidatorTest {

  private final TheoryQuestionValidator validator = new TheoryQuestionValidator();

  private TheoryQuestionItem single(String answer) {
    TheoryQuestionItem q = new TheoryQuestionItem();
    q.setQuestionType("single");
    q.setTypeCode("B");
    q.setDifficulty(3);
    q.setContent("氧化铝的化学式是（ ）。");
    q.setAnswer(answer);
    q.setOptions(List.of(
        option("A", "Al2O3"), option("B", "Al(OH)3"), option("C", "AlCl3"), option("D", "AlN")));
    return q;
  }

  private TheoryQuestionItem.Option option(String label, String content) {
    TheoryQuestionItem.Option o = new TheoryQuestionItem.Option();
    o.setLabel(label);
    o.setContent(content);
    return o;
  }

  @Test
  void validSinglePasses() {
    assertTrue(validator.validate(single("A")).isEmpty());
  }

  @Test
  void singleRequiresExactly4Options() {
    TheoryQuestionItem q = single("A");
    q.setOptions(q.getOptionList().subList(0, 3));
    List<String> errors = validator.validate(q);
    assertFalse(errors.isEmpty());
    assertTrue(errors.get(0).contains("4 个选项"));
  }

  @Test
  void singleAnswerMustBeSingleLetter() {
    List<String> e1 = validator.validate(single("AB"));
    assertFalse(e1.isEmpty(), "多字母答案应判失败");
    List<String> e2 = validator.validate(single("E"));
    assertFalse(e2.isEmpty(), "超出 A~D 应判失败");
  }

  @Test
  void multiRequiresTwoPlusAnswers() {
    TheoryQuestionItem q = new TheoryQuestionItem();
    q.setQuestionType("multi");
    q.setTypeCode("D");
    q.setDifficulty(3);
    q.setContent("下列属于氧化铝生产方法的有（ ）。");
    q.setOptions(List.of(
        option("A", "拜耳法"), option("B", "烧结法"), option("C", "联合法"), option("D", "电解法"),
        option("E", "置换法")));
    q.setAnswer("ABC");
    assertTrue(validator.validate(q).isEmpty());
    q.setAnswer("A");
    assertFalse(validator.validate(q).isEmpty(), "单答案多选应判失败");
  }

  @Test
  void judgeAnswerMustBeCheckOrCross() {
    TheoryQuestionItem q = new TheoryQuestionItem();
    q.setQuestionType("judge");
    q.setTypeCode("C");
    q.setDifficulty(2);
    q.setContent("拜耳法生产氧化铝过程中需要高压溶出。");
    q.setAnswer("√");
    assertTrue(validator.validate(q).isEmpty());
    q.setAnswer("A");
    assertFalse(validator.validate(q).isEmpty());
  }

  @Test
  void objectiveStemRequiresBlank() {
    TheoryQuestionItem q = single("A");
    q.setContent("氧化铝的化学式是什么。");
    assertFalse(validator.validate(q).isEmpty(), "选择题题干缺空缺句应判失败");
  }

  @Test
  void difficultyMustBe1to5() {
    TheoryQuestionItem q = single("A");
    q.setDifficulty(6);
    assertFalse(validator.validate(q).isEmpty());
    q.setDifficulty(0);
    assertFalse(validator.validate(q).isEmpty());
  }

  @Test
  void subjectiveRequiresThreeParts() {
    TheoryQuestionItem q = new TheoryQuestionItem();
    q.setQuestionType("essay");
    q.setTypeCode("E");
    q.setDifficulty(3);
    q.setContent("简述拜耳法生产原理。");
    q.setReferenceAnswer("……");
    q.setScorePoints("要点1（2分）；要点2（2分）；要点3（1分）");
    q.setScoringStandard("答对要点即得分");
    q.setScore(new java.math.BigDecimal("5"));
    assertTrue(validator.validate(q).isEmpty());
    q.setScoringStandard(null);
    assertFalse(validator.validate(q).isEmpty(), "缺评分标准应判失败");
  }

  @Test
  void subjectivePointSumMustEqualScore() {
    TheoryQuestionItem q = new TheoryQuestionItem();
    q.setQuestionType("essay");
    q.setTypeCode("E");
    q.setDifficulty(3);
    q.setContent("简述拜耳法生产原理。");
    q.setReferenceAnswer("……");
    q.setScorePoints("要点1（2分）；要点2（2分）");
    q.setScoringStandard("……");
    q.setScore(new java.math.BigDecimal("5"));
    List<String> errors = validator.validate(q);
    assertFalse(errors.isEmpty(), "要点和 4 ≠ 5 应判失败");
    assertTrue(errors.get(0).contains("分值之和"));
  }

  @Test
  void subjectivePointCap2For5PointType() {
    TheoryQuestionItem q = new TheoryQuestionItem();
    q.setQuestionType("essay");
    q.setTypeCode("E");
    q.setDifficulty(3);
    q.setContent("简述题。");
    q.setReferenceAnswer("……");
    q.setScorePoints("要点1（3分）；要点2（2分）");
    q.setScoringStandard("……");
    q.setScore(new java.math.BigDecimal("5"));
    List<String> errors = validator.validate(q);
    assertFalse(errors.isEmpty(), "5 分制题型要点 3 分应超上限");
  }

  @Test
  void duplicatesDetected() {
    TheoryQuestionItem a = single("A");
    TheoryQuestionItem b = single("B");
    b.setContent("铝土矿的主要成分是（ ）。"); // 题干不同
    assertTrue(validator.checkDuplicates(List.of(a, b)).isEmpty());
    TheoryQuestionItem c = single("C"); // 与 a 题干相同
    List<String> dup = validator.checkDuplicates(List.of(a, b, c));
    assertFalse(dup.isEmpty(), "同题干应判重复");
  }
}
