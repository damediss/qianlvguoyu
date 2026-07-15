package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.trainingdepartment.support.ResearchCollectionFieldOptionsSupport.ResearchCollectionFieldOptions;

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
    Integer status,
    ResearchCollectionFieldOptions collectionFieldOptions
) {
}