package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** demand_employee 创建请求 */
public class DemandEmployeeCreateReq {

  @NotNull
  /** 企业ID */
  private Long orgId;

  @NotBlank
  @Size(max = 255)
  /** 工种/岗位 */
  private String work;

  @Size(max = 50)
  /** 学历要求 */
  private String education;

  @Size(max = 255)
  /** 专业要求 */
  private String major;

  /** 添加时间戳 */
  private Integer addtime;

  public Long getOrgId() {
    return orgId;
  }

  public void setOrgId(Long orgId) {
    this.orgId = orgId;
  }

  public String getWork() {
    return work;
  }

  public void setWork(String work) {
    this.work = work;
  }

  public String getEducation() {
    return education;
  }

  public void setEducation(String education) {
    this.education = education;
  }

  public String getMajor() {
    return major;
  }

  public void setMajor(String major) {
    this.major = major;
  }

  public Integer getAddtime() {
    return addtime;
  }

  public void setAddtime(Integer addtime) {
    this.addtime = addtime;
  }
}
