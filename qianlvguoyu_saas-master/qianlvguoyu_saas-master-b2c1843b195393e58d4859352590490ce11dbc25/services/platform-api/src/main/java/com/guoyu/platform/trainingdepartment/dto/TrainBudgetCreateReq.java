package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 培训预算创建请求
 */
public class TrainBudgetCreateReq {
  /** 培训ID */
  private Long trainId;
  @Size(max = 255) private String province;
  @Size(max = 255) private String city;
  @Size(max = 255) private String region;
  /** 组织ID */
  private Long orgId;
  /** 组织名称 */
  @Size(max = 255) private String orgName;
  /** 培训日期 */
  @Size(max = 255) private String trainDate;
  private Long trainWorkTypeId;
  @Size(max = 255) private String trainWorkTypeName;
  @Size(max = 255) private String trainLevel;
  @Size(max = 255) private String bonusStandard;
  private BigDecimal expectIncome;
  private BigDecimal actualIncome;
  private BigDecimal schoolManageFee;
  private BigDecimal manageFee;
  private BigDecimal researchFee;
  private BigDecimal companyRequireCommission;
  private BigDecimal agentRequireCommission;
  private BigDecimal teacherCourseFee;
  private BigDecimal teacherFee;
  private BigDecimal staffFee;
  private BigDecimal staffTravelFee;
  private BigDecimal trainFee;
  private BigDecimal pointRewards;
  private BigDecimal businessFee;
  private Integer peopleNum;
  @Size(max = 255) private String chargeFee;
  private BigDecimal serviceFee;
  private BigDecimal reviewFee;
  private BigDecimal maintainFee;
  private Integer makeupPeopleNum;
  private Integer theoryMakeupPeopleNum;
  private Integer operationMakeupPeopleNum;
  private BigDecimal staffMakeupTrainFee;
  private BigDecimal reviewMakeupTrainFee;
  private BigDecimal profit;
  private String comments;
  /** 状态 */
  private Integer status;
  /** 排序号 */
  private Integer sortNumber;

  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public String getProvince() { return province; }
  public void setProvince(String province) { this.province = province; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public String getRegion() { return region; }
  public void setRegion(String region) { this.region = region; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public String getTrainDate() { return trainDate; }
  public void setTrainDate(String trainDate) { this.trainDate = trainDate; }
  public Long getTrainWorkTypeId() { return trainWorkTypeId; }
  public void setTrainWorkTypeId(Long trainWorkTypeId) { this.trainWorkTypeId = trainWorkTypeId; }
  public String getTrainWorkTypeName() { return trainWorkTypeName; }
  public void setTrainWorkTypeName(String trainWorkTypeName) { this.trainWorkTypeName = trainWorkTypeName; }
  public String getTrainLevel() { return trainLevel; }
  public void setTrainLevel(String trainLevel) { this.trainLevel = trainLevel; }
  public String getBonusStandard() { return bonusStandard; }
  public void setBonusStandard(String bonusStandard) { this.bonusStandard = bonusStandard; }
  public BigDecimal getExpectIncome() { return expectIncome; }
  public void setExpectIncome(BigDecimal expectIncome) { this.expectIncome = expectIncome; }
  public BigDecimal getActualIncome() { return actualIncome; }
  public void setActualIncome(BigDecimal actualIncome) { this.actualIncome = actualIncome; }
  public BigDecimal getSchoolManageFee() { return schoolManageFee; }
  public void setSchoolManageFee(BigDecimal schoolManageFee) { this.schoolManageFee = schoolManageFee; }
  public BigDecimal getManageFee() { return manageFee; }
  public void setManageFee(BigDecimal manageFee) { this.manageFee = manageFee; }
  public BigDecimal getResearchFee() { return researchFee; }
  public void setResearchFee(BigDecimal researchFee) { this.researchFee = researchFee; }
  public BigDecimal getCompanyRequireCommission() { return companyRequireCommission; }
  public void setCompanyRequireCommission(BigDecimal companyRequireCommission) { this.companyRequireCommission = companyRequireCommission; }
  public BigDecimal getAgentRequireCommission() { return agentRequireCommission; }
  public void setAgentRequireCommission(BigDecimal agentRequireCommission) { this.agentRequireCommission = agentRequireCommission; }
  public BigDecimal getTeacherCourseFee() { return teacherCourseFee; }
  public void setTeacherCourseFee(BigDecimal teacherCourseFee) { this.teacherCourseFee = teacherCourseFee; }
  public BigDecimal getTeacherFee() { return teacherFee; }
  public void setTeacherFee(BigDecimal teacherFee) { this.teacherFee = teacherFee; }
  public BigDecimal getStaffFee() { return staffFee; }
  public void setStaffFee(BigDecimal staffFee) { this.staffFee = staffFee; }
  public BigDecimal getStaffTravelFee() { return staffTravelFee; }
  public void setStaffTravelFee(BigDecimal staffTravelFee) { this.staffTravelFee = staffTravelFee; }
  public BigDecimal getTrainFee() { return trainFee; }
  public void setTrainFee(BigDecimal trainFee) { this.trainFee = trainFee; }
  public BigDecimal getPointRewards() { return pointRewards; }
  public void setPointRewards(BigDecimal pointRewards) { this.pointRewards = pointRewards; }
  public BigDecimal getBusinessFee() { return businessFee; }
  public void setBusinessFee(BigDecimal businessFee) { this.businessFee = businessFee; }
  public Integer getPeopleNum() { return peopleNum; }
  public void setPeopleNum(Integer peopleNum) { this.peopleNum = peopleNum; }
  public String getChargeFee() { return chargeFee; }
  public void setChargeFee(String chargeFee) { this.chargeFee = chargeFee; }
  public BigDecimal getServiceFee() { return serviceFee; }
  public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }
  public BigDecimal getReviewFee() { return reviewFee; }
  public void setReviewFee(BigDecimal reviewFee) { this.reviewFee = reviewFee; }
  public BigDecimal getMaintainFee() { return maintainFee; }
  public void setMaintainFee(BigDecimal maintainFee) { this.maintainFee = maintainFee; }
  public Integer getMakeupPeopleNum() { return makeupPeopleNum; }
  public void setMakeupPeopleNum(Integer makeupPeopleNum) { this.makeupPeopleNum = makeupPeopleNum; }
  public Integer getTheoryMakeupPeopleNum() { return theoryMakeupPeopleNum; }
  public void setTheoryMakeupPeopleNum(Integer theoryMakeupPeopleNum) { this.theoryMakeupPeopleNum = theoryMakeupPeopleNum; }
  public Integer getOperationMakeupPeopleNum() { return operationMakeupPeopleNum; }
  public void setOperationMakeupPeopleNum(Integer operationMakeupPeopleNum) { this.operationMakeupPeopleNum = operationMakeupPeopleNum; }
  public BigDecimal getStaffMakeupTrainFee() { return staffMakeupTrainFee; }
  public void setStaffMakeupTrainFee(BigDecimal staffMakeupTrainFee) { this.staffMakeupTrainFee = staffMakeupTrainFee; }
  public BigDecimal getReviewMakeupTrainFee() { return reviewMakeupTrainFee; }
  public void setReviewMakeupTrainFee(BigDecimal reviewMakeupTrainFee) { this.reviewMakeupTrainFee = reviewMakeupTrainFee; }
  public BigDecimal getProfit() { return profit; }
  public void setProfit(BigDecimal profit) { this.profit = profit; }
  public String getComments() { return comments; }
  public void setComments(String comments) { this.comments = comments; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
