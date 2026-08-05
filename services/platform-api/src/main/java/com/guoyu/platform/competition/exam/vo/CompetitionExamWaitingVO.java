package com.guoyu.platform.competition.exam.vo;

import java.time.LocalDateTime;

public record CompetitionExamWaitingVO(
    Long attemptId,
    String status,
    String eventName,
    String workTypeName,
    String sessionName,
    String roomName,
    String address,
    String seatNo,
    LocalDateTime startAt,
    LocalDateTime endAt,
    LocalDateTime serverTime,
    long secondsUntilStart,
    long secondsRemaining,
    boolean canStart) {
}
