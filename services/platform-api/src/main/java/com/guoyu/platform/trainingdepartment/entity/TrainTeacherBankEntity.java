package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 师资库
 */
@TableName("train_teacher_bank")
public class TrainTeacherBankEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 姓名 */
  private String name;
  /** 手机号 */
  private String phone;
  /** 身份证照片 */
  private String idCard;
  /** 教师资格证 */
  private String teacherLicense;
  /** 工程师证 */
  private String engineerLicense;
  /** 等级证书 */
  private String levelLicense;
  /** 学历证书 */
  private String eduLicense;
  /** 毕业证书 */
  private String graduatePaper;
  /** 工作单位 */
  private String company;
  /** 地址 */
  private String address;
  /** 身份证号 */
  private String idCardNumber;
  /** 开始日期 */
  private String startDate;
  /** 结束日期 */
  private String endDate;
  /** 生日 */
  private String birthday;
  /** 性别 0-女 1-男 */
  private Integer gender;
  /** 民族 */
  private String nation;
  /** 学历 */
  private String education;
  /** 专业 */
  private String major;
  /** 职业技能名称 */
  private String skillName;
  /** 职业资格等级 */
  private String qualificationLevel;
  /** 专业技术职务 */
  private String professionalTitle;
  /** 资格证书原文 */
  private String certText;
  /** 教师资格证号 */
  private String teacherQualNo;
  /** 职称证号 */
  private String titleCertNo;
  /** 技能等级证号摘要 */
  private String skillCertNo;
  private Long idCardFrontFileId;
  private Long idCardBackFileId;
  private Long graduateFileId;
  private Long educationFileId;
  private Long teacherQualFileId;
  private Long titleCertFileId;
  private Long skillCertFileId;
  /** PHP 原 id */
  private Long legacyId;
  /** legacy/excel/manual */
  private String source;
  /** 状态 */
  private Integer status;
  /** 租户ID */
  private Long tenantId;
  /** 用户ID */
  private Long userId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 更新时间 */
  private LocalDateTime updateTime;
  /** 排序号 */
  private Integer sortNumber;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getIdCard() { return idCard; }
  public void setIdCard(String idCard) { this.idCard = idCard; }
  public String getTeacherLicense() { return teacherLicense; }
  public void setTeacherLicense(String teacherLicense) { this.teacherLicense = teacherLicense; }
  public String getEngineerLicense() { return engineerLicense; }
  public void setEngineerLicense(String engineerLicense) { this.engineerLicense = engineerLicense; }
  public String getLevelLicense() { return levelLicense; }
  public void setLevelLicense(String levelLicense) { this.levelLicense = levelLicense; }
  public String getEduLicense() { return eduLicense; }
  public void setEduLicense(String eduLicense) { this.eduLicense = eduLicense; }
  public String getGraduatePaper() { return graduatePaper; }
  public void setGraduatePaper(String graduatePaper) { this.graduatePaper = graduatePaper; }
  public String getCompany() { return company; }
  public void setCompany(String company) { this.company = company; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getIdCardNumber() { return idCardNumber; }
  public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }
  public String getStartDate() { return startDate; }
  public void setStartDate(String startDate) { this.startDate = startDate; }
  public String getEndDate() { return endDate; }
  public void setEndDate(String endDate) { this.endDate = endDate; }
  public String getBirthday() { return birthday; }
  public void setBirthday(String birthday) { this.birthday = birthday; }
  public Integer getGender() { return gender; }
  public void setGender(Integer gender) { this.gender = gender; }
  public String getNation() { return nation; }
  public void setNation(String nation) { this.nation = nation; }
  public String getEducation() { return education; }
  public void setEducation(String education) { this.education = education; }
  public String getMajor() { return major; }
  public void setMajor(String major) { this.major = major; }
  public String getSkillName() { return skillName; }
  public void setSkillName(String skillName) { this.skillName = skillName; }
  public String getQualificationLevel() { return qualificationLevel; }
  public void setQualificationLevel(String qualificationLevel) { this.qualificationLevel = qualificationLevel; }
  public String getProfessionalTitle() { return professionalTitle; }
  public void setProfessionalTitle(String professionalTitle) { this.professionalTitle = professionalTitle; }
  public String getCertText() { return certText; }
  public void setCertText(String certText) { this.certText = certText; }
  public String getTeacherQualNo() { return teacherQualNo; }
  public void setTeacherQualNo(String teacherQualNo) { this.teacherQualNo = teacherQualNo; }
  public String getTitleCertNo() { return titleCertNo; }
  public void setTitleCertNo(String titleCertNo) { this.titleCertNo = titleCertNo; }
  public String getSkillCertNo() { return skillCertNo; }
  public void setSkillCertNo(String skillCertNo) { this.skillCertNo = skillCertNo; }
  public Long getIdCardFrontFileId() { return idCardFrontFileId; }
  public void setIdCardFrontFileId(Long idCardFrontFileId) { this.idCardFrontFileId = idCardFrontFileId; }
  public Long getIdCardBackFileId() { return idCardBackFileId; }
  public void setIdCardBackFileId(Long idCardBackFileId) { this.idCardBackFileId = idCardBackFileId; }
  public Long getGraduateFileId() { return graduateFileId; }
  public void setGraduateFileId(Long graduateFileId) { this.graduateFileId = graduateFileId; }
  public Long getEducationFileId() { return educationFileId; }
  public void setEducationFileId(Long educationFileId) { this.educationFileId = educationFileId; }
  public Long getTeacherQualFileId() { return teacherQualFileId; }
  public void setTeacherQualFileId(Long teacherQualFileId) { this.teacherQualFileId = teacherQualFileId; }
  public Long getTitleCertFileId() { return titleCertFileId; }
  public void setTitleCertFileId(Long titleCertFileId) { this.titleCertFileId = titleCertFileId; }
  public Long getSkillCertFileId() { return skillCertFileId; }
  public void setSkillCertFileId(Long skillCertFileId) { this.skillCertFileId = skillCertFileId; }
  public Long getLegacyId() { return legacyId; }
  public void setLegacyId(Long legacyId) { this.legacyId = legacyId; }
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
