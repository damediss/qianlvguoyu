package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record ExamScoreImportClassVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    String title,
    @JsonSerialize(using = ToStringSerializer.class) Long companyId,
    String companyName,
    @JsonSerialize(using = ToStringSerializer.class) Long trainInfoId,
    String trainTitle,
    @JsonSerialize(using = ToStringSerializer.class) Long workTypeId,
    String workTypeTitle,
    Integer workTypeLevel,
    /** 本班学员人数 */
    int studentCount,
    /** 至少有一项人社成绩（理论/技能/综合/业绩）的人数 */
    int scoredCount,
    /** NONE / PARTIAL / DONE */
    String scoreImportStatus,
    /** 理论≥60 且 技能≥60 */
    int passedCount,
    /** 理论与技能均无成绩 */
    int absentCount,
    /** 有成绩但未双科达标 */
    int failedCount) {}
