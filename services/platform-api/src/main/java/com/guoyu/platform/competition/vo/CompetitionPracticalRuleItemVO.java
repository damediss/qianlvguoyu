package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionPracticalRuleItemVO(
    Long id, String itemCode, String itemName, Integer sortOrder, BigDecimal maxScore,
    Integer deductionAllowed, String criteriaPayload, String status) { }
