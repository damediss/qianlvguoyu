package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionTheoryScheduleVersionVO(
    Long id,
    Long eventId,
    Long stageId,
    Long eventWorkTypeId,
    Long scoreRuleId,
    Integer versionNo,
    String status,
    String randomBatchId,
    LocalDateTime generatedAt,
    LocalDateTime publishedAt,
    LocalDateTime withdrawnAt,
    String withdrawReason,
    int eligibleCandidateCount,
    int assignedCandidateCount,
    int totalCapacity,
    int capacityGap,
    List<CompetitionTheorySessionVO> sessions,
    List<CompetitionTheoryAssignmentVO> assignments
) {
}
