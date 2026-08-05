package com.guoyu.platform.competition.vo;

public record CompetitionTheorySessionRoomVO(
    Long id,
    Long roomId,
    String roomName,
    String address,
    Integer capacity,
    Integer sortOrder
) {
}
