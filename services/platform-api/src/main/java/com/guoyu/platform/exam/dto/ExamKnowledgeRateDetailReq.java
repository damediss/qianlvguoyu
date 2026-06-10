package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotNull;

public record ExamKnowledgeRateDetailReq(
    @NotNull Integer knowledgeId,
    @NotNull Integer rate
) {
}
