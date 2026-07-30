package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrgCreateReq(
    @NotNull Long parentId,
    @NotBlank @Size(max = 32) String orgType,
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 128) String name,
    Integer sortOrder,
    @Size(max = 32) String status
) {
}
