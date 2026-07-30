package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_sign_up_self 视图对象 */
public record TrainSignUpSelfVO(
    Long id,
    /** 标题 */
    String title,
    /** 封面图 */
    String cover,
    /** 培训时间 */
    String trainTime,
    /** 考试时间 */
    String examTime,
    /** 组织ID */
    Long orgId,
    /** 培训信息ID */
    Long trainInfoId,
    /** 班级ID（class_prep） */
    Long trainClassId,
    /** 采集来源 */
    String collectionSource,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
