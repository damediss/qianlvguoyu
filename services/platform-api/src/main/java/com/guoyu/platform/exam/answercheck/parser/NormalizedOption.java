package com.guoyu.platform.exam.answercheck.parser;

/**
 * 归一选项（方案 §8.1）。
 *
 * @param optionId        JEV criteria 的内部 key：opt_0、opt_1……按原数组下标生成，永不随展示顺序变化
 * @param originalIndex   原数组下标
 * @param label           展示字母 A/B…；判断题映射为 T/F
 * @param text            纯文本
 * @param originalCorrect 原答案标记
 */
public record NormalizedOption(
    String optionId,
    int originalIndex,
    String label,
    String text,
    boolean originalCorrect) {
}
