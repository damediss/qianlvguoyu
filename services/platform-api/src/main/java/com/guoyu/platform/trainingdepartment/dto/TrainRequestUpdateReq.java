package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

public class TrainRequestUpdateReq {
  @Size(max = 255) private String companyName;
  private Long companyId;
  @Size(max = 255) private String position;
  private Integer num;
  @Size(max = 255) private String type;
  @Size(max = 255) private String trainDate;
  @Size(max = 255) private String contact;
  @Size(max = 255) private String contactPhone;
  private Integer isOnPosition;
  private String studentForm;
  private Integer sortNumber;

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
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
