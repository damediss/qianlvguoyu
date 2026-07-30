package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JoinQueueRequest(
    @NotNull Long taskId,
    @NotNull Long candidateId,
    @NotBlank @Size(max = 32) String matchMethod
) {
}
