package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record FollowSupervisionItemVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long supervisionId,
    String itemCode,
    String itemTitle,
    Integer sortNo,
    String result,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
