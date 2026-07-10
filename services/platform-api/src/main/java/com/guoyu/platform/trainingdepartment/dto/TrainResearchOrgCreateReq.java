package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

public record TrainResearchOrgCreateReq(
    @NotBlank String title,
    Long targetOrgId,
    String type,
    Integer provinceId,
    String provinceName,
    Integer cityId,
    String cityName,
    Integer areaId,
    String areaName,
    Integer sortNumber,
    Integer status
) {
}
