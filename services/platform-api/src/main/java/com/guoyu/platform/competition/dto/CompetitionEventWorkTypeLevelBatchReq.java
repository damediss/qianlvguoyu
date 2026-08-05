package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CompetitionEventWorkTypeLevelBatchReq(
    @NotEmpty List<@Valid CompetitionEventWorkTypeLevelItemReq> levels
) {
}
