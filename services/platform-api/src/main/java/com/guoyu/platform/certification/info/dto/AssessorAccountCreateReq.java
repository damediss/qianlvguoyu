package com.guoyu.platform.certification.info.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssessorAccountCreateReq(
    @NotBlank @Size(max = 64) String username,
    @NotBlank @Size(min = 8, max = 128) String password
) {
}
