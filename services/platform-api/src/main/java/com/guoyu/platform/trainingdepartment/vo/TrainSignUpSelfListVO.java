package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;
import java.util.List;

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
    List<TrainSignUpSelfFileVO> files,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {}
