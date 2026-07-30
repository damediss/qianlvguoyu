package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleCreateReq(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 64) String name,
    @NotBlank @Size(max = 32) String roleType,
    @Size(max = 32) String status,
    @Size(max = 512) String remark
) {
}
