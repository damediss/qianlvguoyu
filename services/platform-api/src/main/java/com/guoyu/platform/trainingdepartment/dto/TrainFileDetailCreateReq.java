package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 文件详情创建请求 */
public class TrainFileDetailCreateReq {

  /** 文件ID */
  private Long fileId;

  @Size(max = 255)
  private String title;

  @Size(max = 500)
  private String path;

  private Long userId;

  /** 状态 */
  private Integer status;

  private Integer sortNumber;

  public Long getFileId() {
    return fileId;
  }

  public void setFileId(Long fileId) {
    this.fileId = fileId;
  }

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

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
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
