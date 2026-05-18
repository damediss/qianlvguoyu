package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训费用
 */
@TableName("train_budget")
public class TrainBudgetEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 培训ID */
  private Long trainId;
  /** 省 */
  private String province;
  /** 市 */
  private String city;
  /** 区 */
  private String region;
  /** 企业id */
  private Long orgId;
  /** 企业 */
  private String orgName;
  /** 培训时间 */
  private String trainDate;
  /** 培训工种id */
  private Long trainWorkTypeId;
  /** 培训工种 */
  private String trainWorkTypeName;
  /** 培训级别 */
  private String trainLevel;
  /** 补贴标准 */
  private String bonusStandard;
  /** 预计收入 */
  private BigDecimal expectIncome;
  /** 实际收入 */
  private BigDecimal actualIncome;
  /** 学校管理费 */
  private BigDecimal schoolManageFee;
  /** 公司运营管理费 */
  private BigDecimal manageFee;
  /** 研发费用 */
  private BigDecimal researchFee;
  /** 企业要求分成 */
  private BigDecimal companyRequireCommission;
  /** 中介要求分成 */
  private BigDecimal agentRequireCommission;
  /** 培训讲师课酬 */
  private BigDecimal teacherCourseFee;
  /** 培训讲师食宿差旅费 */
  private BigDecimal teacherFee;
  /** 工作人员劳务费用 */
  private BigDecimal staffFee;
  /** 工作人员食宿差旅费 */
  private BigDecimal staffTravelFee;
  /** 培训耗材费 */
  private BigDecimal trainFee;
  /** 积分奖励 */
  private BigDecimal pointRewards;
  /** 业务招待费 */
  private BigDecimal businessFee;
  /** 认定人数 */
  private Integer peopleNum;
  /** 收费标准 */
  private String chargeFee;
  /** 行业服务费 */
  private BigDecimal serviceFee;
  /** 评审费用 */
  private BigDecimal reviewFee;
  /** 认定金额 */
  private BigDecimal maintainFee;
  /** 补考人数 */
  private Integer makeupPeopleNum;
  /** 理论补考 */
  private Integer theoryMakeupPeopleNum;
  /** 实操补考 */
  private Integer operationMakeupPeopleNum;
  /** 工作人员差旅费 */
  private BigDecimal staffMakeupTrainFee;
  /** 考评员差旅费 */
  private BigDecimal reviewMakeupTrainFee;
  /** 项目利润 */
  private BigDecimal profit;
  /** 备注 */
  private String comments;
  /** 状态, 0正常, 1冻结 */
  private Integer status;
  /** 用户ID */
  private Long userId;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 更新时间 */
  private LocalDateTime updateTime;
  /** 排序号 */
  private Integer sortNumber;

  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
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
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
