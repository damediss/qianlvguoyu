package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SubsidyMaterialTemplateVO(
    Long id,
    String scope,
    Long tenantId,
    Long classId,
    String materialType,
    String materialTypeLabel,
    String slotCode,
    String regionCode,
    String trainMode,
    Integer policyYear,
    String title,
    String fileFormat,
    Long templateFileId,
    String templateFilePath,
    List<String> fieldKeys,
    Integer parseVersion,
    String status,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
