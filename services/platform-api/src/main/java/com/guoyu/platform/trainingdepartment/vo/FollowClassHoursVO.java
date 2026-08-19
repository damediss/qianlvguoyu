package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.util.List;

public record FollowClassHoursVO(
    BigDecimal plannedHours,
    BigDecimal plannedHoursFromExcel,
    Integer minuteUnit,
    Integer unconvertibleSessions,
    Integer recordedStudentCount,
    Integer unrecordedStudentCount,
    BigDecimal attendedHoursSum,
    BigDecimal attendedHoursAvg,
    BigDecimal hoursCompletionAvg,
    List<FollowStudentClassHoursVO> students) {}
