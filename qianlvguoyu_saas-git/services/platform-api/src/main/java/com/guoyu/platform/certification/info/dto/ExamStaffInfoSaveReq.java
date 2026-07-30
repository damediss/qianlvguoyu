package com.guoyu.platform.certification.info.dto;

import jakarta.validation.constraints.NotBlank;

public record ExamStaffInfoSaveReq(
    @NotBlank String staffName,
    @NotBlank String phone,
    String accountUsername,
    String accountPassword
) {
  public ExamStaffInfoSaveReq(String staffName, String phone) {
    this(staffName, phone, null, null);
  }
}
