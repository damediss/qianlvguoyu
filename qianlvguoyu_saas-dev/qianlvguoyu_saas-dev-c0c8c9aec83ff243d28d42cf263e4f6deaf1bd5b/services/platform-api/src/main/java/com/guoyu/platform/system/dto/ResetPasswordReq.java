package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordReq(
    @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
