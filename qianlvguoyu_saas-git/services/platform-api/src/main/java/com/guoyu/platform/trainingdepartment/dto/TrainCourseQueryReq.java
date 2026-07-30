package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 课时列表查询请求 */
public class TrainCourseQueryReq extends PageQuery {

  /** 培训ID */
  private Long trainId;
  /** 类型 */
  private Integer type;
  /** 搜索关键词 */
  private String keyword;

  public Long getTrainId() {
    return trainId;
  }

  public void setTrainId(Long trainId) {
    this.trainId = trainId;
  }

  public Integer getType() {
    return type;
  }

  public void setType(Integer type) {
    this.type = type;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }
}
