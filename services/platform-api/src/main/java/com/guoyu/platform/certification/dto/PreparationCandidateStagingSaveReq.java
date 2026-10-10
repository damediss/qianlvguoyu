package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record PreparationCandidateStagingSaveReq(
    @NotBlank String candidateName,
    @NotBlank String idCardNo,
    String phone,
    String gender,
    LocalDate birthDate,
    String education,
    LocalDate graduateDate,
    String school,
    String major,
    String nation,
    String homeAddress,
    String companyName,
    String companyAddress,
    String zipCode,
    String companyCreditCode,
    String industrialPark,
    LocalDate workStartDate,
    String originalWorkTypeName,
    String originalLevelName,
    String originalCertNo,
    String applyWorkTypeName,
    String certWorkTypeName,
    String workDirection,
    String workTypeCode,
    String applyLevelCode,
    String applyLevelName,
    String certLevelName,
    String applyWorkYears,
    String applyCondition,
    String workResume,
    String trainingSummary,
    String remark
) {
}
