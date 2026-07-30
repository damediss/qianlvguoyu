package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ExamMakeExamPlanCreateReq(
    @NotBlank String title,
    @NotNull Integer courseId,
    Integer rateId,
    Integer type,
    @NotNull Integer examType,
    Integer time,
    Integer isComputer,
    Integer isPrint,
    Integer openRate,
    Integer totalPoint,
    Integer passPoint,
    Integer isAll,
    String comments,
    List<ExamMakeExamPlanDetailReq> detailList
) {
}
