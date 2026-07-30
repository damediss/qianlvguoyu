package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrgUpdateReq(
    @NotNull Long parentId,
    @NotBlank @Size(max = 32) String orgType,
    @NotBlank @Size(max = 128) String name,
    Integer sortOrder,
    @NotBlank @Size(max = 32) String status
) {
}
