package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 文件列表更新请求 */
public class TrainFileListUpdateReq {

  @NotBlank
  @Size(max = 255)
  /** 标题 */
  private String title;

  /** 状态 */
  private Integer status;

  /** 排序号 */
  private Integer sortNumber;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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
