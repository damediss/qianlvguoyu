package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 任务详情查询请求 */
public class TrainTaskInfoDetailQueryReq extends PageQuery {

  private Long trainId;
  /** 任务列表ID */
  private Long taskListId;

  public Long getTrainId() {
    return trainId;
  }

  public void setTrainId(Long trainId) {
    this.trainId = trainId;
  }

  public Long getTaskListId() {
    return taskListId;
  }

  public void setTaskListId(Long taskListId) {
    this.taskListId = taskListId;
  }
}
