package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CompetitionPracticalRuleCreateReq(
    @NotNull Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    String ruleName,
    @NotEmpty List<@Valid CompetitionPracticalRuleItemReq> items) { }
