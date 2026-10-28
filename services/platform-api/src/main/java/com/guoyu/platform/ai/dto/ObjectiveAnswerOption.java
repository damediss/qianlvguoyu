package com.guoyu.platform.ai.dto;

/**
 * AI 层选项。originalCorrect 只用于本地判定与展示，绝不出现在请求里。
 */
public record ObjectiveAnswerOption(
    String optionId,
    int originalIndex,
    String label,
    String text,
    boolean originalCorrect) {
}
