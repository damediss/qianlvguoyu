package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_exam_session 视图对象 */
public record TrainExamSessionVO(
    Long id,
    /** 考试计划ID */
    Long examId,
    /** 课程ID */
    Long courseId,
    /** 用户ID */
    Long userId,
    /** 开始时间 */
    String startTime,
    /** 结束时间 */
    String endTime,
    /** 课程名称 */
    String courseName,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
