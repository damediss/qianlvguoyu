package com.guoyu.platform.ai.dto;

import java.util.Map;

/**
 * 多选题单选项二元判定。
 *
 * @param choice        Y / N
 * @param probabilities Y/N 概率分布
 * @param margin        p(choice) - p(另一候选)
 */
public record ObjectiveAnswerOptionDecision(
    String optionId,
    String choice,
    double confidence,
    Map<String, Double> probabilities,
    double margin,
    double top1Probability,
    double top2Probability) {
}
