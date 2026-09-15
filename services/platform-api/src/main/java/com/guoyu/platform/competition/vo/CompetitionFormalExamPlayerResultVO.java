package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionFormalExamPlayerResultVO(
    String status, BigDecimal theoryScore, BigDecimal simulationScore,
    BigDecimal practicalScore, BigDecimal totalScore, Integer rank,
    Boolean tied, LocalDateTime publishedAt) {

  public static CompetitionFormalExamPlayerResultVO pending(String status) {
    return new CompetitionFormalExamPlayerResultVO(
        status, null, null, null, null, null, null, null);
  }
}
