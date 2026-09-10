package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionFormalExamPlayerPaperMetaVO(
    Integer questionCount,
    BigDecimal totalScore,
    Integer answeredCount) {
}
