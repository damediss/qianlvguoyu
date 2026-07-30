package com.guoyu.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginReq(
    @NotBlank String tenantCode,
    @NotBlank String username,
    @NotBlank String password
) {
}
