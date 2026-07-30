package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("exam_course")
public class ExamCourseEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer workTypeId;
  private String title;
  private Integer level;
  private String seriesNoGroup;
  private String seriesNo;
  private Integer userInCharge;
  private String userInChargeName;
  private Integer lastLevelGetYear;
  private Integer workYearLimit;
  private Integer minLevel;
  private Integer minLevelYear;
  private Integer startAge;
  private Integer isGeneral;
  private String examCondition;
  private String version;
  private String comments;
  private Integer userId;
  private Integer status;
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
  public String getSeriesNoGroup() { return seriesNoGroup; }
  public void setSeriesNoGroup(String seriesNoGroup) { this.seriesNoGroup = seriesNoGroup; }
  public String getSeriesNo() { return seriesNo; }
  public void setSeriesNo(String seriesNo) { this.seriesNo = seriesNo; }
  public Integer getUserInCharge() { return userInCharge; }
  public void setUserInCharge(Integer userInCharge) { this.userInCharge = userInCharge; }
  public String getUserInChargeName() { return userInChargeName; }
  public void setUserInChargeName(String userInChargeName) { this.userInChargeName = userInChargeName; }
  public Integer getLastLevelGetYear() { return lastLevelGetYear; }
  public void setLastLevelGetYear(Integer lastLevelGetYear) { this.lastLevelGetYear = lastLevelGetYear; }
  public Integer getWorkYearLimit() { return workYearLimit; }
  public void setWorkYearLimit(Integer workYearLimit) { this.workYearLimit = workYearLimit; }
  public Integer getMinLevel() { return minLevel; }
  public void setMinLevel(Integer minLevel) { this.minLevel = minLevel; }
  public Integer getMinLevelYear() { return minLevelYear; }
  public void setMinLevelYear(Integer minLevelYear) { this.minLevelYear = minLevelYear; }
  public Integer getStartAge() { return startAge; }
  public void setStartAge(Integer startAge) { this.startAge = startAge; }
  public Integer getIsGeneral() { return isGeneral; }
  public void setIsGeneral(Integer isGeneral) { this.isGeneral = isGeneral; }
  public String getExamCondition() { return examCondition; }
  public void setExamCondition(String examCondition) { this.examCondition = examCondition; }
  public String getVersion() { return version; }
  public void setVersion(String version) { this.version = version; }
  public String getComments() { return comments; }
  public void setComments(String comments) { this.comments = comments; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}