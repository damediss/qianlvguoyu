package com.guoyu.platform.certification.info.vo;

public record ExamStaffInfoVO(
    Long id,
    String staffName,
    String phone,
    String maskedPhone,
    String auditStatus,
    String workStatus,
    String assignedTaskNos
) {
}
