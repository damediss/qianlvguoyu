package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;

public record PreparationExamSiteSaveReq(
    @NotBlank String siteName,
    String siteType,
    @NotBlank String address,
    Integer seatCount,
    String filingNo,
    String contactName,
    String contactPhone,
    String status,
    String equipmentCondition,
    String remark
) {
}
