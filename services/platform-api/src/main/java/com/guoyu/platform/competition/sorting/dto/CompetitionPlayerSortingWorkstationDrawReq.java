package com.guoyu.platform.competition.sorting.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionPlayerSortingWorkstationDrawReq(
    @NotNull @Size(min = 1, max = 500) List<@NotNull @Positive Integer> groupNos) {
}
