package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.List;

@TableName("exam_make_exam_plan")
public class ExamMakeExamPlanEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer courseId;
  private Integer rateId;
  private Integer type;
  private Integer examType;
  private String title;
  private Integer time;
  private Integer isComputer;
  private Integer isPrint;
  private Integer openRate;
  private Integer totalPoint;
  private Integer passPoint;
  private String comments;
  private Integer userId;
  private Integer status;
  private Integer isAll;
  private Integer sortNumber;
  private Integer tenantId;
  private LocalDateTime createTime;

  @TableField(exist = false)
  private List<ExamMakeExamPlanDetailEntity> planDetailList;
  @TableField(exist = false)
  private Integer num;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public Integer getRateId() { return rateId; }
  public void setRateId(Integer rateId) { this.rateId = rateId; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public Integer getExamType() { return examType; }
  public void setExamType(Integer examType) { this.examType = examType; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getTime() { return time; }
  public void setTime(Integer time) { this.time = time; }
  public Integer getIsComputer() { return isComputer; }
  public void setIsComputer(Integer isComputer) { this.isComputer = isComputer; }
  public Integer getIsPrint() { return isPrint; }
  public void setIsPrint(Integer isPrint) { this.isPrint = isPrint; }
  public Integer getOpenRate() { return openRate; }
  public void setOpenRate(Integer openRate) { this.openRate = openRate; }
  public Integer getTotalPoint() { return totalPoint; }
  public void setTotalPoint(Integer totalPoint) { this.totalPoint = totalPoint; }
  public Integer getPassPoint() { return passPoint; }
  public void setPassPoint(Integer passPoint) { this.passPoint = passPoint; }
  public String getComments() { return comments; }
  public void setComments(String comments) { this.comments = comments; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getIsAll() { return isAll; }
  public void setIsAll(Integer isAll) { this.isAll = isAll; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public List<ExamMakeExamPlanDetailEntity> getPlanDetailList() { return planDetailList; }
  public void setPlanDetailList(List<ExamMakeExamPlanDetailEntity> planDetailList) { this.planDetailList = planDetailList; }
  public Integer getNum() { return num; }
  public void setNum(Integer num) { this.num = num; }
}
