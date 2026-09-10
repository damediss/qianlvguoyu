package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionPracticalScoreItemVO(
    String id,
    Integer itemNo,
    String itemName,
    String criterion,
    BigDecimal maxScore,
    BigDecimal maxDeduction,
    BigDecimal deductionStep,
    String status
) {
}
