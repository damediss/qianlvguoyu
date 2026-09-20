package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompetitionMakeExamPlanVO(
    String id,
    String eventId,
    String eventWorkTypeId,
    String eventWorkTypeLevelId,
    String practicalQuestionId,
    String contentType,
    String title,
    String examType,
    Integer durationMinutes,
    BigDecimal totalScore,
    BigDecimal passScore,
    Integer paperCount,
    Integer planVersion,
    String status,
    String comments,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<CompetitionMakeExamPlanDetailVO> details,
    String blueprintId,
    List<CompetitionMakeExamPlanTopicRuleVO> topicRules,
    String randomSeed
) {
  public CompetitionMakeExamPlanVO(
      String id,
      String eventId,
      String eventWorkTypeId,
      String eventWorkTypeLevelId,
      String contentType,
      String title,
      String examType,
      Integer durationMinutes,
      BigDecimal totalScore,
      BigDecimal passScore,
      Integer paperCount,
      Integer planVersion,
      String status,
      String comments,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      List<CompetitionMakeExamPlanDetailVO> details) {
    this(id, eventId, eventWorkTypeId, eventWorkTypeLevelId, null, contentType,
        title, examType, durationMinutes, totalScore, passScore, paperCount,
        planVersion, status, comments, createdAt, updatedAt, details, null, List.of(), null);
  }

  public CompetitionMakeExamPlanVO(
      String id,
      String eventId,
      String eventWorkTypeId,
      String eventWorkTypeLevelId,
      String practicalQuestionId,
      String contentType,
      String title,
      String examType,
      Integer durationMinutes,
      BigDecimal totalScore,
      BigDecimal passScore,
      Integer paperCount,
      Integer planVersion,
      String status,
      String comments,
      java.time.LocalDateTime createdAt,
      java.time.LocalDateTime updatedAt,
      List<CompetitionMakeExamPlanDetailVO> details) {
    this(id, eventId, eventWorkTypeId, eventWorkTypeLevelId, practicalQuestionId, contentType,
        title, examType, durationMinutes, totalScore, passScore, paperCount, planVersion, status,
        comments, createdAt, updatedAt, details, null, List.of(), null);
  }

  /** Source-compatible constructor for the extended blueprint-aware response. */
  public CompetitionMakeExamPlanVO(
      String id,
      String eventId,
      String eventWorkTypeId,
      String eventWorkTypeLevelId,
      String practicalQuestionId,
      String contentType,
      String title,
      String examType,
      Integer durationMinutes,
      BigDecimal totalScore,
      BigDecimal passScore,
      Integer paperCount,
      Integer planVersion,
      String status,
      String comments,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      List<CompetitionMakeExamPlanDetailVO> details,
      String blueprintId,
      List<CompetitionMakeExamPlanTopicRuleVO> topicRules) {
    this(id, eventId, eventWorkTypeId, eventWorkTypeLevelId, practicalQuestionId, contentType,
        title, examType, durationMinutes, totalScore, passScore, paperCount, planVersion, status,
        comments, createdAt, updatedAt, details, blueprintId, topicRules, null);
  }
}
