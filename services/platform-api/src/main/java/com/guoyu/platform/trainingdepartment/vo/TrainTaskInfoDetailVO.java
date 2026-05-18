package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 任务详情视图对象 */
public record TrainTaskInfoDetailVO(
    Long id,
    /** 培训ID */
    Long trainId,
    /** 任务列表ID */
    Long taskListId,
    /** 工种ID */
    Long workTypeId,
    /** 课时ID */
    Long courseId,
    /** 知识点ID */
    Long knowledgeId,
    /** 标题 */
    String title,
    /** 类型 */
    String type,
    /** 内容 */
    String content,
    /** 学时 */
    BigDecimal getLearnTime,
    /** 最小时长 */
    Integer minMinute,
    /** 是否必修 */
    Integer isRequired,
    /** 用户ID */
    Long userId,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime,
    /** 得分 */
    Integer score
) {}
