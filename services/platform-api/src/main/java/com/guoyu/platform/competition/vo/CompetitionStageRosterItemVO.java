package com.guoyu.platform.competition.vo;

public record CompetitionStageRosterItemVO(
    Long id,
    Long snapshotId,
    Long contestantMemberId,
    Long contestantProfileId,
    Long registrationId,
    String inclusionStatus,
    String eligibilityStatus,
    String reason) { }
