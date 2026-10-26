package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Size;

public record CompetitionSimulationSyncConfirmReq(@Size(max = 500) String correctionReason) {}
