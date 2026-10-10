package com.guoyu.platform.trainingdepartment.vo;

public record TrainThreeYearPlanItemVO(
    Long id,
    Long planId,
    String personName,
    String idCard,
    String companyName,
    String jobName,
    String levelName,
    String trainMode,
    Integer planYear,
    Integer trainYear,
    Integer certYear,
    String eligibleDate,
    Integer rowIndex,
    String source
) {}
