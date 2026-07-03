package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MatchByTokenRequest(
    @NotNull Long taskId,
    @NotBlank @Size(max = 128) String collectToken
) {
}
