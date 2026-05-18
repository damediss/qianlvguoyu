package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

public record TrainRequestVO(
    Long id,
    String companyName,
    Long companyId,
    String position,
    Integer num,
    String type,
    String trainDate,
    String contact,
    String contactPhone,
    Integer isOnPosition,
    String studentForm,
    Long userId,
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
