package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionFormalExamPaperVO(
    String id,
    String sourcePaperId,
    String paperCode,
    String paperName,
    Integer snapshotVersion,
    Integer questionCount,
    BigDecimal totalScore,
    Integer durationMinutes,
    Integer candidateCount) {
}
