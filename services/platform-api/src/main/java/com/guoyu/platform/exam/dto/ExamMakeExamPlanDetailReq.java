package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ExamMakeExamPlanDetailReq(
    @NotBlank String type,
    @NotNull Integer num,
    BigDecimal point,
    Integer startPoint,
    Integer endPoint,
    Integer difficultyStart,
    Integer difficultyEnd
) {
}
