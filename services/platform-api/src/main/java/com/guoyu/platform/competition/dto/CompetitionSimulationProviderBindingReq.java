package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionSimulationProviderBindingReq(
    @NotNull Long stageId,
    @NotNull Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    @NotBlank @Size(max = 64) String externalHostTenantId,
    @NotBlank @Size(max = 128) String externalSessionId,
    @Size(max = 128) String moduleFilter) {}
