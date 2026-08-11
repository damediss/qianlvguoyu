package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 学员任务态 VO */
public record FollowTaskItemVO(
    String id,
    String taskId,
    Long peopleId,
    String userName,
    String status,
    String statusName,
    BigDecimal score,
    Boolean passed,
    LocalDateTime submittedAt,
    LocalDateTime gradedAt,
    String comment,
    Integer correctCount,
    Integer questionCount,
    BigDecimal accuracyRate,
    LocalDateTime startedAt,
    Integer durationSeconds
) {}
