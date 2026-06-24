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
    String remark,
    Long taskScopeId,
    String admissionTicketNo
) {
  public PreparationCandidateSaveReq(
      String candidateName, String idCardNo, String phone, String levelCode, String levelName,
      String importSource, String orgName, String remark
  ) {
    this(candidateName, idCardNo, phone, levelCode, levelName, importSource, orgName, remark, null, null);
  }

  public PreparationCandidateSaveReq(
      String candidateName, String idCardNo, String phone, String levelCode, String levelName,
      String importSource, String orgName, String remark, Long taskScopeId
  ) {
    this(candidateName, idCardNo, phone, levelCode, levelName, importSource, orgName, remark, taskScopeId, null);
  }
}
