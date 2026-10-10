package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record FollowIssueVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long classId,
    @JsonSerialize(using = ToStringSerializer.class) Long supervisionId,
    @JsonSerialize(using = ToStringSerializer.class) Long itemId,
    String title,
    String description,
    String severity,
    String status,
    String assigneeName,
    String dueDate,
    String resolveNote,
    @JsonSerialize(using = ToStringSerializer.class) Long openedBy,
    LocalDateTime openedAt,
    LocalDateTime closedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
