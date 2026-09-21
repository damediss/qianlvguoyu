package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarkInfoErrorRequest(
    @NotNull Long taskId,
    Long examSessionId,
    @NotNull Long candidateId,
    @NotBlank @Size(max = 1000) String reason
) {
}
