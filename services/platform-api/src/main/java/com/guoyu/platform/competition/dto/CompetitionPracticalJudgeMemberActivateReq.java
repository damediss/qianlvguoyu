package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionPracticalJudgeMemberActivateReq(
    @NotNull Long replacedMemberId,
    @Size(min = 2, max = 1000) String reason) { }
