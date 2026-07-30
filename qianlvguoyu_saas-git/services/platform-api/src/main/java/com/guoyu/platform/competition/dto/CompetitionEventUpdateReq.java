package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CompetitionEventUpdateReq(
    @NotBlank @Size(max = 128) String name,
    Integer registrationAuditRequired,
    LocalDateTime startAt,
    LocalDateTime endAt,
    @Size(max = 2000) String description
) {
}
