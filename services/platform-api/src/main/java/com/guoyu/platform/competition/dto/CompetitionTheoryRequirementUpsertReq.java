package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompetitionTheoryRequirementUpsertReq(
    @NotBlank String templateCode,
    @NotNull @Min(1) Integer durationMinutes,
    @NotNull @Min(0) Integer singleCount,
    @NotNull @DecimalMin("0") BigDecimal singleScore,
    @NotNull @Min(0) Integer multiCount,
    @NotNull @DecimalMin("0") BigDecimal multiScore,
    @NotNull @Min(0) Integer judgeCount,
    @NotNull @DecimalMin("0") BigDecimal judgeScore,
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100")
    BigDecimal totalScore,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal passScore
) {
}
