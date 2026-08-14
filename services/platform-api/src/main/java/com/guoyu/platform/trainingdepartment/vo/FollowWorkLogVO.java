package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record FollowWorkLogVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long classId,
    @JsonSerialize(using = ToStringSerializer.class) Long scheduleId,
    String bizDate,
    @JsonSerialize(using = ToStringSerializer.class) Long authorUserId,
    String authorName,
    String attendanceSummary,
    String exceptionNote,
    String content,
    String sourceOpIdsJson,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
