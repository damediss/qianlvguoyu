package com.guoyu.platform.ai.dto;

import java.util.Map;

/**
 * 归一后的 AI 判断（方案 §10.1）。
 *
 * <p>scope=question：单选/判断只有一个全局 choice，不得把全局 confidence 当成每个选项各自独立判断；
 * scope=option：多选每个选项一个二元子问题。
 */
public record ObjectiveAnswerDecision(
    String scope,
    String choice,
    Double confidence,
    Map<String, Double> probabilities,
    Map<String, ObjectiveAnswerOptionDecision> optionDecisions,
    int chunkCount) {

  public static final String SCOPE_QUESTION = "question";
  public static final String SCOPE_OPTION = "option";
}
