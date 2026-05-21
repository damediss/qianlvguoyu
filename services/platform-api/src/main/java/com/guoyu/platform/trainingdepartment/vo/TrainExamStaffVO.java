package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_exam_staff 视图对象 */
public record TrainExamStaffVO(
    Long id,
    /** 考试计划ID */
    Long examId,
    /** 考场ID */
    Long examRoomId,
    /** 教室ID */
    Long roomId,
    /** 用户ID */
    Long userId,
    /** 姓名 */
    String realname,
    /** 人员类型 */
    String type,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
