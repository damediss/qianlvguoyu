package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionFormalExamPlayerRealtimeAckReq(
    @NotBlank @Size(max = 32) String commandId) {
}
