package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;

public record ExamScoreImportTargetVO(
    Long peopleId,
    Long classId,
    String classTitle,
    String rosterName,
    BigDecimal oldExamScore,
    BigDecimal oldRealScore,
    boolean nameMismatch,
    boolean jobMismatch) {}
