package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 用户课程计划视图对象 */
public record TrainUserCoursePlanVO(
    Long id,
    /** 用户ID */
    Long userId,
    /** 平台用户ID */
    Long platformUserId,
    /** 真实姓名 */
    String realname,
    /** 添加来源 */
    Integer addFrom,
    /** 课时ID */
    Long courseId,
    /** 日期 */
    String date,
    /** 类型 */
    Integer type,
    /** 状态 */
    Integer status,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
