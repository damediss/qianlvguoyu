package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户证书更新请求 */
public class TrainUserPaperUpdateReq {

  @Size(max = 500)
  private String url;

  @NotBlank
  @Size(max = 255)
  private String title;

  /** 证书类型 */
  @Size(max = 255)
  private String type;

  private Integer sortNumber;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
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

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
