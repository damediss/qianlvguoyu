package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 培训配置任务列表视图对象 */
public record TrainTaskInfoVO(
    Long id,
    /** 培训ID */
    Long trainId,
    /** 封面 */
    String cover,
    /** 课时ID */
    Long courseId,
    /** 培训任务ID */
    Long trainTaskId,
    /** 知识点ID */
    Long knowledgeId,
    /** 简介 */
    String intro,
    /** 是否必修 */
    Integer isRequired,
    /** 得分 */
    Integer score,
    /** 用户ID */
    Long userId,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
