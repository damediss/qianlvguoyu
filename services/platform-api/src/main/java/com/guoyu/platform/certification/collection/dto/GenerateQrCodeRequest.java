package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GenerateQrCodeRequest(
    @NotNull Long taskId,
    Long examSessionId,
    Boolean regenerateExisting,
    @Size(max = 500) List<Long> candidateIds
) {
}
