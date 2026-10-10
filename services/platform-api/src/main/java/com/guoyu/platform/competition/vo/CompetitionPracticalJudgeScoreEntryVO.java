package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionPracticalJudgeScoreEntryVO(
    Integer paperItemNo,
    BigDecimal score,
    BigDecimal deduction,
    String remark) { }
