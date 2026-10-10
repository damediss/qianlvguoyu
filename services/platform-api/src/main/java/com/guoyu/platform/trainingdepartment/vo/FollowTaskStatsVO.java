package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.util.List;

/** 任务维度统计：正确率 / 均分 / 单题正确率 / 平均时长 */
public record FollowTaskStatsVO(
    String taskId,
    Integer questionCount,
    Integer totalCount,
    Integer submittedCount,
    BigDecimal avgScore,
    BigDecimal passRate,
    BigDecimal avgAccuracyRate,
    Integer avgDurationSeconds,
    List<FollowTaskQuestionStatVO> questionStats
) {}
