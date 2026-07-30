package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** demand_train 创建请求 */
public class DemandTrainCreateReq {

  @Size(max = 255)
  /** 企业ID */
  private Long orgId;

  @Size(max = 255)
  /** 需求内容 */
  private String content;

  @Size(max = 255)
  /** 人数 */
  private Integer nums;

  @Size(max = 255)
  /** 状态 0-待处理 1-已处理 */
  private Integer status;

  @Size(max = 255)
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
