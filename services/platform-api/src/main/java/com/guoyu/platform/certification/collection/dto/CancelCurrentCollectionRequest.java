package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotNull;

public record CancelCurrentCollectionRequest(
    @NotNull Long taskId,
    @NotNull Long candidateId
) {
}
