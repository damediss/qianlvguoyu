package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotNull;

public record PreparationTaskAssessorSaveReq(
    @NotNull Long assessorId,
    String taskRole,
    String roleName,
    String remark
) {
}
