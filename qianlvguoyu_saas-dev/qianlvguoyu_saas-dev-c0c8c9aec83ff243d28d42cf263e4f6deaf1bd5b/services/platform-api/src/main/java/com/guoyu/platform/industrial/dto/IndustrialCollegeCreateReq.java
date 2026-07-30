package com.guoyu.platform.industrial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IndustrialCollegeCreateReq(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 80) String tag,
    @Size(max = 1000) String businessScope,
    @Size(max = 2000) String description,
    @Size(max = 1000) String trainingQualification,
    @Size(max = 1000) String certificationQualification,
    @Size(max = 300) String address,
    @Size(max = 50) String phone,
    @Size(max = 20) String status,
    Integer sortOrder
) {
}
