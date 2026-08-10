package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 指派班级角色 */
public class TrainClassRoleAssignReq {

  @NotBlank
  private String roleCode;
  /** teacher_bank / people / platform_user；可空时按来源推断 */
  private String subjectType;
  @NotNull
  private Long subjectId;
  private Long peopleId;
  private String displayName;
  private String phone;
  private String idCard;
  /** 是否尝试按手机号/身份证匹配 platform_user（不强制建号） */
  private Boolean bindPlatformUser = true;

  public String getRoleCode() {
    return roleCode;
  }

  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }

  public String getSubjectType() {
    return subjectType;
  }

  public void setSubjectType(String subjectType) {
    this.subjectType = subjectType;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public void setSubjectId(Long subjectId) {
    this.subjectId = subjectId;
  }

  public Long getPeopleId() {
    return peopleId;
  }

  public void setPeopleId(Long peopleId) {
    this.peopleId = peopleId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getIdCard() {
    return idCard;
  }

  public void setIdCard(String idCard) {
    this.idCard = idCard;
  }

  public Boolean getBindPlatformUser() {
    return bindPlatformUser;
  }

  public void setBindPlatformUser(Boolean bindPlatformUser) {
    this.bindPlatformUser = bindPlatformUser;
  }
}
