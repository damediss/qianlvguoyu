package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CompetitionPracticalAssignmentReq(
    @NotNull Long sessionId,
    @NotNull Long judgeGroupId,
    @NotNull Long rosterItemId,
    @NotNull Long contestantMemberId,
    String workstationCode,
    @NotEmpty List<@NotNull Long> refereeMemberIds) { }
