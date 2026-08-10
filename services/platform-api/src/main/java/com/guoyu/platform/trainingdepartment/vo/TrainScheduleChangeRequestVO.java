package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 调课申请 VO */
public record TrainScheduleChangeRequestVO(
    String id,
    Long classId,
    Long scheduleId,
    String changeType,
    String changeTypeName,
    String beforeSnapshot,
    String afterPayload,
    String reason,
    String status,
    String statusName,
    Long applicantPlatformUserId,
    String applicantRoleCode,
    Long reviewerUserId,
    LocalDateTime reviewedAt,
    String rejectReason,
    LocalDateTime createdAt
) {
}
