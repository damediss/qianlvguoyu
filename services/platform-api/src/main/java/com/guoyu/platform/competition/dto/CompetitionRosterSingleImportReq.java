package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionRosterSingleImportReq(
    @NotNull Long orgId,
    @NotBlank @Size(max = 64) String employeeNo,
    @NotBlank @Size(max = 128) String name,
    @NotBlank @Size(max = 32) String idCardNo,
    @NotBlank @Size(max = 32) String mobile,
    @Size(max = 64) String workType,
    @Size(max = 32) String level) {
}
