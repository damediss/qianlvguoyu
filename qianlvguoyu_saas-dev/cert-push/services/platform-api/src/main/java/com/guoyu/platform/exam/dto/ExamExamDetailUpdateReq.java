package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ExamExamDetailUpdateReq(
    @Size(max = 50) String type,
    String content,
    String answer,
    Integer knowledgeId,
    Integer difficulty,
    BigDecimal score,
    String resolve,
    String keyword,
    Integer source,
    Integer status,
    Integer sortNumber,
    Integer checkStatus,
    Integer finalCheckStatus
) {
}
