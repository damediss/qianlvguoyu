package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;

/** 考试页：班级人员成绩花名册行（对齐人社：理论/技能/综合/工作业绩） */
public record ExamScoreImportClassPeopleVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    String realname,
    String idCard,
    String ticketNo,
    String phone,
    BigDecimal theoryScore,
    BigDecimal skillScore,
    BigDecimal examScore,
    BigDecimal performanceScore,
    /** PASS / FAIL / ABSENT */
    String examResultStatus) {}
