package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_exam_review 视图对象 */
public record TrainExamReviewVO(
    Long id,
    /** 考试计划ID */
    Long examId,
    /** 试卷ID */
    Long paperId,
    /** 用户ID */
    Long userId,
    /** 复习题数 */
    Integer num,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
