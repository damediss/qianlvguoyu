package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import java.util.List;

public record CompetitionQuestionUpdateReq(
    Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    String contentType,
    String questionType,
    String stem,
    String referenceAnswer,
    String fillAnswer,
    String optionsJson,
    Integer difficulty,
    String status,
    @Valid CompetitionPracticalQuestionDetailReq practicalDetail,
    @Valid List<@Valid CompetitionPracticalScoreItemReq> scoreItems,
    Long blueprintId,
    Long blueprintNodeId
) {
  /** Keeps source compatibility with callers created before blueprint binding was added. */
  public CompetitionQuestionUpdateReq(
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      String contentType,
      String questionType,
      String stem,
      String referenceAnswer,
      String fillAnswer,
      String optionsJson,
      Integer difficulty,
      String status,
      CompetitionPracticalQuestionDetailReq practicalDetail,
      List<@Valid CompetitionPracticalScoreItemReq> scoreItems) {
    this(eventWorkTypeId, eventWorkTypeLevelId, contentType, questionType, stem, referenceAnswer,
        fillAnswer, optionsJson, difficulty, status, practicalDetail, scoreItems, null, null);
  }
}
