package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionStagePaperAssignmentBatchReq(
    @NotNull @Size(max = 5000) List<@Valid CompetitionStagePaperAssignmentItemReq> assignments) {
}
