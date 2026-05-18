package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_request")
public class TrainRequestEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String companyName;
  private Long companyId;
  private String position;
  private Integer num;
  private String type;
  private String trainDate;
  private String contact;
  private String contactPhone;
  private Integer isOnPosition;
  private String studentForm;
  private Long userId;
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getCompanyName() { return companyName; }
  public void setCompanyName(String companyName) { this.companyName = companyName; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public String getPosition() { return position; }
  public void setPosition(String position) { this.position = position; }
  public Integer getNum() { return num; }
  public void setNum(Integer num) { this.num = num; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getTrainDate() { return trainDate; }
  public void setTrainDate(String trainDate) { this.trainDate = trainDate; }
  public String getContact() { return contact; }
  public void setContact(String contact) { this.contact = contact; }
  public String getContactPhone() { return contactPhone; }
  public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
  public Integer getIsOnPosition() { return isOnPosition; }
  public void setIsOnPosition(Integer isOnPosition) { this.isOnPosition = isOnPosition; }
  public String getStudentForm() { return studentForm; }
  public void setStudentForm(String studentForm) { this.studentForm = studentForm; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
