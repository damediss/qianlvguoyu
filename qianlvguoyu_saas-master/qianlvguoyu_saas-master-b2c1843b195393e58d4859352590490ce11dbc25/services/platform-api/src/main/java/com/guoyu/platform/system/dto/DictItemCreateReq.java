package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DictItemCreateReq(
    @NotBlank @Size(max = 64) String itemValue,
    @NotBlank @Size(max = 128) String itemLabel,
    Integer sortOrder,
    @Size(max = 32) String status
) {
}
