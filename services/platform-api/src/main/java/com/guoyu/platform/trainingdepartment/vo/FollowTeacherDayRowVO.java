package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;

public record FollowTeacherDayRowVO(
    String teacherName,
    String teachForm,
    Integer taughtSessionCount,
    BigDecimal taughtDays,
    Integer plannedSessionCount,
    BigDecimal plannedDays,
    Integer overflowSessionCount,
    BigDecimal ratePerDay,
    BigDecimal payTotal,
    String remark) {}
