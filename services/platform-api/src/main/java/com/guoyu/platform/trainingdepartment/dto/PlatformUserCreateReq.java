package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 平台用户创建请求 */
public class PlatformUserCreateReq {

  @NotBlank
  @Size(max = 20)
  private String realname;

  @NotBlank
  @Size(max = 11)
  private String phone;

  @NotBlank
  @Size(max = 100)
  private String password;

  /** 身份证号 */
  @Size(max = 20)
  private String idcard;

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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getIdcard() {
    return idcard;
  }

  public void setIdcard(String idcard) {
    this.idcard = idcard;
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
