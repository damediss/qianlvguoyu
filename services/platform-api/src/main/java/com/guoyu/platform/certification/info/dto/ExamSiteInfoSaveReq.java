package com.guoyu.platform.certification.info.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ExamSiteInfoSaveReq(
    @NotBlank String siteName,
    @NotBlank String siteType,
    @NotBlank String address,
    @Min(1) Integer seatCount,
    String filingNo,
    String contactName,
    String contactPhone,
    String equipmentCondition,
    String materialsDescription,
    String status,
    String remark
) {
}
