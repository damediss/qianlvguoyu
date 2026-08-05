package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CompetitionStagePlanResourceBatchReq(
    @NotEmpty List<@Valid CompetitionStagePlanResourceItemReq> resources) {
}
