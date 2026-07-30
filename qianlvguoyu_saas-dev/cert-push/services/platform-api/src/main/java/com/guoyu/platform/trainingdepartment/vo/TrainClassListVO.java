package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 分班视图对象 */
public record TrainClassListVO(
    Long id,
    /** 培训ID */
    Long trainId,
    /** 用户ID */
    Long userId,
    /** 是否组长 */
    Integer isLeader,
    /** 是否班委 */
    Integer isCommittee,
    /** 班级名称 */
    String className,
    /** 排序号 */
    Integer sortNumber,
    /** 状态 0-未开班 1-开班 2-结课 */
    Integer status,
    Long tenantId,
    LocalDateTime createTime
) {}
