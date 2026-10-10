package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionTheorySessionVO(
    Long id,
    Long paperSnapshotId,
    String sessionCode,
    String sessionName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    Integer totalCapacity,
    String status,
    List<CompetitionTheorySessionRoomVO> rooms
) {
}
