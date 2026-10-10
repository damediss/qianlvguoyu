package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;

public record FollowStudentClassHoursVO(
    @JsonSerialize(using = ToStringSerializer.class) Long peopleId,
    String name,
    BigDecimal attendedHours,
    BigDecimal plannedHours,
    BigDecimal completionRate,
    Integer presentSessions,
    Integer absentSessions,
    String hoursStatus) {}
