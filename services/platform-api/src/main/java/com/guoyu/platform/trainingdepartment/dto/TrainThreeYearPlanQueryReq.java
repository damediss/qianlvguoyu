package com.guoyu.platform.trainingdepartment.dto;

public record TrainThreeYearPlanQueryReq(
    Integer page,
    Integer pageSize,
    String title,
    Long targetOrgId,
    Integer status
) {
  public TrainThreeYearPlanQueryReq {
    if (page == null || page < 1) page = 1;
    if (pageSize == null || pageSize < 1) pageSize = 10;
  }
}
