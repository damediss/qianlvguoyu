package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchJoinQueueRequest(
    @NotNull Long taskId,
    Long examSessionId,
    @NotEmpty List<Long> candidateIds,
    String matchMethod
) {
}
