package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionFormalExamPlayerSubmitVO(
    String candidateExamStatus,
    LocalDateTime submitTime,
    int answeredCount,
    int totalQuestionCount,
    int unansweredCount,
    LocalDateTime serverTime) {
}
