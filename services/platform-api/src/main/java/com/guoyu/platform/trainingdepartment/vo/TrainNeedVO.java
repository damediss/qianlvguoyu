package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

public record TrainNeedVO(
    Long id,
    Long orgId,
    String orgName,
    String trainObject,
    String position,
    Integer num,
    String workType,
    String level,
    String target,
    String method,
    String address,
    String trainDate,
    String budget,
    String teacherRequire,
    Integer needExam,
    Integer needMatch,
    Integer needPaper,
    Long userId,
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime,
    String jsonData
) {}
