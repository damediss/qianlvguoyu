package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 培训档案视图对象
 */
public record TrainArchiveVO(
    /** 主键ID */
    Long id,
    /** 项目ID */
    Long projectId,
    String archiveType,
    Integer fileCount,
    String archiveTime,
    String filePaths,
    /** 状态 */
    Integer status,
    Long approverId,
    String approverName,
    /** 审批时间 */
    String approveTime,
    String rejectReason,
    /** 备注 */
    String remark,
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
