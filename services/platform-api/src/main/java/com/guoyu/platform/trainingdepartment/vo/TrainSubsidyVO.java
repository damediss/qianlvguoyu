package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrainSubsidyVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long projectId,
    @JsonSerialize(using = ToStringSerializer.class) Long classId,
    @JsonSerialize(using = ToStringSerializer.class) Long trainInfoId,
    String className,
    String policyTemplate,
    BigDecimal subsidyAmount,
    Integer studentCount,
    String materialPackagePath,
    Integer materialStatus,
    Integer rssbStatus,
    String rssbSubmitTime,
    String rssbReceiptNo,
    String rssbRejectReason,
    BigDecimal grantedAmount,
    String grantTime,
    String remark,
    Integer status,
    String phase,
    String regionCode,
    String regionName,
    String trainMode,
    Integer policyYear,
    @JsonSerialize(using = ToStringSerializer.class) Long profileId,
    String profileVersion,
    Integer profileLocked,
    String checklistJson,
    String evidenceIndexPath,
    Integer sortNumber,
    @JsonSerialize(using = ToStringSerializer.class) Long tenantId,
    @JsonSerialize(using = ToStringSerializer.class) Long userId,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {}
