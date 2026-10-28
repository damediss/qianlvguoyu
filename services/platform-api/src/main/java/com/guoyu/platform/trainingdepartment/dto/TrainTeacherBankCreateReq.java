package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 师资库创建请求 */
public class TrainTeacherBankCreateReq {

  @Size(max = 20)
  /** 名称 */
  private String name;

  @Size(max = 30)
  /** 联系电话 */
  private String phone;

  /** 身份证 */
  @Size(max = 255)
  private String idCard;

  /** 教师资格证 */
  private String teacherLicense;

  /** 工程师资格证 */
  private String engineerLicense;

  /** 等级证书 */
  private String levelLicense;

  /** 学历证书 */
  private String eduLicense;

  /** 毕业证书 */
  private String graduatePaper;

  /** 工作单位 */
  @Size(max = 255)
  /** 单位 */
  private String company;

  /** 归属企业组织 ID */
  private Long companyId;

  @Size(max = 500)
  /** 培训地点 */
  private String address;

  /** 身份证号码 */
  @Size(max = 255)
  private String idCardNumber;

  /** 开始日期 */
  @Size(max = 30)
  private String startDate;

  /** 结束日期 */
  @Size(max = 30)
  private String endDate;

  /** 生日 */
  @Size(max = 30)
  private String birthday;

  /** 性别 0女 1男 */
  private Integer gender;

  /** 民族 */
  @Size(max = 20)
  private String nation;

  /** 学历 */
  @Size(max = 50)
  /** 学历 */
  private String education;

  /** 专业 */
  @Size(max = 50)
  /** 专业 */
  private String major;

  @Size(max = 512)
  private String skillName;
  @Size(max = 255)
  private String qualificationLevel;
  @Size(max = 255)
  private String professionalTitle;
  private String certText;
  @Size(max = 128)
  private String teacherQualNo;
  @Size(max = 128)
  private String titleCertNo;
  @Size(max = 255)
  private String skillCertNo;

  /** 状态 */
  private Integer status;

  /** 用户ID */
  private Long userId;

  /** 排序号 */
  private Integer sortNumber;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public String getTeacherLicense() {
    return teacherLicense;
  }

  public void setTeacherLicense(String teacherLicense) {
    this.teacherLicense = teacherLicense;
  }

  public String getEngineerLicense() {
    return engineerLicense;
  }

  public void setEngineerLicense(String engineerLicense) {
    this.engineerLicense = engineerLicense;
  }

  public String getLevelLicense() {
    return levelLicense;
  }

  public void setLevelLicense(String levelLicense) {
    this.levelLicense = levelLicense;
  }

  public String getEduLicense() {
    return eduLicense;
  }

  public void setEduLicense(String eduLicense) {
    this.eduLicense = eduLicense;
  }

  public String getGraduatePaper() {
    return graduatePaper;
  }

  public void setGraduatePaper(String graduatePaper) {
    this.graduatePaper = graduatePaper;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getIdCardNumber() {
    return idCardNumber;
  }

  public void setIdCardNumber(String idCardNumber) {
    this.idCardNumber = idCardNumber;
  }

  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  public String getBirthday() {
    return birthday;
  }

  public void setBirthday(String birthday) {
    this.birthday = birthday;
  }

  public Integer getGender() {
    return gender;
  }

  public void setGender(Integer gender) {
    this.gender = gender;
  }

  public String getNation() {
    return nation;
  }

  public void setNation(String nation) {
    this.nation = nation;
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

  public String getSkillName() {
    return skillName;
  }

  public void setSkillName(String skillName) {
    this.skillName = skillName;
  }

  public String getQualificationLevel() {
    return qualificationLevel;
  }

  public void setQualificationLevel(String qualificationLevel) {
    this.qualificationLevel = qualificationLevel;
  }

  public String getProfessionalTitle() {
    return professionalTitle;
  }

  public void setProfessionalTitle(String professionalTitle) {
    this.professionalTitle = professionalTitle;
  }

  public String getCertText() {
    return certText;
  }

  public void setCertText(String certText) {
    this.certText = certText;
  }

  public String getTeacherQualNo() {
    return teacherQualNo;
  }

  public void setTeacherQualNo(String teacherQualNo) {
    this.teacherQualNo = teacherQualNo;
  }

  public String getTitleCertNo() {
    return titleCertNo;
  }

  public void setTitleCertNo(String titleCertNo) {
    this.titleCertNo = titleCertNo;
  }

  public String getSkillCertNo() {
    return skillCertNo;
  }

  public void setSkillCertNo(String skillCertNo) {
    this.skillCertNo = skillCertNo;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
