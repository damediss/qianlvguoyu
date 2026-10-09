package com.guoyu.platform.competition.sorting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionPlayerSortingWorkstationsReq(
    @NotBlank @Size(max = 16) String label,
    @NotNull @Size(min = 1, max = 500) List<@NotBlank @Size(max = 32) String> codes) {
}
