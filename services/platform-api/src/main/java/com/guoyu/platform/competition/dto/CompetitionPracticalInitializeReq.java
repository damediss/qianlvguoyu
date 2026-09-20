package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionPracticalInitializeReq(
    @NotNull Long eventWorkTypeLevelId) { }
