package com.guoyu.platform.ai.dto;

import java.util.List;

/** AI 层归一题目：不含原解析、租户/组织/用户信息，也不含 answer JSON 原文。 */
public record ObjectiveAnswerQuestion(
    Integer detailId,
    String questionType,
    String stemText,
    String contextPath,
    List<ObjectiveAnswerOption> options) {
}
