package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CompetitionEventCreateReq(
    @NotBlank @Size(max = 128) String name,
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 32) String scope,
    Integer registrationAuditRequired,
    LocalDateTime startAt,
    LocalDateTime endAt,
    @Size(max = 2000) String description
) {
}
