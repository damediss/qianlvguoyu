package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/**
 * 课程课件查询请求
 */
public class CourseKejianQueryReq extends PageQuery {

  /** 关键字（课件名称或课件编号） */
  private String keyword;

  /** 工种id */
  private Long workTypeId;

  /** 获取方式:0免费,1收费 */
  private Integer type;

  /** 0平台,1企业 */
  private Integer tag;

  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public Integer getTag() { return tag; }
  public void setTag(Integer tag) { this.tag = tag; }
}
