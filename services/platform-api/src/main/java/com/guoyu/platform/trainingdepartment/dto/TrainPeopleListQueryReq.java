package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 开班人员列表查询请求 */
public class TrainPeopleListQueryReq extends PageQuery {

  private Long trainId;
  private Long trainClassId;
  /** 类型 */
  private String type;
  private String keyword;

  public Long getTrainId() {
    return trainId;
  }

  public void setTrainId(Long trainId) {
    this.trainId = trainId;
  }

  public Long getTrainClassId() {
    return trainClassId;
  }

  public void setTrainClassId(Long trainClassId) {
    this.trainClassId = trainClassId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }
}
