package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 培训需求视图对象
 */
public record TrainNeedVO(
    /** 主键ID */
    Long id,
    /** 组织ID */
    Long orgId,
    /** 组织名称 */
    String orgName,
    /** 培训对象 */
    String trainObject,
    /** 岗位 */
    String position,
    /** 人数 */
    Integer num,
    /** 工种 */
    String workType,
    /** 等级 */
    String level,
    /** 培训目标 */
    String target,
    /** 培训方式 */
    String method,
    /** 培训地点 */
    String address,
    /** 培训日期 */
    String trainDate,
    /** 预算 */
    String budget,
    /** 师资要求 */
    String teacherRequire,
    /** 是否需要考试 0-否 1-是 */
    Integer needExam,
    /** 是否需要匹配 0-否 1-是 */
    Integer needMatch,
    /** 是否需要试卷 0-否 1-是 */
    Integer needPaper,
    /** 用户ID */
    Long userId,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 扩展JSON数据 */
    String jsonData
) {}
