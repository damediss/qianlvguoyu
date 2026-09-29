package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** One scoring element under an assessment content. */
public record CompetitionPracticalAssessmentElementReq(
    @NotNull Integer elementOrder,
    @NotBlank String elementHtml,
    @NotNull Boolean allocationEnabled,
    @DecimalMin("0.01") @DecimalMax("100") BigDecimal maxScore,
    @NotBlank String standardHtml,
    @DecimalMin("0") @DecimalMax("100") BigDecimal maxDeduction,
    @DecimalMin("0.01") @DecimalMax("100") BigDecimal deductionStep,
    String itemName
) {
  public CompetitionPracticalAssessmentElementReq(
      Integer elementOrder, String elementHtml, Boolean allocationEnabled, BigDecimal maxScore,
      String standardHtml, BigDecimal maxDeduction, BigDecimal deductionStep) {
    this(elementOrder, elementHtml, allocationEnabled, maxScore, standardHtml,
        maxDeduction, deductionStep, null);
  }
}
