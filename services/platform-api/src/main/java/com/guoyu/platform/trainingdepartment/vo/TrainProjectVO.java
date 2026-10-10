package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 培训项目视图对象
 */
public record TrainProjectVO(
    /** 主键ID */
    Long id,
    String projectName,
    Long companyId,
    String companyName,
    Long managerId,
    String managerName,
    String planStart,
    String planEnd,
    String currentStage,
    /** 进度 */
    Integer progress,
    /** 状态 */
    Integer status,
    /** 描述 */
    String description,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    Long createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
