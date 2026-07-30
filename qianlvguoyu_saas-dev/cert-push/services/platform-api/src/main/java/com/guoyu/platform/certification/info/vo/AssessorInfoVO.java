package com.guoyu.platform.certification.info.vo;

import java.time.LocalDate;

public record AssessorInfoVO(
    Long id,
    String assessorName,
    String certificateNo,
    String workTypeScope,
    String levelScope,
    LocalDate validUntil,
    String maskedPhone,
    String auditStatus,
    String workStatus,
    String materialsDescription,
    String remark
) {
}
