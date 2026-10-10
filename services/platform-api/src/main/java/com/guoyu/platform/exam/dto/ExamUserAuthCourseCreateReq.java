package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ExamUserAuthCourseCreateReq(
    @NotBlank @Size(max = 255) String realname,
    @Size(max = 50) String idCard,
    @NotEmpty List<CourseItem> items,
    Integer status,
    Integer sortNumber
) {
}