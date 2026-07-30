package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DictItemUpdateReq(
    @NotBlank @Size(max = 128) String itemLabel,
    Integer sortOrder,
    @NotBlank @Size(max = 32) String status
) {
}
