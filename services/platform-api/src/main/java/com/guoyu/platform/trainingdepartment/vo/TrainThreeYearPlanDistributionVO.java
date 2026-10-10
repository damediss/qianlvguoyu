package com.guoyu.platform.trainingdepartment.vo;

public record TrainThreeYearPlanDistributionVO(
    String jobName,
    String levelName,
    Integer employeeCount,
    Integer thresholdMin,
    Boolean meetsThreshold,
    String warning
) {}
