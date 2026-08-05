package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CompetitionStageContentItemReq(
    @NotBlank @Pattern(regexp = "theory|simulation|practical") String contentType,
    @Pattern(regexp = "enabled|disabled") String status,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal weight,
    @Size(max = 64) String providerCode
) {
}
