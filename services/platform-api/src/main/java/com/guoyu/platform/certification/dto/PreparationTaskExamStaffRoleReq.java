package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PreparationTaskExamStaffRoleReq(
    @NotBlank @Size(max = 40) String taskRole,
    @Size(max = 80) String roleName
) {
}
