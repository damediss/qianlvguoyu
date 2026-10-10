package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotNull;

public record StartNextRequest(
    @NotNull Long taskId,
    Long examSessionId
) {
}
