package com.guoyu.platform.competition.sorting.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionPlayerSortingTaskVO(
    String id,
    String eventId,
    String stageId,
    String stageName,
    Integer groupCount,
    Integer workstationCount,
    Integer playersPerGroup,
    Integer playerCount,
    String status,
    LocalDateTime createdAt,
    List<CompetitionPlayerSortingAssignmentVO> assignments) {
}
