package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;

public record ExamScoreImportBatchItemVO(
    Long id,
    Integer rowNo,
    String idCard,
    String name,
    String ticketNo,
    BigDecimal theoryScore,
    BigDecimal skillScore,
    BigDecimal comprehensiveScore,
    Long peopleId,
    Long classId,
    String resultCode,
    BigDecimal oldExamScore,
    BigDecimal newExamScore,
    BigDecimal oldRealScore,
    BigDecimal newRealScore,
    boolean theoryFallback) {}
