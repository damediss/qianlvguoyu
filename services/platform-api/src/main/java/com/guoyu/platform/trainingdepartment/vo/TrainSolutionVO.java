package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

public record TrainSolutionVO(
    Long id,
    Long trainInfoId,
    Long companyId,
    String companyName,
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
    Integer status,
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime,
    Long userId,
    String content,
    String filePath,
    String rejectReason
) {}
