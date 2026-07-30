package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("train_exam_people_list")
public class TrainExamPeopleListEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
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
  private BigDecimal normalScore;
  private BigDecimal score;
  private BigDecimal totalScore;
  private Integer learnTime;
  private Integer learnUseTime;
  private Long userId;
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
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
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
