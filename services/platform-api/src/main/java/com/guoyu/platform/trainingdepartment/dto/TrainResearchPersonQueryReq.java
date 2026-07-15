package com.guoyu.platform.trainingdepartment.dto;

public record TrainResearchPersonQueryReq(
    Integer page,
    Integer pageSize,
    String keyword
) {
  public TrainResearchPersonQueryReq {
    if (page == null || page < 1) {
      page = 1;
    }
    if (pageSize == null || pageSize < 1) {
      pageSize = 20;
    }
  }
}
