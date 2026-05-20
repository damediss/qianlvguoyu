package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 培训申请视图对象
 */
public record TrainRequestVO(
    /** 主键ID */
    Long id,
    String companyName,
    Long companyId,
    /** 岗位 */
    String position,
    /** 人数 */
    Integer num,
    /** 类型 */
    String type,
    /** 培训日期 */
    String trainDate,
    String contact,
    String contactPhone,
    Integer isOnPosition,
    String studentForm,
    /** 用户ID */
    Long userId,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime
) {}
