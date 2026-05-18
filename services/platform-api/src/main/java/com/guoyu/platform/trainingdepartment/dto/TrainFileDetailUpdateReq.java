package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 文件详情更新请求 */
public class TrainFileDetailUpdateReq {

  @NotBlank
  @Size(max = 255)
  private String title;

  @Size(max = 500)
  private String path;

  /** 状态 */
  private Integer status;

  private Integer sortNumber;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
