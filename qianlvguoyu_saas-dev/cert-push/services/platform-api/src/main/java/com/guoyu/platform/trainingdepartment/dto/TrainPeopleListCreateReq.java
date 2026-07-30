package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 开班人员列表创建请求 */
public class TrainPeopleListCreateReq {

  /** 培训ID */
  private Long trainId;

  /** 培训班级ID */
  private Long trainClassId;

  /** 类型 */
  @Size(max = 20)
  /** 类型 */
  private String type;

  /** 用户ID */
  private Long userId;

  /** 平台用户ID */
  private Long platformUserId;

  @Size(max = 20)
  private String realname;

  @Size(max = 20)
  /** 联系电话 */
  private String phone;

  /** 是否组长 */
  private Integer isLeader;

  /** 是否班委 */
  private Integer isCommittee;

  /** 考试成绩 */
  private BigDecimal examScore;

  /** 实操成绩 */
  private BigDecimal realScore;

  /** 证书文件 */
  @Size(max = 255)
  private String paper;

  /** 平时成绩 */
  private BigDecimal normalScore;

  /** 国家证书 */
  @Size(max = 255)
  private String nationalPaper;

  /** 当前等级 */
  private Integer currentLevel;

  /** 排序号 */
  private Integer sortNumber;

  /** 身份证 */
  @Size(max = 255)
  private String idCard;

  public Long getTrainId() {
    return trainId;
  }

  public void setTrainId(Long trainId) {
    this.trainId = trainId;
  }

  public Long getTrainClassId() {
    return trainClassId;
  }

  public void setTrainClassId(Long trainClassId) {
    this.trainClassId = trainClassId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getPlatformUserId() {
    return platformUserId;
  }

  public void setPlatformUserId(Long platformUserId) {
    this.platformUserId = platformUserId;
  }

  public String getRealname() {
    return realname;
  }

  public void setRealname(String realname) {
    this.realname = realname;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public Integer getIsLeader() {
    return isLeader;
  }

  public void setIsLeader(Integer isLeader) {
    this.isLeader = isLeader;
  }

  public Integer getIsCommittee() {
    return isCommittee;
  }

  public void setIsCommittee(Integer isCommittee) {
    this.isCommittee = isCommittee;
  }

  public BigDecimal getExamScore() {
    return examScore;
  }

  public void setExamScore(BigDecimal examScore) {
    this.examScore = examScore;
  }

  public BigDecimal getRealScore() {
    return realScore;
  }

  public void setRealScore(BigDecimal realScore) {
    this.realScore = realScore;
  }

  public String getPaper() {
    return paper;
  }

  public void setPaper(String paper) {
    this.paper = paper;
  }

  public BigDecimal getNormalScore() {
    return normalScore;
  }

  public void setNormalScore(BigDecimal normalScore) {
    this.normalScore = normalScore;
  }

  public String getNationalPaper() {
    return nationalPaper;
  }

  public void setNationalPaper(String nationalPaper) {
    this.nationalPaper = nationalPaper;
  }

  public Integer getCurrentLevel() {
    return currentLevel;
  }

  public void setCurrentLevel(Integer currentLevel) {
    this.currentLevel = currentLevel;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

  public String getIdCard() {
    return idCard;
  }

  public void setIdCard(String idCard) {
    this.idCard = idCard;
  }

}
