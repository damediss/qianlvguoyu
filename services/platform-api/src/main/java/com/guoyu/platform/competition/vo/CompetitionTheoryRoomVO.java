package com.guoyu.platform.competition.vo;

public record CompetitionTheoryRoomVO(
    Long id,
    Long eventId,
    String name,
    String address,
    Integer capacity,
    String status,
    String remark
) {
}
