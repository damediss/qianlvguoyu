package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_sign_up_people_self 更新请求 */
public class TrainSignUpPeopleSelfUpdateReq {

  @Size(max = 255)
  /** 报名计划ID */
  private Long signUpId;

  @Size(max = 255)
  /** 平台用户ID */
  private Long platformUserId;

  @Size(max = 255)
  /** 姓名 */
  private String name;

  @Size(max = 255)
  /** 手机号 */
  private String phone;

  @Size(max = 255)
  /** 身份证号 */
  private String idCardNumber;

  @Size(max = 255)
  /** 性别 */
  private String gender;

  @Size(max = 255)
  /** 学历 */
  private String education;

  @Size(max = 255)
  /** 照片路径 */
  private String photo;

  @Size(max = 255)
  /** 合同路径 */
  private String contract;

  @Size(max = 255)
  /** 证书路径 */
  private String certificate;

  @Size(max = 255)
  /** 状态 */
  private Integer status;
  public Long getSignUpId() { return signUpId; }
  public void setSignUpId(Long signUpId) { this.signUpId = signUpId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getIdCardNumber() { return idCardNumber; }
  public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }
  public String getGender() { return gender; }
  public void setGender(String gender) { this.gender = gender; }
  public String getEducation() { return education; }
  public void setEducation(String education) { this.education = education; }
  public String getPhoto() { return photo; }
  public void setPhoto(String photo) { this.photo = photo; }
  public String getContract() { return contract; }
  public void setContract(String contract) { this.contract = contract; }
  public String getCertificate() { return certificate; }
  public void setCertificate(String certificate) { this.certificate = certificate; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
}
