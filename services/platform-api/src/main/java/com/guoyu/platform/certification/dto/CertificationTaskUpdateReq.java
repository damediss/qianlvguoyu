package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CertificationTaskUpdateReq(
    @NotBlank @Size(max = 200) String taskName,
    @NotBlank @Size(max = 80) String taskManagerName,
    @Size(max = 200) String companyName,
    @Size(max = 80) String certificationType,
    List<CertificationTaskScopeReq> scopes,
    @Min(0) Integer plannedPeopleCount,
    LocalDate planStartDate,
    LocalDate planEndDate,
    LocalDate examDate,
    @Size(max = 40) String status,
    @Size(max = 40) String currentPhase,
    @Size(max = 1000) String remark
) {
}
