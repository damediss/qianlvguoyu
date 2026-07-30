package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 培训方案列表项（含费用汇总和培训需求标题） */
public record TrainSolutionListVO(
    Long id,
    String title,
    Long companyId,
    String companyName,
    String trainTitle,
    BigDecimal badge,
    BigDecimal expend,
    LocalDateTime createTime,
    String rejectReason
) {}
