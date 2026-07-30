package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 人才需求视图对象
 */
public record TrainTalentNeedsVO(
    /** 主键ID */
    Long id,
    /** 标题 */
    String title,
    /** 组织ID */
    Long orgId,
    /** 组织名称 */
    String orgName,
    String job,
    /** 工种 */
    String workType,
    /** 专业 */
    String major,
    String edu,
    /** 性别 */
    Integer gender,
    /** 人数 */
    Integer num,
    /** 培训地点 */
    String address,
    String workYear,
    String inPositionDate,
    String paperRequire,
    /** 用户ID */
    Long userId,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 扩展JSON数据 */
    String jsonData,
    String duty,
    String schoolNeed
) {}
