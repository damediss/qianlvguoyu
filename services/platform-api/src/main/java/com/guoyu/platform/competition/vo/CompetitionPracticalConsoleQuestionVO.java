package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionPracticalConsoleQuestionVO(
    String paperQuestionId,
    int sortOrder,
    String title,
    BigDecimal maxScore,
    BigDecimal score,
    String status) { }
