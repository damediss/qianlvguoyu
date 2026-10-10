package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionFormalExamPlayerPaperSessionVO(
    String id,
    String status,
    LocalDateTime serverTime,
    LocalDateTime actualStartTime,
    LocalDateTime deadlineTime,
    Integer durationSeconds) {
}
