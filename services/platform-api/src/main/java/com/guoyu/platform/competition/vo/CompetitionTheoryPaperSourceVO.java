package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionTheoryPaperSourceVO(
    Long paperId,
    Long planId,
    Long courseId,
    String planTitle,
    String paperTitle,
    Integer durationMinutes,
    BigDecimal totalScore,
    BigDecimal passScore,
    Integer questionCount
) {
}
