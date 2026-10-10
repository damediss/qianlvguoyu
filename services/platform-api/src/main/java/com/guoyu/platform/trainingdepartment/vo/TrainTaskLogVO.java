package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_task_log 视图对象 */
public record TrainTaskLogVO(
    Long id,
    /** 班级ID */
    Long trainClassId,
    /** 任务检查点ID */
    Long ckId,
    /** 日期 */
    String date,
    /** 添加时间戳 */
    Integer addtime,
    /** 状态 */
    Integer status,
    /** 时间范围 */
    String times,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
