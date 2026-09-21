package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExceptionReleaseRequest(
    @NotNull Long taskId,
    Long examSessionId,
    @NotNull Long candidateId,
    @NotBlank @Size(max = 32) String exceptionType,
    @NotBlank @Size(max = 32) String releaseScope,
    @NotBlank @Size(max = 1000) String reason,
    @Size(max = 128) String deviceSn,
    @Size(max = 32) String deviceStatus
) {
}
