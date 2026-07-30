package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 用户已获得证书视图对象 */
public record TrainUserCoursePaperVO(
    Long id,
    /** 用户ID */
    Long userId,
    /** 平台用户ID */
    Long platformUserId,
    /** 课时ID */
    Long courseId,
    /** 工种ID */
    Long workTypeId,
    /** 获得年份 */
    String getYear,
    /** 状态 */
    Integer status,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
