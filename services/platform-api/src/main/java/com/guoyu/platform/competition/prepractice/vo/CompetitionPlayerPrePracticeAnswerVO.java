package com.guoyu.platform.competition.prepractice.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record CompetitionPlayerPrePracticeAnswerVO(
    String questionId,
    JsonNode userAnswer,
    boolean answered,
    String judgmentStatus,
    Boolean correct,
    String correctAnswer,
    Integer answeredCount,
    Integer questionCount,
    LocalDateTime answeredAt) {
}
