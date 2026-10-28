package com.guoyu.platform.competition.prepractice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record CompetitionPlayerPrePracticeAnswerSaveReq(
    @NotNull(message = "作答内容不能为空") JsonNode answer) {
}
