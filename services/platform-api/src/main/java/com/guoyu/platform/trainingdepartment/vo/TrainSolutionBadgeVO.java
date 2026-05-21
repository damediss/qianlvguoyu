package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** train_solution_badge 视图对象 */
public record TrainSolutionBadgeVO(
    Long id,
    /** 方案ID */
    Long solutionId,
    /** 费用项目名称 */
    String title,
    /** 费用金额 */
    BigDecimal badge,
    /** 支出金额 */
    BigDecimal expend,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
