package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompleteCollectionRequest(
    @NotNull Long taskId,
    @NotNull Long candidateId,
    Long sessionId,
    @Size(max = 500) String remark
) {
}
