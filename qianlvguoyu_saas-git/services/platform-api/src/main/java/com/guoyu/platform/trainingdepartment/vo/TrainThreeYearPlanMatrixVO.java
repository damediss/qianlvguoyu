package com.guoyu.platform.trainingdepartment.vo;

public record TrainThreeYearPlanMatrixVO(
    Long id,
    String jobName,
    Integer planYear,
    String trainMode,
    String levelName,
    Integer headcount
) {}
