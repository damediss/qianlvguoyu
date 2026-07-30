package com.guoyu.platform.trainingdepartment.dto;

public record TrainThreeYearPlanItemQueryReq(
    Integer page,
    Integer pageSize,
    Integer planYear,
    String jobName
) {
  public TrainThreeYearPlanItemQueryReq {
    if (page == null || page < 1) page = 1;
    if (pageSize == null || pageSize < 1) pageSize = 50;
  }
}
