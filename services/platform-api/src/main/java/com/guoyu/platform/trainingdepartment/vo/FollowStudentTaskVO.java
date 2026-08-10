package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;

/** 学员视角任务（待办/已做） */
public record FollowStudentTaskVO(
    String id,
    Long classId,
    String taskType,
    String taskTypeName,
    String title,
    String status,
    String itemStatus,
    String itemStatusName,
    BigDecimal score,
    Boolean passed,
    Boolean showAnalysisAfterSubmit,
    String deadlineAt,
    String publishedAt
) {}
