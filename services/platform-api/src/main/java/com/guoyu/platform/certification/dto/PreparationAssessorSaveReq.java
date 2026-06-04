package com.guoyu.platform.certification.dto;

import java.time.LocalDate;

public record PreparationAssessorSaveReq(
    Long userId,
    String assessorName,
    String certificateNo,
    String workTypeScope,
    String levelScope,
    LocalDate validUntil,
    String phone,
    String status,
    String remark
) {
}
