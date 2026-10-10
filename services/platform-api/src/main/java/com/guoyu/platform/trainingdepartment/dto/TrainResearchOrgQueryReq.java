package com.guoyu.platform.trainingdepartment.dto;

public record TrainResearchOrgQueryReq(
    Integer page,
    Integer pageSize,
    String title,
    String type,
    Integer status
) {
  public TrainResearchOrgQueryReq {
    if (page == null || page < 1) page = 1;
    if (pageSize == null || pageSize < 1) pageSize = 10;
    if (status != null && status < 0) status = null;
  }
}
