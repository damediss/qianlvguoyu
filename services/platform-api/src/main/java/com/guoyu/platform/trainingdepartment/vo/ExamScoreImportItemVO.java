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
    boolean theoryFallback,
    String status,
    List<ExamScoreImportTargetVO> targets) {}
