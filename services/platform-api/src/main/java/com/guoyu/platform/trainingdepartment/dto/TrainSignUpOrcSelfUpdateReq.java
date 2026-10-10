package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_sign_up_orc_self 更新请求 */
public class TrainSignUpOrcSelfUpdateReq {

  @Size(max = 255)
  /** 报名计划ID */
  private Long signUpId;

  @Size(max = 255)
  /** 平台用户ID */
  private Long platformUserId;

  @Size(max = 255)
  /** OCR识别原始数据 */
  private String orcData;

  @Size(max = 255)
  /** 状态 0-失败 1-成功 */
  private Integer status;

  @Size(max = 255)
  /** 失败原因 */
  private String failReason;
  public Long getSignUpId() { return signUpId; }
  public void setSignUpId(Long signUpId) { this.signUpId = signUpId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
  public String getOrcData() { return orcData; }
  public void setOrcData(String orcData) { this.orcData = orcData; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getFailReason() { return failReason; }
  public void setFailReason(String failReason) { this.failReason = failReason; }
}
