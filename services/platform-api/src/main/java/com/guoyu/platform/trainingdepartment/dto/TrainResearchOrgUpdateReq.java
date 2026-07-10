package com.guoyu.platform.trainingdepartment.dto;

public record TrainResearchOrgUpdateReq(
    String title,
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
