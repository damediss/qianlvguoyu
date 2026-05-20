package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训补贴视图对象
 */
public record TrainSubsidyVO(
    /** 主键ID */
    Long id,
    /** 项目ID */
    Long projectId,
    String policyTemplate,
    /** 补贴金额 */
    BigDecimal subsidyAmount,
    Integer studentCount,
    String materialPackagePath,
    Integer materialStatus,
    Integer rssbStatus,
    String rssbSubmitTime,
    String rssbReceiptNo,
    String rssbRejectReason,
    BigDecimal grantedAmount,
    String grantTime,
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
