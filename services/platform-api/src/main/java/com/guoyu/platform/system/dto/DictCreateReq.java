package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DictCreateReq(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 64) String name,
    @Size(max = 1000) String value,
    @Size(max = 32) String status
) {
}
