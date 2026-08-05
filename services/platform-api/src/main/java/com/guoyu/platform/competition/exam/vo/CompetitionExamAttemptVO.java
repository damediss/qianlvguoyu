package com.guoyu.platform.competition.exam.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionExamAttemptVO(
    Long attemptId,
    String status,
    LocalDateTime serverTime,
    LocalDateTime startAt,
    LocalDateTime endAt,
    long secondsRemaining,
    int questionCount,
    int answeredCount,
    List<CompetitionExamAnswerVO> answers) {
}
