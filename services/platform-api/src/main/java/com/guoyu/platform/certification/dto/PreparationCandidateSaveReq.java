package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record PreparationCandidateSaveReq(
    @NotBlank String candidateName,
    @NotBlank String idCardNo,
    String phone,
    String levelCode,
    String levelName,
    String importSource,
    String orgName,
    String remark,
    Long taskScopeId,
    String admissionTicketNo,
    String gender,
    String education,
    LocalDate graduateDate,
    String companyAddress,
    String zipCode,
    LocalDate workStartDate,
    String originalWorkTypeName,
    String originalLevelName,
    String originalCertNo,
    String applyWorkYears,
    String companyCreditCode,
    String workTypeCode,
    String workDirection,
    String workResume,
    String trainingSummary,
    String homeAddress,
    String school,
    String major,
    String nation
) {
  public PreparationCandidateSaveReq(
      String candidateName, String idCardNo, String phone, String levelCode, String levelName,
      String importSource, String orgName, String remark
  ) {
    this(candidateName, idCardNo, phone, levelCode, levelName, importSource, orgName, remark,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null);
  }

  public PreparationCandidateSaveReq(
      String candidateName, String idCardNo, String phone, String levelCode, String levelName,
      String importSource, String orgName, String remark, Long taskScopeId
  ) {
    this(candidateName, idCardNo, phone, levelCode, levelName, importSource, orgName, remark,
        taskScopeId, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null);
  }

  public PreparationCandidateSaveReq(
      String candidateName, String idCardNo, String phone, String levelCode, String levelName,
      String importSource, String orgName, String remark, Long taskScopeId, String admissionTicketNo
  ) {
    this(candidateName, idCardNo, phone, levelCode, levelName, importSource, orgName, remark,
        taskScopeId, admissionTicketNo, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null);
  }
}
