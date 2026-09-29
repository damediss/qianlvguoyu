package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionPracticalAssessmentElementVO(
    String id,
    Integer itemNo,
    Integer elementOrder,
    String elementHtml,
    String itemName,
    Boolean allocationEnabled,
    BigDecimal maxScore,
    String standardHtml,
    BigDecimal maxDeduction,
    BigDecimal deductionStep,
    String status
) {
}
