package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.util.List;

/** 交卷/结果 VO */
public record FollowTaskResultVO(
    String taskId,
    String itemId,
    String status,
    BigDecimal score,
    Boolean passed,
    Boolean showAnalysis,
    Integer correctCount,
    Integer questionCount,
    BigDecimal accuracyRate,
    Integer durationSeconds,
    List<FollowTaskAnswerResultVO> answers
) {}
