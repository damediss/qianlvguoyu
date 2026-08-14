package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record SubsidyEventVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long subsidyId,
    String action,
    String fromPhase,
    String toPhase,
    String remark,
    @JsonSerialize(using = ToStringSerializer.class) Long operatorId,
    LocalDateTime createdAt
) {}
