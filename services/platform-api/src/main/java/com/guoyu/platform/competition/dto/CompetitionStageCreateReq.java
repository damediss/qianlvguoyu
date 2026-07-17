package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CompetitionStageCreateReq(
    @NotBlank @Size(max = 128) String name,
    @NotBlank @Size(max = 32) String stageType,
    @Min(0) Integer sortOrder,
    LocalDateTime startAt,
    LocalDateTime endAt,
    @Size(max = 2000) String description
) {
}
