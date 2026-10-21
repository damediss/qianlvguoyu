package com.guoyu.platform.textbook.dto;

import com.guoyu.platform.common.api.PageQuery;

/**
 * 教材工程分页查询（GET 查询参数绑定）。
 */
public class ProjectQueryReq extends PageQuery {

  private String workTypeTitle;
  private String direction;
  /** 5=五级 … 1=一级 */
  private Integer levelCode;
  /** parsed/outlined/reviewed */
  private String status;

  public String getWorkTypeTitle() {
    return workTypeTitle;
  }

  public void setWorkTypeTitle(String workTypeTitle) {
    this.workTypeTitle = workTypeTitle;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public Integer getLevelCode() {
    return levelCode;
  }

  public void setLevelCode(Integer levelCode) {
    this.levelCode = levelCode;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
