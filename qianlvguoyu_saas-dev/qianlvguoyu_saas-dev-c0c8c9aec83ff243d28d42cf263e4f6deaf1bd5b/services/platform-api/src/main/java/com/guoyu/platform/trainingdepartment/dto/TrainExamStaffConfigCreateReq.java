package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_exam_staff_config 创建请求 */
public class TrainExamStaffConfigCreateReq {

  @Size(max = 255)
  /** 人员类型 */
  private String type;

  @Size(max = 255)
  /** 姓名 */
  private String realname;

  @Size(max = 255)
  /** 用户ID */
  private Long userId;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getRealname() {
    return realname;
  }

  public void setRealname(String realname) {
    this.realname = realname;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }
}
