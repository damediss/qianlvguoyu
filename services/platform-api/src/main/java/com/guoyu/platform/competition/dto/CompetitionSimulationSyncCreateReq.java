package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionSimulationSyncCreateReq(@NotNull Long bindingId) {}
