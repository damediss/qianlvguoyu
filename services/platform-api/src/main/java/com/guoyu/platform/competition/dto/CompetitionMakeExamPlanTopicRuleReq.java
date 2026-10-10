package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompetitionMakeExamPlanTopicRuleReq(
    @NotNull Long blueprintNodeId,
    @NotNull @Min(0) Integer questionCount,
    @NotNull @DecimalMin("0.01") BigDecimal score,
    Boolean selected,
    @NotNull Integer sortOrder
) {
}
