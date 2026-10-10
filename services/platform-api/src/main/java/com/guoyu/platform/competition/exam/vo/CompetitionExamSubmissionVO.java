package com.guoyu.platform.competition.exam.vo;

import java.time.LocalDateTime;

public record CompetitionExamSubmissionVO(
    Long attemptId,
    String status,
    String submitType,
    LocalDateTime submittedAt,
    boolean submitted) {
}
