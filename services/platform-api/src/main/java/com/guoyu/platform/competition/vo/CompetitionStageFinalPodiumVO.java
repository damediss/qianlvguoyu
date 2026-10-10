package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionStageFinalPodiumVO(
    Long id,
    Long settlementId,
    Long planId,
    Long eventId,
    Long stageId,
    Long firstContestantMemberId,
    Long secondContestantMemberId,
    Long thirdContestantMemberId,
    String specialExplanation,
    String status,
    Long confirmedBy,
    LocalDateTime confirmedAt,
    Long publishedBy,
    LocalDateTime publishedAt) { }
