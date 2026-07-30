package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * train_sign_up_people_self
 */
@TableName("train_sign_up_people_self")
public class TrainSignUpPeopleSelfEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 报名计划ID */
  private Long signUpId;

  /** 平台用户ID */
  private Long platformUserId;

  /** 姓名 */
  private String name;

  /** 手机号 */
  private String phone;

  /** 身份证号 */
  private String idCardNumber;

  /** 性别 */
  private String gender;

  /** 学历 */
  private String education;

  /** 照片路径 */
  private String photo;

  /** 合同路径 */
  private String contract;

  /** 证书路径 */
  private String certificate;

  /** 状态 */
  private Integer status;

  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 更新时间 */
  private LocalDateTime updateTime;

  /** 逻辑删除 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
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
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
