package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotNull;

public record CourseItem(
    @NotNull Integer courseId,
    @NotNull String courseTitle,
    @NotNull Integer type
) {
}