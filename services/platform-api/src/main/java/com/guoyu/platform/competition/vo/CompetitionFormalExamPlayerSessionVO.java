package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionFormalExamPlayerSessionVO(
    String id,
    String status,
    String examType,
    Integer durationSeconds,
    LocalDateTime actualStartTime,
    LocalDateTime deadlineTime) {
}
