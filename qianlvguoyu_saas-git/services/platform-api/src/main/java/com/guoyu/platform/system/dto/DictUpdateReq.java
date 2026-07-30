package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DictUpdateReq(
    @NotBlank @Size(max = 64) String name,
    @Size(max = 1000) String value,
    @NotBlank @Size(max = 32) String status
) {
}
