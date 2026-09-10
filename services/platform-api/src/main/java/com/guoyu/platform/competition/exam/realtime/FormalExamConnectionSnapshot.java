package com.guoyu.platform.competition.exam.realtime;

public record FormalExamConnectionSnapshot(
    Long accessSessionId,
    Integer generation,
    String accessSessionStatus,
    String connectionState,
    String lastSeenAt) {
}
