package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;

public class TrainExamPeopleListCreateReq {

  @NotNull(message = "考试ID不能为空")
  private Long examId;
  private Long platformUserId;
  private Long workTypeId;
  private String workTypeLevel;
  private String realname;
  private String idCardNum;
  private String phone;
  private String education;
  private Long roomId;
  private Integer inWhiteList;
  private Integer sortNumber;

  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public String getWorkTypeLevel() { return workTypeLevel; }
  public void setWorkTypeLevel(String workTypeLevel) { this.workTypeLevel = workTypeLevel; }
  public String getRealname() { return realname; }
  public void setRealname(String realname) { this.realname = realname; }
  public String getIdCardNum() { return idCardNum; }
  public void setIdCardNum(String idCardNum) { this.idCardNum = idCardNum; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getEducation() { return education; }
  public void setEducation(String education) { this.education = education; }
  public Long getRoomId() { return roomId; }
  public void setRoomId(Long roomId) { this.roomId = roomId; }
  public Integer getInWhiteList() { return inWhiteList; }
  public void setInWhiteList(Integer inWhiteList) { this.inWhiteList = inWhiteList; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
