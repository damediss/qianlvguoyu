package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

/** platform_user 创建/更新 */
public class PlatformUserSaveReq {
  private Long id;
  @NotBlank
  private String realname;
  @NotBlank
  private String idcard;
  @NotBlank
  private String phone;
  private String password;
  private Long companyId;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getRealname() { return realname; }
  public void setRealname(String realname) { this.realname = realname; }
  public String getIdcard() { return idcard; }
  public void setIdcard(String idcard) { this.idcard = idcard; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
}
