package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.util.List;

public record FollowTeacherDaysVO(
    Long classId,
    String classTitle,
    BigDecimal sessionUnitDays,
    Integer maxSessionsPerDay,
    String asOfDate,
    Integer assignedSessionCount,
    Integer unassignedSessionCount,
    Integer overflowSessionCount,
    BigDecimal taughtDaysSum,
    BigDecimal plannedDaysSum,
    BigDecimal payTotalSum,
    List<FollowTeacherDayRowVO> teachers) {}
