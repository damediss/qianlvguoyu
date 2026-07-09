package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

public record TrainThreeYearPlanVO(
    Long id,
    String planNo,
    String title,
    Long targetOrgId,
    String targetOrgName,
    Long researchOrgId,
    String researchOrgTitle,
    Long rencaiTaskId,
    Long rencaiInputFileId,
    String planYears,
    Integer status,
    String statusLabel,
    String analysisStatus,
    String rejectReason,
    LocalDateTime companyConfirmTime,
    String companyConfirmUsername,
    Long createdBy,
    LocalDateTime createTime,
    LocalDateTime updateTime,
    Integer totalRows,
    Integer recommendedRows,
    Double progressPct,
    String rencaiTaskStatus
) {}
