package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

public record TrainDeclareVO(
    Long id,
    Long trainId,
    String title,
    Long cityId,
    String cityName,
    Long userId,
    Integer status,
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
