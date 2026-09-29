package com.guoyu.platform.competition.sorting.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionPlayerSortingTaskCreateReq(
    @NotNull @Min(1) @Max(200) Integer groupCount,
    @NotNull @Min(1) @Max(5000) Integer workstationCount,
    @NotNull @Min(1) @Max(5000) Integer playersPerGroup,
    @NotNull @Size(max = 5000) List<@NotNull @Positive Long> contestantProfileIds) {
}
