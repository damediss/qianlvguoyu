package com.guoyu.platform.exam.answercheck.vo;

import java.util.List;

/** 试题分页（方案 §5.1）。题干只给纯文本摘要，禁止渲染原始 HTML。 */
public record ObjectiveAnswerQuestionPageVO(
    List<QuestionRow> items,
    long page,
    long pageSize,
    long total) {

  public record QuestionRow(
      Integer detailId,
      String questionType,
      String questionTypeLabel,
      String stemPreview,
      String originalAnswer,
      Integer source,
      String sourceLabel,
      Long workTypeId,
      String workTypeTitle,
      Integer levelCode,
      String levelLabel,
      Integer courseId,
      String courseTitle,
      String invalidReason,
      String contentQuality) {
  }
}
