package com.guoyu.platform.competition.vo;

public record CompetitionFormalExamConsoleRealtimeTicketVO(
    String ticket,
    int expiresIn,
    String wsPath,
    int heartbeatSeconds) {
}
