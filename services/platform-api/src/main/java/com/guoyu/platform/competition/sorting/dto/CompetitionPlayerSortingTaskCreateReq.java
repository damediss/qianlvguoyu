package com.guoyu.platform.competition.sorting.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionPlayerSortingTaskCreateReq(
    @NotNull @Size(min = 1, max = 5000) List<@NotNull @Positive Long> contestantProfileIds) {
}
