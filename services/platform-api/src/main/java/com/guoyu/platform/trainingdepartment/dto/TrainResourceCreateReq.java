package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 资源创建请求 */
public class TrainResourceCreateReq {

  /** 工种ID */
  private Long workTypeId;

  @Size(max = 255)
  private String title;

  /** 类型 */
  @Size(max = 20)
  private String type;

  @Size(max = 500)
  private String path;

  private Long userId;

  private Integer sortNumber;

  public Long getWorkTypeId() {
    return workTypeId;
  }

  public void setWorkTypeId(Long workTypeId) {
    this.workTypeId = workTypeId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
