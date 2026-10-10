package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompetitionTheoryRoomUpsertReq(
    @NotBlank @Size(max = 128) String name,
    @NotBlank @Size(max = 500) String address,
    @NotNull @Min(1) @Max(10000) Integer capacity,
    @Pattern(regexp = "enabled|disabled") String status,
    @Size(max = 1000) String remark
) {
}
