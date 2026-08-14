package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record FollowEvidenceVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long classId,
    @JsonSerialize(using = ToStringSerializer.class) Long scheduleId,
    String bizDate,
    String mediaType,
    Integer durationSeconds,
    @JsonSerialize(using = ToStringSerializer.class) Long fileId,
    String filePath,
    String url,
    String caption,
    @JsonSerialize(using = ToStringSerializer.class) Long uploaderUserId,
    String uploaderRole,
    String reviewStatus,
    @JsonSerialize(using = ToStringSerializer.class) Long reviewedBy,
    LocalDateTime reviewedAt,
    String rejectReason,
    Integer sortNo,
    String scheduleDate,
    String courseName,
    String teachForm,
    String classHours,
    String actualClassHours,
    String startTime,
    String endTime,
    LocalDateTime createdAt
) {}
