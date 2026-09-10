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
    @NotNull @Valid List<@Valid CompetitionMakeExamPlanDetailReq> details
) {
}
