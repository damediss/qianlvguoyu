package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ExamKnowledgeRateCreateReq(
    @NotBlank String title,
    @NotNull Integer courseId,
    Integer type,
    Integer isStandard,
    String comments,
    List<ExamKnowledgeRateDetailReq> detailList
) {
}
