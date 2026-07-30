package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 培训申报视图对象
 */
public record TrainDeclareVO(
    /** 主键ID */
    Long id,
    /** 培训ID */
    Long trainId,
    /** 标题 */
    String title,
    Long cityId,
    String cityName,
    /** 用户ID */
    Long userId,
    /** 状态 */
    Integer status,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime
) {}
