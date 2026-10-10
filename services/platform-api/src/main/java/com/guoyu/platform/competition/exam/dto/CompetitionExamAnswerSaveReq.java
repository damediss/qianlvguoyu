package com.guoyu.platform.competition.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionExamAnswerSaveReq(
    @NotNull Long questionId,
    @NotBlank @Size(max = 64) String requestId,
    @Size(max = 20) List<@NotBlank @Size(max = 64) String> answerKeys,
    boolean markedForReview) {
}
