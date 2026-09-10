package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompetitionFormalCandidateResultVO(
    String id,
    String examSessionId,
    String candidateExamId,
    String resultStatus,
    BigDecimal fullScore,
    BigDecimal totalScore,
    Integer questionCount,
    Integer answeredCount,
    Integer correctCount,
    Integer wrongCount,
    Integer unansweredCount,
    Integer scoreVersion,
    LocalDateTime scoredTime,
    List<CompetitionFormalCandidateQuestionScoreVO> questionScores) {
}
