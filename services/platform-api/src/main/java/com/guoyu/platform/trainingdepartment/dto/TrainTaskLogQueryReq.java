package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 培训任务记录查询请求 */
public class TrainTaskLogQueryReq extends PageQuery {

  /** 培训班级ID */
  private Long trainClassId;

  public Long getTrainClassId() {
    return trainClassId;
  }

  public void setTrainClassId(Long trainClassId) {
    this.trainClassId = trainClassId;
  }
}
