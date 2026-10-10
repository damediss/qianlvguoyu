package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Size;

public record CompetitionSimulationImportConfirmReq(
    @Size(max = 500, message = "更正原因不能超过500字") String correctionReason) {
}
