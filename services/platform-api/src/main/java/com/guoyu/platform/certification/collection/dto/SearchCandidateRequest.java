package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SearchCandidateRequest(
    @NotNull Long taskId,
    Long examSessionId,
    @NotBlank @Size(max = 80) String keyword
) {
}
