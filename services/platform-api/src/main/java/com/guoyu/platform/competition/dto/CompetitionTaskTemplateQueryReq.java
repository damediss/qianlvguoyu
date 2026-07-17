package com.guoyu.platform.competition.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionTaskTemplateQueryReq extends PageQuery {

  private String taskType;
  private String status;

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
