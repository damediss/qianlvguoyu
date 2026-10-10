package com.guoyu.platform.competition.vo;

public record CompetitionStagePaperAssignmentPlayerVO(
    String contestantProfileId,
    String contestantMemberId,
    String registrationId,
    String playerId,
    String contestantNo,
    String name,
    String orgId,
    String orgName,
    String workTypeName,
    String levelCode,
    String levelName,
    String rosterLevel,
    String paperSnapshotId,
    String paperCode,
    String paperTitle,
    String assessmentSheetId,
    String assessmentSheetCode,
    String allocationSource,
    String assignmentStatus,
    String configurationStatus) {
}
