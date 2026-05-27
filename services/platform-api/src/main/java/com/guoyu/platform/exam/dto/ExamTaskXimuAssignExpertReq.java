package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotNull;

public record ExamTaskXimuAssignExpertReq(
    @NotNull Integer taskId,
    @NotNull Integer expertId,
    @NotNull Integer role
) {
}
