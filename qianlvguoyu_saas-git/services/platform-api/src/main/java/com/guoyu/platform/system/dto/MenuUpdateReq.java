package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MenuUpdateReq(
    @NotNull Long parentId,
    @NotBlank @Size(max = 32) String menuType,
    @NotBlank @Size(max = 64) String name,
    @Size(max = 255) String path,
    @Size(max = 255) String component,
    @Size(max = 128) String permissionCode,
    @Size(max = 64) String icon,
    Integer sortOrder,
    Integer visible,
    Integer expanded,
    @NotBlank @Size(max = 32) String status
) {
}
