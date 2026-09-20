package com.guoyu.platform.competition.vo;

/** Stage-scope association between a practical paper snapshot and a competition stage. */
public record CompetitionPracticalPaperBindingVO(
    String id,
    String eventId,
    String stageId,
    String eventWorkTypeId,
    String eventWorkTypeLevelId,
    String paperSnapshotId) {
}
