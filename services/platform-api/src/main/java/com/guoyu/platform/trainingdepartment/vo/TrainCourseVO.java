package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 课时列表视图对象 */
public record TrainCourseVO(
    Long id,
    /** 培训ID */
    Long trainId,
    /** 教师身份证 */
    String teacherIdCard,
    /** 教师姓名 */
    String teacherName,
    /** 教师用户ID */
    Long teacherUserId,
    /** 类型 */
    Integer type,
    /** 课时日期 */
    String courseDate,
    /** 标题 */
    String title,
    /** 开始时间 */
    String startTime,
    /** 结束时间 */
    String endTime,
    /** 理论学时 */
    Integer theoryTime,
    /** 操作学时 */
    Integer operationTime,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
