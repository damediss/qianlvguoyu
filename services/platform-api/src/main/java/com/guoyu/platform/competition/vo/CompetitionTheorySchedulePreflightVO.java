package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionTheorySchedulePreflightVO(
    Long scheduleVersionId,
    boolean readyToPublish,
    int eligibleCandidateCount,
    int assignedCandidateCount,
    int totalCapacity,
    int capacityGap,
    List<String> issues
) {
}
