package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户专利申请请求 */
public class TrainUserPatentCreateReq {

  @NotBlank
  @Size(max = 255)
  /** 标题 */
  private String title;

  /** 专利证书文件 */
  @Size(max = 255)
  private String paper;

  /** 用户ID */
  private Long userId;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPaper() {
    return paper;
  }

  public void setPaper(String paper) {
    this.paper = paper;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

}
