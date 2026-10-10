package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.util.List;

public record CompetitionMakeExamSnapshotVO(
    String id,
    String planId,
    String blueprintId,
    String eventId,
    String eventWorkTypeId,
    String eventWorkTypeLevelId,
    String contentType,
    String paperCode,
    Integer versionNo,
    String title,
    Integer durationMinutes,
    Integer questionCount,
    BigDecimal totalScore,
    BigDecimal passScore,
    String status,
    List<CompetitionMakeExamSnapshotQuestionVO> questions
) {
  public CompetitionMakeExamSnapshotVO(
      String id,
      String planId,
      String eventId,
      String eventWorkTypeId,
      String eventWorkTypeLevelId,
      String contentType,
      String paperCode,
      Integer versionNo,
      String title,
      Integer durationMinutes,
      Integer questionCount,
      BigDecimal totalScore,
      BigDecimal passScore,
      String status,
      List<CompetitionMakeExamSnapshotQuestionVO> questions) {
    this(id, planId, null, eventId, eventWorkTypeId, eventWorkTypeLevelId, contentType, paperCode,
        versionNo, title, durationMinutes, questionCount, totalScore, passScore, status, questions);
  }
}
