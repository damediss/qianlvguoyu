package com.guoyu.platform.certification.info.dto;

import jakarta.validation.constraints.NotBlank;

public record ExamStaffInfoSaveReq(
    @NotBlank String staffName,
    @NotBlank String phone
) {
}
