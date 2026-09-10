package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionFormalExamPlayerStatusCandidateVO(
    String id,
    String status,
    Integer answeredCount,
    Integer totalQuestionCount,
    LocalDateTime submitTime,
    LocalDateTime lastActiveTime) {
}
