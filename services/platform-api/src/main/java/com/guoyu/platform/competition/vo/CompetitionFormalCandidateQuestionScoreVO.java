package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionFormalCandidateQuestionScoreVO(
    String questionSnapshotId,
    Integer questionOrder,
    String questionType,
    Integer answerVersion,
    Boolean answered,
    Boolean correct,
    BigDecimal maxScore,
    BigDecimal awardedScore) {
}
