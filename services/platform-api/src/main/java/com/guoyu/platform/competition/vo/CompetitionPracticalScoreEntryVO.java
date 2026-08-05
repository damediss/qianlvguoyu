package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionPracticalScoreEntryVO(
    Long id, Long ruleItemId, BigDecimal score, BigDecimal deduction, String remark) { }
