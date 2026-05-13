package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserUpdateReq(
    Long orgId,
    @NotBlank @Size(max = 64) String displayName,
    @NotBlank @Size(max = 32) String userType,
    @Size(max = 32) String phone,
    @Size(max = 128) String email,
    @NotBlank @Size(max = 32) String status,
    @NotNull List<Long> roleIds
) {
}
