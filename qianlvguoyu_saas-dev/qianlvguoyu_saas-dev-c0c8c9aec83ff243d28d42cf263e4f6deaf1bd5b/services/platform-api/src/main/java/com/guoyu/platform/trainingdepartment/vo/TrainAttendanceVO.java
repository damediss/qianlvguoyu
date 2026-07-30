package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 培训考勤视图对象
 */
public record TrainAttendanceVO(
    /** 主键ID */
    Long id,
    /** 项目ID */
    Long projectId,
    /** 班级ID */
    Long classId,
    /** 用户ID */
    Long userId,
    String userName,
    String attendanceDate,
    /** 状态 */
    Integer status,
    String checkInTime,
    String checkOutTime,
    String photoPath,
    String videoPath,
    /** 备注 */
    String remark,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    Long createdBy,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
