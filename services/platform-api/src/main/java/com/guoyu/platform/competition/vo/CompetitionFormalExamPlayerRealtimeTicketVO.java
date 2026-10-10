package com.guoyu.platform.competition.vo;

public record CompetitionFormalExamPlayerRealtimeTicketVO(
    String ticket,
    int expiresIn,
    String wsPath,
    int heartbeatSeconds) {
}
