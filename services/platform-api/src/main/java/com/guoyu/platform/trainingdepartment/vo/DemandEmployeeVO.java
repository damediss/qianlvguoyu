package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** demand_employee 视图对象 */
public record DemandEmployeeVO(
    Long id,
    /** 企业ID */
    Long orgId,
    /** 工种/岗位 */
    String work,
    /** 学历要求 */
    String education,
    /** 专业要求 */
    String major,
    /** 添加时间戳 */
    Integer addtime,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
