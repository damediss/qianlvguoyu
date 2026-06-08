package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 信息收集页报名计划列表项 */
public record TrainSignUpSelfListVO(
    Long id,
    String title,
    String cover,
    String trainTime,
    String examTime,
    Long orgId,
    String orgName,
    Long trainInfoId,
    Integer sortNumber,
    Long peopleCount,
    String wxMiniQrcode,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {}
