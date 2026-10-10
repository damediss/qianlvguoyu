package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamMakeExamPlanEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ExamMakeExamPlanVO {

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
  private List<ExamMakeExamPlanDetailVO> planDetailList;

  public static ExamMakeExamPlanVO from(ExamMakeExamPlanEntity entity) {
    ExamMakeExamPlanVO vo = new ExamMakeExamPlanVO();
    vo.setId(entity.getId());
    vo.setCourseId(entity.getCourseId());
    vo.setRateId(entity.getRateId());
    vo.setType(entity.getType());
    vo.setExamType(entity.getExamType());
    vo.setTitle(entity.getTitle());
    vo.setTime(entity.getTime());
    vo.setIsComputer(entity.getIsComputer());
    vo.setIsPrint(entity.getIsPrint());
    vo.setOpenRate(entity.getOpenRate());
    vo.setTotalPoint(entity.getTotalPoint());
    vo.setPassPoint(entity.getPassPoint());
    vo.setComments(entity.getComments());
    vo.setUserId(entity.getUserId());
    vo.setStatus(entity.getStatus());
    vo.setIsAll(entity.getIsAll());
    vo.setSortNumber(entity.getSortNumber());
    vo.setTenantId(entity.getTenantId());
    vo.setCreateTime(entity.getCreateTime());
    if (entity.getPlanDetailList() != null) {
      vo.setPlanDetailList(entity.getPlanDetailList().stream()
          .map(ExamMakeExamPlanDetailVO::from)
          .collect(Collectors.toList()));
    }
    return vo;
  }

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
  public List<ExamMakeExamPlanDetailVO> getPlanDetailList() { return planDetailList; }
  public void setPlanDetailList(List<ExamMakeExamPlanDetailVO> planDetailList) { this.planDetailList = planDetailList; }
}
