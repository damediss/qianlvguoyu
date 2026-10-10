package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 培训项目表单视图对象
 */
public record TrainProjectTableVO(
    /** 主键ID */
    Long id,
    /** 项目ID */
    Long projectId,
    Integer tableType,
    String tableName,
    /** 内容 */
    String content,
    Integer version,
    /** 状态 */
    Integer status,
    Long fileId,
    Long ownerId,
    String ownerName,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    /** 用户ID */
    Long userId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
