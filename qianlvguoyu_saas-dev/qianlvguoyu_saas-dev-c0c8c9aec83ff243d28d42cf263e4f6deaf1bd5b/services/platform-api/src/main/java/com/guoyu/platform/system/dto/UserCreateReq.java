package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserCreateReq(
    Long orgId,
    @NotBlank @Size(max = 64) String username,
    @NotBlank @Size(min = 8, max = 128) String password,
    @NotBlank @Size(max = 64) String displayName,
    @NotBlank @Size(max = 32) String userType,
    @Size(max = 32) String phone,
    @Size(max = 128) String email,
    @Size(max = 32) String status,
    @NotNull List<Long> roleIds
) {
}
