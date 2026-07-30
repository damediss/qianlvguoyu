package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RemoveQueueRequest(
    @NotNull Long taskId,
    @NotNull Long queueId,
    @Size(max = 500) String removeReason
) {
}
