package com.guoyu.platform.exam.answercheck.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 人工复核请求（方案 §13.4）。 */
public record ObjectiveAnswerReviewReq(
    @NotBlank String reviewStatus,
    @Size(max = 500) String note,
    @NotNull Integer expectedVersion) {
}
