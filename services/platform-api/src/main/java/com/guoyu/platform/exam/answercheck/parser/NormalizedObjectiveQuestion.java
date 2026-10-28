package com.guoyu.platform.exam.answercheck.parser;

import java.util.List;

/**
 * 归一题目（方案 §8.1）。resolve 永不进入本结构，避免被序列化进 AI 请求。
 */
public record NormalizedObjectiveQuestion(
    Integer detailId,
    String type,
    String stemText,
    String contextPath,
    List<NormalizedOption> options,
    String sourceHash) {
}
