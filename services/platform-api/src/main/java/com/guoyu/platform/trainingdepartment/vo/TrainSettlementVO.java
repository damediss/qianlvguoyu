package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训结算视图对象
 */
public record TrainSettlementVO(
    /** 主键ID */
    Long id,
    /** 项目ID */
    Long projectId,
    BigDecimal settlementAmount,
    BigDecimal institutionShare,
    BigDecimal enterpriseShare,
    BigDecimal thirdPartyShare,
    String shareRatio,
    Integer approvalStatus,
    Long approverId,
    String approverName,
    /** 审批时间 */
    String approveTime,
    String rejectReason,
    /** 备注 */
    String remark,
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
