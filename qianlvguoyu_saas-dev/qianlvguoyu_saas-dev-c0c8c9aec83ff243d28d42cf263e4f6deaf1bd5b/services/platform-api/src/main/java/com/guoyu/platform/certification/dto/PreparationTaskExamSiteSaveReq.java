package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotNull;

public record PreparationTaskExamSiteSaveReq(
    @NotNull Long siteId,
    String examDate,
    String startTime,
    String endTime,
    String examType,
    Integer arrangedCount,
    String contactName,
    String contactPhone,
    String remark
) {
}
