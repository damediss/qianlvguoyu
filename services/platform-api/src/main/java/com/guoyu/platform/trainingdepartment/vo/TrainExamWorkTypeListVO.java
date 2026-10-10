package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_exam_work_type_list 视图对象 */
public record TrainExamWorkTypeListVO(
    Long id,
    /** 考试计划ID */
    Long examId,
    /** 工种ID */
    Long workTypeId,
    /** 课程ID */
    Long courseId,
    /** 等级 */
    String level,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
