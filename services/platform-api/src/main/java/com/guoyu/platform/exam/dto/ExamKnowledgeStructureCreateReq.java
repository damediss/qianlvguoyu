package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExamKnowledgeStructureCreateReq(
    @NotBlank @Size(max = 255) String title,
    Integer type,
    Integer courseId,
    Integer parentId,
    @Size(max = 200) String seriesNo,
    String important,
    Integer status,
    Integer sortNumber,
    @Size(max = 500) String cover,
    @Size(max = 1000) String intro,
    Integer amount
) {
}
