package com.guoyu.platform.trainingdepartment.vo;

import com.guoyu.platform.trainingdepartment.support.ResearchCollectionFieldOptionsSupport.ResearchCollectionFieldOptions;
import java.time.LocalDateTime;

public record TrainResearchOrgVO(
    Long id,
    String title,
    Long targetOrgId,
    String targetOrgName,
    Long activeSignUpId,
    Integer personnelCount,
    ResearchCollectionFieldOptions collectionFieldOptions,
    String type,
    Integer provinceId,
    String provinceName,
    Integer cityId,
    String cityName,
    Integer areaId,
    String areaName,
    Long userId,
    Integer sortNumber,
    Integer status,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {
}
