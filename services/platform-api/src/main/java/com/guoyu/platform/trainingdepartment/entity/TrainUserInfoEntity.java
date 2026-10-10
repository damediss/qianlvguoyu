package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 参与培训人员信息
 */
@TableName("train_user_info")
public class TrainUserInfoEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户ID */
  private Long userId;
  /** 企业ID */
  private Long companyId;
  /** 人员类型 */
  private String personType;
  /** 人员补充信息 */
  private String personSupplementary;
  /** 户籍类型 */
  private String houseType;
  /** 国籍 */
  private String nationality;
  /** 学历 */
  private String education;
  /** 求职意向 */
  private String jobIntention;
  /** 是否党员 0-否 1-是 */
  private Integer isPartyMember;
  /** 地址 */
  private String address;
  /** 身份证正面 */
  private String idCardFront;
  /** 身份证反面 */
  private String idCardBack;
  /** 照片 */
  private String photo;
  /** 学历照片 */
  private String eduPhoto;
  /** 劳动合同 */
  private String workContract;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public String getPersonType() { return personType; }
  public void setPersonType(String personType) { this.personType = personType; }
  public String getPersonSupplementary() { return personSupplementary; }
  public void setPersonSupplementary(String personSupplementary) { this.personSupplementary = personSupplementary; }
  public String getHouseType() { return houseType; }
  public void setHouseType(String houseType) { this.houseType = houseType; }
  public String getNationality() { return nationality; }
  public void setNationality(String nationality) { this.nationality = nationality; }
  public String getEducation() { return education; }
  public void setEducation(String education) { this.education = education; }
  public String getJobIntention() { return jobIntention; }
  public void setJobIntention(String jobIntention) { this.jobIntention = jobIntention; }
  public Integer getIsPartyMember() { return isPartyMember; }
  public void setIsPartyMember(Integer isPartyMember) { this.isPartyMember = isPartyMember; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getIdCardFront() { return idCardFront; }
  public void setIdCardFront(String idCardFront) { this.idCardFront = idCardFront; }
  public String getIdCardBack() { return idCardBack; }
  public void setIdCardBack(String idCardBack) { this.idCardBack = idCardBack; }
  public String getPhoto() { return photo; }
  public void setPhoto(String photo) { this.photo = photo; }
  public String getEduPhoto() { return eduPhoto; }
  public void setEduPhoto(String eduPhoto) { this.eduPhoto = eduPhoto; }
  public String getWorkContract() { return workContract; }
  public void setWorkContract(String workContract) { this.workContract = workContract; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
