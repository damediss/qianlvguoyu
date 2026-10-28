package com.guoyu.platform.exam.answercheck.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.ai.dto.ObjectiveAnswerDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerOptionDecision;
import com.guoyu.platform.exam.answercheck.parser.NormalizedObjectiveQuestion;
import com.guoyu.platform.exam.answercheck.parser.NormalizedOption;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 结果判定（方案 §10 / §16.1-E）：两个置信度语义不同，多选取最弱项，阈值先比较后舍入。
 */
class ObjectiveAnswerCheckDomainServiceTest {

  private final ObjectiveAnswerCheckDomainService service =
      new ObjectiveAnswerCheckDomainService();

  private static final BigDecimal THETA = new BigDecimal("0.80");
  private static final BigDecimal DELTA = new BigDecimal("0.20");

  private NormalizedObjectiveQuestion question(String type, boolean... correct) {
    List<NormalizedOption> options = new ArrayList<>();
    for (int i = 0; i < correct.length; i++) {
      options.add(new NormalizedOption("opt_" + i, i, "judge".equals(type)
          ? (i == 0 ? "T" : "F") : String.valueOf((char) ('A' + i)), "选项" + i, correct[i]));
    }
    return new NormalizedObjectiveQuestion(101, type, "题干", "路径", options, "hash");
  }

  private ObjectiveAnswerDecision questionDecision(String choice, double confidence,
      Map<String, Double> probabilities) {
    return new ObjectiveAnswerDecision(ObjectiveAnswerDecision.SCOPE_QUESTION, choice, confidence,
        probabilities, Map.of(), 1);
  }

  private Map<String, Double> probs(double... values) {
    Map<String, Double> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i++) {
      map.put("opt_" + i, values[i]);
    }
    return map;
  }

  @Test
  void singleReferenceConfidenceIsWeakestOriginalMarkSupport() {
    NormalizedObjectiveQuestion q = question("single", false, true, false, false);
    ObjectiveAnswerCheckResult result = service.judge(q,
        questionDecision("opt_1", 0.9, probs(0.05, 0.85, 0.07, 0.03)), THETA, DELTA);

    assertEquals(ObjectiveAnswerCheckDomainService.RESULT_CONSISTENT_HIGH, result.resultCode());
    assertTrue(result.consistent());
    assertEquals(new BigDecimal("0.85000"), result.referenceConfidence());
    assertEquals(new BigDecimal("0.90000"), result.aiConfidence());
    assertEquals(new BigDecimal("0.78000"), result.minMargin());
    assertEquals(4, result.optionResults().options().size());
    assertEquals(0.95, result.optionResults().options().get(0).referenceProbability(), 1e-9);
    assertNull(result.optionResults().options().get(0).choice());
    assertEquals("opt_1", result.optionResults().choice());
  }

  @Test
  void consistentHighAlsoRequiresReferenceSupport() {
    NormalizedObjectiveQuestion q = question("single", false, true);
    // AI 很自信，但概率分布对原答案（B）支持不足 → 只能低置信
    ObjectiveAnswerCheckResult result = service.judge(q,
        questionDecision("opt_1", 0.9, probs(0.21, 0.79)), THETA, DELTA);
    assertEquals(ObjectiveAnswerCheckDomainService.RESULT_LOW_CONFIDENCE, result.resultCode());
    assertTrue(result.consistent());
    assertEquals(new BigDecimal("0.79000"), result.referenceConfidence());
  }

  @Test
  void mismatchHighAndLowConfidenceSeparatedByThresholds() {
    NormalizedObjectiveQuestion q = question("single", true, false, false, false);
    ObjectiveAnswerCheckResult high = service.judge(q,
        questionDecision("opt_2", 0.95, probs(0.02, 0.03, 0.93, 0.02)), THETA, DELTA);
    assertEquals(ObjectiveAnswerCheckDomainService.RESULT_MISMATCH_HIGH, high.resultCode());
    assertFalse(high.consistent());

    ObjectiveAnswerCheckResult low = service.judge(q,
        questionDecision("opt_2", 0.70, probs(0.10, 0.20, 0.70, 0.0)), THETA, DELTA);
    assertEquals(ObjectiveAnswerCheckDomainService.RESULT_LOW_CONFIDENCE, low.resultCode());
    assertFalse(low.consistent());
  }

  @Test
  void thresholdBoundaryIsInclusiveAndRoundingDoesNotDecide() {
    NormalizedObjectiveQuestion q = question("single", false, true);
    ObjectiveAnswerCheckResult exact = service.judge(q,
        questionDecision("opt_0", 0.80, probs(0.60, 0.40)), THETA, DELTA);
    assertEquals(ObjectiveAnswerCheckDomainService.RESULT_MISMATCH_HIGH, exact.resultCode());
    assertEquals(new BigDecimal("0.20000"), exact.minMargin());

    ObjectiveAnswerCheckResult justBelow = service.judge(q,
        questionDecision("opt_1", 0.7999, probs(0.7999, 0.2001)), THETA, DELTA);
    assertEquals(ObjectiveAnswerCheckDomainService.RESULT_LOW_CONFIDENCE,
        justBelow.resultCode());
  }

  @Test
  void judgeUsesOriginalTruthValueProbabilityNotItsComplement() {
    NormalizedObjectiveQuestion q = question("judge", false, true);
    ObjectiveAnswerCheckResult result = service.judge(q,
        questionDecision("opt_1", 0.9, probs(0.1, 0.9)), THETA, DELTA);
    assertEquals(ObjectiveAnswerCheckDomainService.RESULT_CONSISTENT_HIGH, result.resultCode());
    assertEquals(new BigDecimal("0.90000"), result.referenceConfidence());
    assertEquals("F", result.optionResults().options().get(1).label());
  }

  @Test
  void multiUsesWeakestOptionWithoutMultiplyingProbabilities() {
    NormalizedObjectiveQuestion q = question("multi", true, true, false, false);
    Map<String, ObjectiveAnswerOptionDecision> decisions = new LinkedHashMap<>();
    decisions.put("opt_0", sub("opt_0", "Y", 0.9, 0.9, 0.1));
    decisions.put("opt_1", sub("opt_1", "Y", 0.6, 0.6, 0.4));
    decisions.put("opt_2", sub("opt_2", "N", 0.95, 0.05, 0.95));
    decisions.put("opt_3", sub("opt_3", "N", 0.88, 0.12, 0.88));
    ObjectiveAnswerDecision decision = new ObjectiveAnswerDecision(
        ObjectiveAnswerDecision.SCOPE_OPTION, null, null, null, decisions, 1);

    ObjectiveAnswerCheckResult result = service.judge(q, decision, THETA, DELTA);

    assertEquals(ObjectiveAnswerCheckDomainService.RESULT_LOW_CONFIDENCE, result.resultCode());
    assertEquals(new BigDecimal("0.60000"), result.aiConfidence(), "取最小值，不做概率连乘");
    assertTrue(result.consistent());
    assertTrue(result.minMargin().compareTo(new BigDecimal("0.20000")) == 0);
    assertNull(result.decisionWarning());
  }

  @Test
  void multiWithFewerThanTwoAiCorrectForcesLowConfidence() {
    NormalizedObjectiveQuestion q = question("multi", true, true, false);
    Map<String, ObjectiveAnswerOptionDecision> decisions = new LinkedHashMap<>();
    decisions.put("opt_0", sub("opt_0", "Y", 0.99, 0.99, 0.01));
    decisions.put("opt_1", sub("opt_1", "N", 0.99, 0.01, 0.99));
    decisions.put("opt_2", sub("opt_2", "N", 0.99, 0.01, 0.99));
    ObjectiveAnswerDecision decision = new ObjectiveAnswerDecision(
        ObjectiveAnswerDecision.SCOPE_OPTION, null, null, null, decisions, 1);

    ObjectiveAnswerCheckResult result = service.judge(q, decision, THETA, DELTA);

    assertEquals(ObjectiveAnswerCheckDomainService.RESULT_LOW_CONFIDENCE, result.resultCode());
    assertEquals(ObjectiveAnswerCheckDomainService.WARNING_AI_COMBINATION_INVALID,
        result.decisionWarning());
  }

  @Test
  void multiReferenceConfidenceUsesNForOriginalWrongOption() {
    NormalizedObjectiveQuestion q = question("multi", true, true, false);
    Map<String, ObjectiveAnswerOptionDecision> decisions = new LinkedHashMap<>();
    decisions.put("opt_0", sub("opt_0", "Y", 0.9, 0.9, 0.1));
    decisions.put("opt_1", sub("opt_1", "Y", 0.9, 0.9, 0.1));
    // 原标记为“不应选”，模型只有 0.60 认为不应选 → 原答案支持度最弱项 0.60
    decisions.put("opt_2", sub("opt_2", "Y", 0.9, 0.4, 0.6));
    ObjectiveAnswerDecision decision = new ObjectiveAnswerDecision(
        ObjectiveAnswerDecision.SCOPE_OPTION, null, null, null, decisions, 1);

    ObjectiveAnswerCheckResult result = service.judge(q, decision, THETA, DELTA);

    assertEquals("0.60000", result.referenceConfidence().toPlainString());
    assertFalse(result.consistent());
  }

  private ObjectiveAnswerOptionDecision sub(String optionId, String choice, double confidence,
      double pYes, double pNo) {
    Map<String, Double> probabilities = new LinkedHashMap<>();
    probabilities.put("Y", pYes);
    probabilities.put("N", pNo);
    double margin = Math.abs(pYes - pNo);
    return new ObjectiveAnswerOptionDecision(optionId, choice, confidence, probabilities, margin,
        Math.max(pYes, pNo), Math.min(pYes, pNo));
  }
}
