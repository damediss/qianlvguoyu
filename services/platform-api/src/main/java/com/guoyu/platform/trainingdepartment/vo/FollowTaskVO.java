package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 跟班作业任务 VO */
public record FollowTaskVO(
    String id,
    Long classId,
    Long trainInfoId,
    Long scheduleId,
    String bizDate,
    String taskType,
    String taskTypeName,
    String title,
    String description,
    LocalDateTime deadlineAt,
    String status,
    String statusName,
    BigDecimal passScore,
    String passRule,
    Boolean showAnalysisAfterSubmit,
    LocalDateTime publishedAt,
    LocalDateTime createdAt,
    Integer questionCount,
    Integer totalCount,
    Integer submittedCount,
    Integer gradedCount,
    BigDecimal completionRate,
    List<FollowTaskQuestionVO> questions
) {}
