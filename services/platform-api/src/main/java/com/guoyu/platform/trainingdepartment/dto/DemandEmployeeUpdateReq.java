package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** demand_employee 更新请求 */
public class DemandEmployeeUpdateReq {

  @Size(max = 255)
  /** 企业ID */
  private Long orgId;

  @Size(max = 255)
  /** 工种/岗位 */
  private String work;

  @Size(max = 255)
  /** 学历要求 */
  private String education;

  @Size(max = 255)
  /** 专业要求 */
  private String major;

  @Size(max = 255)
  /** 添加时间戳 */
  private Integer addtime;
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getWork() { return work; }
  public void setWork(String work) { this.work = work; }
  public String getEducation() { return education; }
  public void setEducation(String education) { this.education = education; }
  public String getMajor() { return major; }
  public void setMajor(String major) { this.major = major; }
  public Integer getAddtime() { return addtime; }
  public void setAddtime(Integer addtime) { this.addtime = addtime; }
}
