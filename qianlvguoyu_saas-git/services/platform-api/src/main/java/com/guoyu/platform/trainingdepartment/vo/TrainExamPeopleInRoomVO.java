package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_exam_people_in_room 视图对象 */
public record TrainExamPeopleInRoomVO(
    Long id,
    /** 考试计划ID */
    Long examId,
    /** 课程ID */
    Long courseId,
    /** 场次ID */
    Long sessionId,
    /** 考场ID */
    Long roomId,
    /** 考场关联ID */
    Long examRoomId,
    /** 用户ID */
    Long userId,
    /** 姓名 */
    String realname,
    /** 考生人员ID */
    Long peopleId,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
