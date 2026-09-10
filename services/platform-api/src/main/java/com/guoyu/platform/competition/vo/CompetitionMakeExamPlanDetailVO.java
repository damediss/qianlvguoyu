package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionMakeExamPlanDetailVO(
    String id,
    String questionType,
    Integer questionCount,
    BigDecimal point,
    Integer difficultyStart,
    Integer difficultyEnd,
    Integer sortOrder
) {
}
