package com.guoyu.platform.trainingdepartment.dto;

public record TrainThreeYearPlanItemUpdateReq(
    String personName,
    String jobName,
    String levelName,
    String eligibleDate,
    Integer planYear
) {}
