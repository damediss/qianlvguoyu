package com.guoyu.platform.certification.dto;

import java.time.LocalDate;

public record PreparationAssessorDispatchReq(
    String subject,
    String title,
    Long assessmentFileId,
    String assessmentFileName,
    Integer durationMin,
    LocalDate dueDate,
    String remark
) {
}
