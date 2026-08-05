package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Size;

public record CompetitionStageFinalPodiumReq(
    Long firstContestantMemberId,
    Long secondContestantMemberId,
    Long thirdContestantMemberId,
    @Size(max = 1000) String specialExplanation) { }
