package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExamWorkTypeCreateReq(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 200) String seriesNo,
    @Size(max = 200) String levelsNo,
    Integer parentId,
    Integer status,
    Integer sortNumber
) {
}
