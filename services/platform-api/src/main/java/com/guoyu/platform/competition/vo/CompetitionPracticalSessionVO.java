package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionPracticalSessionVO(
    Long id, Long planId, Long eventId, Long stageId, Long eventWorkTypeId,
    Long eventWorkTypeLevelId, String sessionCode, String sessionName,
    LocalDateTime startAt, LocalDateTime endAt, Integer capacity, Long resourceId,
    Long paperSnapshotId, String status) { }
