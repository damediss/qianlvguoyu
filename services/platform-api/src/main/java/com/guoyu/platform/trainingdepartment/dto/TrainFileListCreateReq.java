package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 文件列表创建请求 */
public class TrainFileListCreateReq {

  /** 关联表名 */
  @Size(max = 255)
  private String tableName;

  /** 关联主键 */
  @Size(max = 255)
  private String pk;

  @Size(max = 255)
  private String title;

  private Long userId;

  /** 状态 */
  private Integer status;

  private Integer sortNumber;

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getPk() {
    return pk;
  }

  public void setPk(String pk) {
    this.pk = pk;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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
