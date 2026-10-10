package com.guoyu.platform.trainingdepartment.dto;

import java.math.BigDecimal;

public class TrainExamPeopleListUpdateReq {

  private Long workTypeId;
  private String workTypeLevel;
  private String realname;
  private String idCardNum;
  private String phone;
  private String education;
  private Long roomId;
  private Integer inWhiteList;
  private BigDecimal normalScore;
  private BigDecimal score;
  private BigDecimal totalScore;
  private Integer learnTime;
  private Integer learnUseTime;
  private Integer sortNumber;

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
  public BigDecimal getNormalScore() { return normalScore; }
  public void setNormalScore(BigDecimal normalScore) { this.normalScore = normalScore; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public Integer getLearnTime() { return learnTime; }
  public void setLearnTime(Integer learnTime) { this.learnTime = learnTime; }
  public Integer getLearnUseTime() { return learnUseTime; }
  public void setLearnUseTime(Integer learnUseTime) { this.learnUseTime = learnUseTime; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
