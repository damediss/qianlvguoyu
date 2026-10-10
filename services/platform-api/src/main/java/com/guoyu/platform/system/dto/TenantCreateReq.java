package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantCreateReq(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 128) String name,
    @Size(max = 32) String status,
    @Size(max = 64) String contactName,
    @Size(max = 32) String contactPhone,
    @Size(max = 512) String remark
) {
}
