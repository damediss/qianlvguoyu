package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_exam_shijuan 视图对象 */
public record TrainExamShijuanVO(
    Long id,
    /** 考试计划ID */
    Long examId,
    /** 试卷ID */
    Long paperId,
    /** 考场ID */
    Long examRoomId,
    /** 实操试卷ID */
    Long operatePaperId,
    /** 教室ID */
    Long roomId,
    /** 用户ID */
    Long userId,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
