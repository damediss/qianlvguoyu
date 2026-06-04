package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;

public record PreparationCandidateSaveReq(
    @NotBlank String candidateName,
    @NotBlank String idCardNo,
    String phone,
    String levelCode,
    String levelName,
    String importSource,
    String orgName,
    String remark
) {
}
