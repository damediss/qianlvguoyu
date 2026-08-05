package com.guoyu.platform.competition.vo;

public record CompetitionStageReferenceItemVO(
    Long id,
    Long contestantMemberId,
    Integer sourceRank,
    String selectionStatus,
    String reason) { }
