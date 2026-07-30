package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.Size;

public record ExamKnowledgeStructureUpdateReq(
    @Size(max = 255) String title,
    Integer type,
    Integer courseId,
    Integer parentId,
    @Size(max = 200) String seriesNo,
    Integer important,
    Integer status,
    Integer sortNumber
) {
}
