package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ExamExamDetailCreateReq(
    @NotBlank @Size(max = 50) String type,
    String content,
    String answer,
    Integer knowledgeId,
    Integer difficulty,
    BigDecimal score,
    String resolve,
    String keyword,
    Integer source,
    Integer status,
    Integer sortNumber
) {
}
