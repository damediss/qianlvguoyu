package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 培训协议视图对象
 */
public record TrainProtocolVO(
    /** 主键ID */
    Long id,
    /** 项目ID */
    Long projectId,
    /** 标题 */
    String title,
    /** 类型 */
    String type,
    /** 内容 */
    String content,
    Integer version,
    /** 状态 */
    Integer status,
    String signedAt,
    Long signerId,
    Long approverId,
    String rejectReason,
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
