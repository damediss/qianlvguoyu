package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

public record TrainTalentNeedsVO(
    Long id,
    String title,
    Long orgId,
    String orgName,
    String job,
    String workType,
    String major,
    String edu,
    Integer gender,
    Integer num,
    String address,
    String workYear,
    String inPositionDate,
    String paperRequire,
    Long userId,
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime,
    String jsonData,
    String duty,
    String schoolNeed
) {}
