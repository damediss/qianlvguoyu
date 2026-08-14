package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record FollowOpLogVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long classId,
    @JsonSerialize(using = ToStringSerializer.class) Long scheduleId,
    String bizDate,
    String eventCode,
    String eventCategory,
    String title,
    String summary,
    String detailJson,
    @JsonSerialize(using = ToStringSerializer.class) Long actorUserId,
    String actorName,
    String actorRole,
    String targetType,
    @JsonSerialize(using = ToStringSerializer.class) Long targetId,
    String severityLevel,
    String client,
    String requestId,
    LocalDateTime createdAt
) {}
