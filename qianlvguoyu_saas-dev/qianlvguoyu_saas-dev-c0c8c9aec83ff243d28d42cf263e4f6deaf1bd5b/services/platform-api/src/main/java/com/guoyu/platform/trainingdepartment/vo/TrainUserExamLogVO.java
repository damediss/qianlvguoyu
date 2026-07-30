package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 用户刷题记录视图对象 */
public record TrainUserExamLogVO(
    Long id,
    /** 培训ID */
    Long trainId,
    /** 知识点ID */
    Long knowledgeId,
    /** 用户ID */
    Long userId,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
