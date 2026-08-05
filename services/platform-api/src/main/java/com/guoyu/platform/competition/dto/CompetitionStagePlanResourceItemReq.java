package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CompetitionStagePlanResourceItemReq(
    @NotBlank @Size(max = 32) String contentType,
    @NotBlank @Size(max = 64) String resourceType,
    Long resourceId,
    @Size(max = 128) String resourceCode,
    @Size(max = 128) String resourceVersion,
    LocalDateTime startAt,
    LocalDateTime endAt,
    @Size(max = 20000) String resourcePayload,
    String status) {
}
