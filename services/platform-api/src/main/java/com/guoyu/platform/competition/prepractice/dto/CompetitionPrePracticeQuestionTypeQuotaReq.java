package com.guoyu.platform.competition.prepractice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CompetitionPrePracticeQuestionTypeQuotaReq(
    @NotBlank String questionType,
    @Min(0) Integer questionCount) {
}
