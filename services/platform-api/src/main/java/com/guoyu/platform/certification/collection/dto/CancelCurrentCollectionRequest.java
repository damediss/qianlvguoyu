package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotNull;

public record CancelCurrentCollectionRequest(
    @NotNull Long taskId,
    Long examSessionId,
    @NotNull Long candidateId
) {
}
