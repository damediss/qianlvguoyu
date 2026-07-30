package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 学习记录视图对象 */
public record TrainUserLearnLogVO(
    Long id,
    /** 培训ID */
    Long trainId,
    /** 比赛ID */
    Long matchId,
    /** 培训任务ID */
    Long trainTaskId,
    /** 学习时长 */
    Integer learnTime,
    /** 得分 */
    Integer score,
    /** 用户ID */
    Long userId,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
