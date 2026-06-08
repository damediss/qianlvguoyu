package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 开班人员列表
 */
@TableName("train_people_list")
public class TrainPeopleListEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 培训ID */
  private Long trainId;
  /** 班级ID */
  private Long trainClassId;
  /** 人员类型 */
  private String type;
  /** 用户ID */
  private Long userId;
  /** 平台用户ID */
  private Long platformUserId;
  /** 真实姓名 */
  private String realname;
  /** 手机号 */
  private String phone;
  /** 是否组长 0-否 1-是 */
  private Integer isLeader;
  /** 是否班委 0-否 1-是 */
  private Integer isCommittee;
  /** 考试成绩 */
  private BigDecimal examScore;
  /** 实操成绩 */
  private BigDecimal realScore;
  /** 证书 */
  private String paper;
  /** 平时成绩 */
  private BigDecimal normalScore;
  /** 国家证书 */
  private String nationalPaper;
  /** 当前等级 */
  private Integer currentLevel;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 身份证号 */
  private String idCard;
  /** 审核状态: 0待审核, 1通过, 2拒绝 */
  private Integer status;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public Long getTrainClassId() { return trainClassId; }
  public void setTrainClassId(Long trainClassId) { this.trainClassId = trainClassId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
  public String getRealname() { return realname; }
  public void setRealname(String realname) { this.realname = realname; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public Integer getIsLeader() { return isLeader; }
  public void setIsLeader(Integer isLeader) { this.isLeader = isLeader; }
  public Integer getIsCommittee() { return isCommittee; }
  public void setIsCommittee(Integer isCommittee) { this.isCommittee = isCommittee; }
  public BigDecimal getExamScore() { return examScore; }
  public void setExamScore(BigDecimal examScore) { this.examScore = examScore; }
  public BigDecimal getRealScore() { return realScore; }
  public void setRealScore(BigDecimal realScore) { this.realScore = realScore; }
  public String getPaper() { return paper; }
  public void setPaper(String paper) { this.paper = paper; }
  public BigDecimal getNormalScore() { return normalScore; }
  public void setNormalScore(BigDecimal normalScore) { this.normalScore = normalScore; }
  public String getNationalPaper() { return nationalPaper; }
  public void setNationalPaper(String nationalPaper) { this.nationalPaper = nationalPaper; }
  public Integer getCurrentLevel() { return currentLevel; }
  public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public String getIdCard() { return idCard; }
  public void setIdCard(String idCard) { this.idCard = idCard; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
