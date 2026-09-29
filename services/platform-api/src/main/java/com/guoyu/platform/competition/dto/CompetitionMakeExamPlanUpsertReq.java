package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CompetitionMakeExamPlanUpsertReq(
    @NotNull Long eventWorkTypeId,
    @NotNull Long eventWorkTypeLevelId,
    @NotBlank String contentType,
    @NotBlank String title,
    String examType,
    @NotNull Integer durationMinutes,
    @NotNull @DecimalMin("0.01") BigDecimal totalScore,
    @NotNull @DecimalMin("0") BigDecimal passScore,
    String comments,
    @NotNull @Valid List<@Valid CompetitionMakeExamPlanDetailReq> details,
    Long practicalQuestionId,
    Long blueprintId,
    // Legacy theory clients may still send arbitrary topicRules. Validation and
    // use are deliberately deferred to the practical-only service path so an
    // obsolete theory/practical payload cannot block or affect automatic allocation.
    List<CompetitionMakeExamPlanTopicRuleReq> topicRules,
    Integer paperCount,
    Long randomSeed,
    List<Long> practicalQuestionIds
) {
  /** Source-compatible constructor for clients that predate explicit project selection. */
  public CompetitionMakeExamPlanUpsertReq(
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      String contentType,
      String title,
      String examType,
      Integer durationMinutes,
      BigDecimal totalScore,
      BigDecimal passScore,
      String comments,
      List<@Valid CompetitionMakeExamPlanDetailReq> details,
      Long practicalQuestionId,
      Long blueprintId,
      List<CompetitionMakeExamPlanTopicRuleReq> topicRules,
      Integer paperCount,
      Long randomSeed) {
    this(eventWorkTypeId, eventWorkTypeLevelId, contentType, title, examType, durationMinutes,
        totalScore, passScore, comments, details, practicalQuestionId, blueprintId, topicRules,
        paperCount, randomSeed, null);
  }

  /**
   * Source-compatible constructor for theory plans and existing callers. The
   * practical question selector is optional for legacy theory requests.
   */
  public CompetitionMakeExamPlanUpsertReq(
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      String contentType,
      String title,
      String examType,
      Integer durationMinutes,
      BigDecimal totalScore,
      BigDecimal passScore,
      String comments,
      List<@Valid CompetitionMakeExamPlanDetailReq> details) {
    this(eventWorkTypeId, eventWorkTypeLevelId, contentType, title, examType,
        durationMinutes, totalScore, passScore, comments, details, null, null, null, null, null, null);
  }

  public CompetitionMakeExamPlanUpsertReq(
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      String contentType,
      String title,
      String examType,
      Integer durationMinutes,
      BigDecimal totalScore,
      BigDecimal passScore,
      String comments,
      List<@Valid CompetitionMakeExamPlanDetailReq> details,
      Long practicalQuestionId) {
    this(eventWorkTypeId, eventWorkTypeLevelId, contentType, title, examType, durationMinutes,
        totalScore, passScore, comments, details, practicalQuestionId, null, null, null, null, null);
  }

  /** Source-compatible constructor for callers that already provide blueprint/topic rules. */
  public CompetitionMakeExamPlanUpsertReq(
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      String contentType,
      String title,
      String examType,
      Integer durationMinutes,
      BigDecimal totalScore,
      BigDecimal passScore,
      String comments,
      List<@Valid CompetitionMakeExamPlanDetailReq> details,
      Long practicalQuestionId,
      Long blueprintId,
      List<CompetitionMakeExamPlanTopicRuleReq> topicRules) {
    this(eventWorkTypeId, eventWorkTypeLevelId, contentType, title, examType, durationMinutes,
        totalScore, passScore, comments, details, practicalQuestionId, blueprintId, topicRules,
        null, null, null);
  }
}
