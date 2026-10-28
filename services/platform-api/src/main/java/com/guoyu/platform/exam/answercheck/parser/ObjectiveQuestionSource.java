package com.guoyu.platform.exam.answercheck.parser;

/**
 * 待解析试题的原始快照（只读，不含任何租户/组织/用户信息，避免误发上游）。
 */
public record ObjectiveQuestionSource(
    Integer detailId,
    String type,
    String contentHtml,
    String answerJson,
    String audio,
    String video,
    String contextPath) {
}
