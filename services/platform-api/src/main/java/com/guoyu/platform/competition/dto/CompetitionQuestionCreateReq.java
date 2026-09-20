package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CompetitionQuestionCreateReq(
    @NotNull Long eventWorkTypeId,
    @NotNull Long eventWorkTypeLevelId,
    @NotBlank String contentType,
    @NotBlank String questionType,
    @NotBlank String stem,
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
  public CompetitionQuestionCreateReq(
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
