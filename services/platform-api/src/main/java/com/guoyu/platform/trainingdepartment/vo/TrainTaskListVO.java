package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 知识点任务列表视图对象 */
public record TrainTaskListVO(
    Long id,
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
    Integer score,
    /** 获得积分 */
    BigDecimal getPoint,
    /** 审核状态 */
    Integer checkStatus,
    /** 终审状态 */
    Integer finalCheckStatus,
    /** 审核时间 */
    LocalDateTime checkTime,
    /** 审核人ID */
    Long checkUserId,
    /** 审核人名称 */
    String checkUsername,
    /** 终审时间 */
    LocalDateTime finalCheckTime,
    /** 终审人ID */
    Long finalCheckUserId,
    /** 终审人名称 */
    String finalCheckUsername
) {}
