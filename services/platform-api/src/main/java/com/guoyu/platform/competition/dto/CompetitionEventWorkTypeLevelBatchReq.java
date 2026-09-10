package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CompetitionEventWorkTypeLevelBatchReq(
    @NotNull List<@Valid CompetitionEventWorkTypeLevelItemReq> levels
) {
}
