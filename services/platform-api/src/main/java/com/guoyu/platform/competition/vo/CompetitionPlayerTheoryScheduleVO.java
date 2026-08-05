package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionPlayerTheoryScheduleVO(
    Long scheduleVersionId,
    Long eventId,
    String eventName,
    String workTypeName,
    String sessionName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String roomName,
    String address,
    String seatNo,
    String contestantNo
) {
}
