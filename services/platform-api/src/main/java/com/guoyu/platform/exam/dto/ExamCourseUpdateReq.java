package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExamCourseUpdateReq(
    @NotBlank @Size(max = 255) String title,
    Integer workTypeId,
    Integer level,
    @Size(max = 200) String seriesNoGroup,
    @Size(max = 200) String seriesNo,
    @Size(max = 255) String userInChargeName,
    Integer lastLevelGetYear,
    Integer workYearLimit,
    Integer minLevel,
    Integer minLevelYear,
    Integer startAge,
    Integer isGeneral,
    String examCondition,
    @Size(max = 255) String comments,
    Integer status,
    Integer sortNumber
) {
}