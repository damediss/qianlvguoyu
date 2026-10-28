package com.guoyu.platform.exam.answercheck.service;

import com.guoyu.platform.ai.dto.ObjectiveAnswerDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerOptionDecision;
import com.guoyu.platform.exam.answercheck.parser.NormalizedObjectiveQuestion;
import com.guoyu.platform.exam.answercheck.parser.NormalizedOption;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 唯一结果判定入口（方案 §6 / §10）。
 *
 * <p>两个置信度语义不同，禁止互相替代：
 * referenceConfidence 是模型对原答案各标记的支持度（取最弱选项），不是正确率；
 * aiConfidence 是模型对自身判断的置信度（多选取各选项最小值，禁止概率连乘）。
 */
@Service
public class ObjectiveAnswerCheckDomainService {

  public static final String RESULT_CONSISTENT_HIGH = "consistent_high";
  public static final String RESULT_MISMATCH_HIGH = "mismatch_high";
  public static final String RESULT_LOW_CONFIDENCE = "low_confidence";
  public static final String RESULT_INVALID = "invalid";
  public static final String RESULT_FAILED = "failed";

  public static final String WARNING_AI_COMBINATION_INVALID = "AI_COMBINATION_INVALID";

  private static final int SCALE = 5;
  private static final BigDecimal THRESHOLD_SCALE = BigDecimal.valueOf(1, SCALE);

  public ObjectiveAnswerCheckResult judge(NormalizedObjectiveQuestion question,
      ObjectiveAnswerDecision decision, BigDecimal confidenceThreshold,
      BigDecimal marginThreshold) {
    double theta = confidenceThreshold.doubleValue();
    double delta = marginThreshold.doubleValue();
    List<NormalizedOption> options = question.options();

    Set<String> originalIds = options.stream().filter(NormalizedOption::originalCorrect)
        .map(NormalizedOption::optionId)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    boolean questionScope = ObjectiveAnswerDecision.SCOPE_QUESTION.equals(decision.scope());
    Set<String> aiIds = new LinkedHashSet<>();
    List<ObjectiveAnswerCheckResult.OptionLine> lines = new ArrayList<>();
    double referenceConfidence;
    double aiConfidence;
    double minMargin;

    if (questionScope) {
      String choice = decision.choice();
      aiIds.add(choice);
      Map<String, Double> probabilities = decision.probabilities();
      List<Double> sorted = probabilities.values().stream().sorted(Comparator.reverseOrder())
          .toList();
      double top1 = sorted.isEmpty() ? 0.0 : sorted.get(0);
      double top2 = sorted.size() > 1 ? sorted.get(1) : 0.0;
      double choiceProb = probabilities.getOrDefault(choice, 0.0);
      double margin = Math.max(0.0, choiceProb - top2);
      aiConfidence = decision.confidence() == null ? 0.0 : decision.confidence();
      minMargin = margin;

      double minReference = Double.MAX_VALUE;
      for (NormalizedOption option : options) {
        double p = probabilities.getOrDefault(option.optionId(), 0.0);
        double referenceProbability = option.originalCorrect() ? p : 1.0 - p;
        minReference = Math.min(minReference, referenceProbability);
        lines.add(new ObjectiveAnswerCheckResult.OptionLine(option.optionId(), option.label(),
            option.text(), option.originalCorrect(), choice.equals(option.optionId()),
            choice.equals(option.optionId()) ? choice : null, aiConfidence, referenceProbability,
            top1, top2, margin));
      }
      referenceConfidence = minReference == Double.MAX_VALUE ? 0.0 : minReference;
      ObjectiveAnswerCheckResult.OptionResults optionResults =
          new ObjectiveAnswerCheckResult.OptionResults(ObjectiveAnswerDecision.SCOPE_QUESTION,
              choice, decision.confidence(), top1, top2, margin, lines);
      return assemble(question, originalIds, aiIds, referenceConfidence, aiConfidence, minMargin,
          optionResults, theta, delta);
    }

    Map<String, ObjectiveAnswerOptionDecision> decisions = decision.optionDecisions();
    double minReference = Double.MAX_VALUE;
    double minAi = Double.MAX_VALUE;
    double minMarginValue = Double.MAX_VALUE;
    for (NormalizedOption option : options) {
      ObjectiveAnswerOptionDecision sub = decisions.get(option.optionId());
      if (sub == null) {
        throw new IllegalStateException("缺少选项判断: " + option.optionId());
      }
      boolean aiCorrect = "Y".equals(sub.choice());
      if (aiCorrect) {
        aiIds.add(option.optionId());
      }
      double pYes = sub.probabilities().getOrDefault("Y", 0.0);
      double pNo = sub.probabilities().getOrDefault("N", 0.0);
      double referenceProbability = option.originalCorrect() ? pYes : pNo;
      minReference = Math.min(minReference, referenceProbability);
      minAi = Math.min(minAi, sub.confidence());
      minMarginValue = Math.min(minMarginValue, sub.margin());
      lines.add(new ObjectiveAnswerCheckResult.OptionLine(option.optionId(), option.label(),
          option.text(), option.originalCorrect(), aiCorrect, sub.choice(), sub.confidence(),
          referenceProbability, sub.top1Probability(), sub.top2Probability(), sub.margin()));
    }
    referenceConfidence = minReference == Double.MAX_VALUE ? 0.0 : minReference;
    aiConfidence = minAi == Double.MAX_VALUE ? 0.0 : minAi;
    minMargin = minMarginValue == Double.MAX_VALUE ? 0.0 : minMarginValue;
    ObjectiveAnswerCheckResult.OptionResults optionResults =
        new ObjectiveAnswerCheckResult.OptionResults(ObjectiveAnswerDecision.SCOPE_OPTION, null,
            null, null, null, minMargin, lines);
    return assemble(question, originalIds, aiIds, referenceConfidence, aiConfidence, minMargin,
        optionResults, theta, delta);
  }

  private ObjectiveAnswerCheckResult assemble(NormalizedObjectiveQuestion question,
      Set<String> originalIds, Set<String> aiIds, double referenceConfidence, double aiConfidence,
      double minMargin, ObjectiveAnswerCheckResult.OptionResults optionResults, double theta,
      double delta) {
    boolean consistent = originalIds.equals(aiIds);
    // 边界包含：概率差是浮点减法（0.6-0.4=0.19999…），比较时加极小容差；仍先比较后舍入
    boolean baseHigh = aiConfidence + 1e-9 >= theta && minMargin + 1e-9 >= delta;
    String resultCode;
    String warning = null;

    if (consistent) {
      resultCode = baseHigh && referenceConfidence + 1e-9 >= theta
          ? RESULT_CONSISTENT_HIGH : RESULT_LOW_CONFIDENCE;
    } else {
      resultCode = baseHigh ? RESULT_MISMATCH_HIGH : RESULT_LOW_CONFIDENCE;
    }

    // 多选 AI 最终正确项 <2：即便 baseHigh 也只能低置信，不能当成高置信修改建议
    if ("multi".equals(question.type()) && aiIds.size() < 2) {
      if (RESULT_MISMATCH_HIGH.equals(resultCode) || RESULT_CONSISTENT_HIGH.equals(resultCode)) {
        resultCode = RESULT_LOW_CONFIDENCE;
      }
      warning = WARNING_AI_COMBINATION_INVALID;
    }

    return new ObjectiveAnswerCheckResult(resultCode, consistent, joinKeys(originalIds),
        joinKeys(aiIds), round(referenceConfidence), round(aiConfidence), round(minMargin),
        warning, optionResults);
  }

  private String joinKeys(Set<String> keys) {
    return String.join(",", keys);
  }

  private BigDecimal round(double value) {
    return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
  }

  /** 暴露阈值精度常量，供导出与测试断言使用。 */
  public static BigDecimal thresholdScale() {
    return THRESHOLD_SCALE;
  }
}
