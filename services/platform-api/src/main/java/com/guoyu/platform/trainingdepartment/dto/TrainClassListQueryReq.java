package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 分班查询请求 */
public class TrainClassListQueryReq extends PageQuery {

  /** 培训ID */
  private Long trainId;
  /** 搜索关键词 */
  private String keyword;

  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
