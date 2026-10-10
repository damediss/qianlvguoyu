package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PreparationTaskExamStaffSaveReq(
    @NotNull Long staffId,
    @Size(max = 40) String taskRole,
    @Size(max = 80) String roleName,
    String remark
) {
}
