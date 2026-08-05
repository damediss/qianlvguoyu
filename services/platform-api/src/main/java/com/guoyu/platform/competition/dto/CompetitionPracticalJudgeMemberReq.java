package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionPracticalJudgeMemberReq(
    @NotNull Long refereeMemberId,
    String memberRole) { }
