package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotNull;

public record ExamTaskExamAssignExpertReq(
    @NotNull Integer taskId,
    @NotNull Integer expertId,
    @NotNull Integer role
) {
}
