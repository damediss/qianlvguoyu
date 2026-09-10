package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionStagePaperAssignmentSummaryVO(
    String eventId,
    String eventName,
    String stageId,
    String stageName,
    String eventWorkTypeId,
    String workTypeName,
    String eventWorkTypeLevelId,
    String levelCode,
    String levelName,
    Integer eligibleCount,
    Integer paperCount,
    Integer assignedCount,
    Integer unassignedCount,
    boolean confirmed,
    List<CompetitionStagePaperAssignmentPaperVO> papers,
    boolean formalExamLocked,
    String formalExamSessionId,
    String formalExamStatus) {
}
