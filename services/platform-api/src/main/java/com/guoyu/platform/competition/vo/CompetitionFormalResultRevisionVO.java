package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionFormalResultRevisionVO(
    String id, String examSessionId, String candidateExamId, Integer revisionNo,
    Boolean current, String resultStatus, BigDecimal totalScore, BigDecimal fullScore,
    Integer scoreVersion, String correctionReason, LocalDateTime scoredTime) {
}
