package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.Size;

public record CertificationTaskScopeReq(
    @Size(max = 80) String certificationType,
    @Size(max = 120) String workTypeName,
    @Size(max = 40) String levelCode,
    @Size(max = 80) String levelName,
    Integer plannedPeopleCount
) {
}

