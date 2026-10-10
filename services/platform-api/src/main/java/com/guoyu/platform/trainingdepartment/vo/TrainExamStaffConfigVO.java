package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_exam_staff_config 视图对象 */
public record TrainExamStaffConfigVO(
    Long id,
    /** 人员类型 */
    String type,
    /** 姓名 */
    String realname,
    /** 用户ID */
    Long userId,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
