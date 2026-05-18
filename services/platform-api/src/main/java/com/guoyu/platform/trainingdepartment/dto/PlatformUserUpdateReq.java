package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 平台用户更新请求 */
public class PlatformUserUpdateReq {

  /** 真实姓名 */
  @Size(max = 20)
  private String realname;

  /** 手机号 */
  @Size(max = 11)
  private String phone;

  /** 身份证号 */
  @Size(max = 20)
  private String idcard;

  /** 状态 */
  @Size(max = 4)
  private String stat;

  /** 机构ID */
  private Long orgId;

  /** 企业ID */
  private Long companyId;

  /** 微信OpenID */
  @Size(max = 100)
  private String wxopenid;

  /** 专家ID */
  private Long expertId;

  /** 任务标识 */
  private Integer task;

  public String getRealname() {
    return realname;
  }

  public void setRealname(String realname) {
    this.realname = realname;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getIdcard() {
    return idcard;
  }

  public void setIdcard(String idcard) {
    this.idcard = idcard;
  }

  public String getStat() {
    return stat;
  }

  public void setStat(String stat) {
    this.stat = stat;
  }

  public Long getOrgId() {
    return orgId;
  }

  public void setOrgId(Long orgId) {
    this.orgId = orgId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public String getWxopenid() {
    return wxopenid;
  }

  public void setWxopenid(String wxopenid) {
    this.wxopenid = wxopenid;
  }

  public Long getExpertId() {
    return expertId;
  }

  public void setExpertId(Long expertId) {
    this.expertId = expertId;
  }

  public Integer getTask() {
    return task;
  }

  public void setTask(Integer task) {
    this.task = task;
  }
}
