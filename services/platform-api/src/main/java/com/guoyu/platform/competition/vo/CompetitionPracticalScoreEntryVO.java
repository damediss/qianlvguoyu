package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionPracticalScoreEntryVO(
    Long id, Long ruleItemId, Long paperQuestionId, Integer paperItemNo,
    BigDecimal score, BigDecimal deduction, String remark) { }
