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
    @Valid List<@Valid CompetitionPracticalScoreItemReq> scoreItems
) {
}
