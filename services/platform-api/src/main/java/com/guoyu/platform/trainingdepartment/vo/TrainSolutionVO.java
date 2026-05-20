package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 培训方案视图对象
 */
public record TrainSolutionVO(
    /** 主键ID */
    Long id,
    Long trainInfoId,
    Long companyId,
    String companyName,
    /** 标题 */
    String title,
    String nationPolicy,
    String industrySituationAnalysis,
    String companyDevelopmentAnalysis,
    String researchDataAnalysis,
    String projectDevelopmentIdeas,
    String mainObjectives,
    String mainInitiatives,
    String developmentPlan,
    String division,
    String risk,
    String communication,
    String checkpoint,
    String results,
    String teacherIntro,
    String requestAnalysis,
    String trainContent,
    String trainMethod,
    String course,
    /** 状态 */
    Integer status,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 用户ID */
    Long userId,
    /** 内容 */
    String content,
    String filePath,
    String rejectReason
) {}
