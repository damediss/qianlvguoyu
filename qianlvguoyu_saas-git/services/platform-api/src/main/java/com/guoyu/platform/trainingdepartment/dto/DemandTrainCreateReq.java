package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** demand_train 创建请求 */
public class DemandTrainCreateReq {

  @NotNull
  /** 企业ID */
  private Long orgId;

  @NotBlank
  /** 需求内容 */
  private String content;

  /** 人数 */
  private Integer nums;

  /** 状态 0-待处理 1-已处理 */
  private Integer status;

  /** 添加时间戳 */
  private Integer addtime;

  public Long getOrgId() {
    return orgId;
  }

  public void setOrgId(Long orgId) {
    this.orgId = orgId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Integer getNums() {
    return nums;
  }

  public void setNums(Integer nums) {
    this.nums = nums;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getAddtime() {
    return addtime;
  }

  public void setAddtime(Integer addtime) {
    this.addtime = addtime;
  }
}
