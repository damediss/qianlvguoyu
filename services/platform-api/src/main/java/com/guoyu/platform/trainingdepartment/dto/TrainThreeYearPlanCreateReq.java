package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrainThreeYearPlanCreateReq(
    @NotBlank String title,
    @NotNull Long targetOrgId,
    Long researchOrgId
) {}
