package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训总结视图对象
 */
public record TrainSummaryVO(
    /** 主键ID */
    Long id,
    /** 项目ID */
    Long projectId,
    BigDecimal certificationRate,
    BigDecimal subsidySuccessRate,
    String achievements,
    String lessonsLearned,
    String improvementSuggestions,
    /** 内容 */
    String content,
    String filePath,
    Integer approvalStatus,
    Long approverId,
    String approverName,
    /** 审批时间 */
    String approveTime,
    String rejectReason,
    /** 状态 */
    Integer status,
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
