package com.guoyu.platform.competition.exam.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionTheoryResultSummaryVO(
    Long attemptId,
    Long playerId,
    String contestantNo,
    String contestantName,
    String status,
    boolean absent,
    BigDecimal totalScore,
    String confirmationStatus,
    boolean published,
    LocalDateTime submittedAt) {
}
