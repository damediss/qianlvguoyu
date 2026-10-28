package com.guoyu.platform.competition.prepractice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionPrePracticeTaskCreateReq(
    @NotNull Long eventId,
    @NotNull Long eventWorkTypeId,
    @NotBlank String eventWorkTypeLevelCode,
    @NotNull Long stageId,
    @NotBlank @Size(max = 255) String title,
    @NotNull @Valid CompetitionPrePracticeSourceScopeReq sourceScope,
    @NotEmpty @Valid List<CompetitionPrePracticeQuestionTypeQuotaReq> questionTypeQuotas) {
}
