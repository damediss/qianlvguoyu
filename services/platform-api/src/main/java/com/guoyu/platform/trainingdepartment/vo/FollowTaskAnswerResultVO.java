package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;

public record FollowTaskAnswerResultVO(
    String questionId,
    Integer sortNumber,
    String questionType,
    String stem,
    String optionsJson,
    String userAnswerJson,
    String correctAnswerJson,
    String analysis,
    Boolean correct,
    BigDecimal score,
    BigDecimal fullScore
) {}
