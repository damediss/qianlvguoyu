package com.guoyu.platform.certification.info.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AssessorInfoSaveReq(
    @NotBlank String assessorName,
    @NotBlank String certificateNo,
    @NotBlank String workTypeScope,
    String levelScope,
    @NotNull LocalDate validUntil,
    String phone,
    String materialsDescription,
    String remark
) {
}
