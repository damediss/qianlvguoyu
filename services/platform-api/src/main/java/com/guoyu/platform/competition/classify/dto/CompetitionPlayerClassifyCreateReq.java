package com.guoyu.platform.competition.classify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionPlayerClassifyCreateReq(
    @NotNull @Size(min = 1, max = 20) List<@NotBlank @Size(max = 32) String> typeNames,
    @NotNull @Size(min = 1, max = 5000) List<@NotNull @Positive Long> contestantProfileIds) {
}
