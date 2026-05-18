package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 参与培训人员信息更新请求 */
public class TrainUserInfoUpdateReq {

  private Long companyId;

  /** 人员类型 */
  @Size(max = 200)
  private String personType;

  /** 人员补充信息 */
  @Size(max = 200)
  private String personSupplementary;

  /** 住房类型 */
  @Size(max = 200)
  private String houseType;

  /** 国籍 */
  @Size(max = 20)
  private String nationality;

  /** 学历 */
  @Size(max = 200)
  private String education;

  /** 求职意向 */
  @Size(max = 200)
  private String jobIntention;

  /** 是否党员 0否 1是 */
  private Integer isPartyMember;

  @Size(max = 500)
  private String address;

  /** 身份证正面 */
  @Size(max = 255)
  private String idCardFront;

  /** 身份证反面 */
  @Size(max = 255)
  private String idCardBack;

  /** 照片 */
  @Size(max = 255)
  private String photo;

  /** 学历照片 */
  private String eduPhoto;

  /** 劳动合同 */
  private String workContract;

  private Integer sortNumber;

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public String getPersonType() {
    return personType;
  }

  public void setPersonType(String personType) {
    this.personType = personType;
  }

  public String getPersonSupplementary() {
    return personSupplementary;
  }

  public void setPersonSupplementary(String personSupplementary) {
    this.personSupplementary = personSupplementary;
  }

  public String getHouseType() {
    return houseType;
  }

  public void setHouseType(String houseType) {
    this.houseType = houseType;
  }

  public String getNationality() {
    return nationality;
  }

  public void setNationality(String nationality) {
    this.nationality = nationality;
  }

  public String getEducation() {
    return education;
  }

  public void setEducation(String education) {
    this.education = education;
  }

  public String getJobIntention() {
    return jobIntention;
  }

  public void setJobIntention(String jobIntention) {
    this.jobIntention = jobIntention;
  }

  public Integer getIsPartyMember() {
    return isPartyMember;
  }

  public void setIsPartyMember(Integer isPartyMember) {
    this.isPartyMember = isPartyMember;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getIdCardFront() {
    return idCardFront;
  }

  public void setIdCardFront(String idCardFront) {
    this.idCardFront = idCardFront;
  }

  public String getIdCardBack() {
    return idCardBack;
  }

  public void setIdCardBack(String idCardBack) {
    this.idCardBack = idCardBack;
  }

  public String getPhoto() {
    return photo;
  }

  public void setPhoto(String photo) {
    this.photo = photo;
  }

  public String getEduPhoto() {
    return eduPhoto;
  }

  public void setEduPhoto(String eduPhoto) {
    this.eduPhoto = eduPhoto;
  }

  public String getWorkContract() {
    return workContract;
  }

  public void setWorkContract(String workContract) {
    this.workContract = workContract;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
