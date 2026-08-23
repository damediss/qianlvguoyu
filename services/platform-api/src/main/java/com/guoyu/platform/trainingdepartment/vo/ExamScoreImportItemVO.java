package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.util.List;

public record ExamScoreImportItemVO(
    int rowNo,
    String idCard,
    String name,
    String ticketNo,
    String jobName,
    String levelName,
    BigDecimal theoryScore,
    BigDecimal skillScore,
    BigDecimal examScore,
    BigDecimal performanceScore,
    boolean theoryFallback,
    String status,
    List<ExamScoreImportTargetVO> targets,
    /** 多场合并说明，如「合并2场（补考继承实操）」 */
    String mergeNote,
    boolean examTimeMissing) {}
