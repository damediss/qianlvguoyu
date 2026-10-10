package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;

/** 单题正确率统计 */
public record FollowTaskQuestionStatVO(
    String questionId,
    Integer sortNumber,
    String questionType,
    String stemPreview,
    Integer answeredCount,
    Integer correctCount,
    BigDecimal accuracyRate
) {}
