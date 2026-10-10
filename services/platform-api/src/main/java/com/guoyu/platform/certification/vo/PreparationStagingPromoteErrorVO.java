package com.guoyu.platform.certification.vo;

public record PreparationStagingPromoteErrorVO(
    Long stagingId,
    String candidateName,
    String idCardNo,
    String errorMessage
) {
}
