package com.guoyu.platform.certification.dto;

public record PreparationAssessorTaskReviewReq(
    Long reviewerAssessorId,
    String reviewStatus,
    String reviewNote
) {
}
