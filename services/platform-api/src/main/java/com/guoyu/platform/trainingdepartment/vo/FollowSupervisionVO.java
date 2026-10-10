package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;

public record FollowSupervisionVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long classId,
    String bizDate,
    @JsonSerialize(using = ToStringSerializer.class) Long scheduleId,
    @JsonSerialize(using = ToStringSerializer.class) Long inspectorUserId,
    String inspectorName,
    String status,
    String conclusion,
    String remark,
    LocalDateTime submittedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<FollowSupervisionItemVO> items,
    Long openIssueCount
) {}
