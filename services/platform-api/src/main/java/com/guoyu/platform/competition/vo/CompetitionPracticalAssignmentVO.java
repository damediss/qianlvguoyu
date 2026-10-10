package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionPracticalAssignmentVO(
    Long id, Long planId, Long sessionId, Long judgeGroupId, Long rosterItemId,
    Long contestantMemberId, Long refereeMemberId, String workstationCode,
    String status, LocalDateTime assignedAt) { }
