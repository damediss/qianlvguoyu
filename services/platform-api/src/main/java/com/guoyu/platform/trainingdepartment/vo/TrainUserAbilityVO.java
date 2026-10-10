package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 个人技能矩阵视图对象 */
public record TrainUserAbilityVO(
    Long id,
    /** 培训ID */
    Long trainId,
    /** 课时ID */
    Long courseId,
    /** 知识点ID */
    Long knowledgeId,
    /** 评估结果 */
    String result,
    /** 配置ID */
    Long configId,
    /** 得分 */
    Integer score,
    /** 当前得分 */
    Integer currentScore,
    /** 用户ID */
    Long userId,
    /** 平台用户ID */
    Long platformUserId,
    /** 评语 */
    String comments,
    /** 状态 */
    Integer status,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
