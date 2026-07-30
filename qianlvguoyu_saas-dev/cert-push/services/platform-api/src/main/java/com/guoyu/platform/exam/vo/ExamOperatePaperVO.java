package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamOperatePaperEntity;
import java.time.LocalDateTime;

public class ExamOperatePaperVO {

  private Integer id;
  private Integer makeId;
  private Integer source;
  private Integer workTypeId;
  private Integer courseId;
  private Integer num;
  private String code;
  private String level;
  private String title;
  private Integer time;
  private Integer totalPoint;
  private Integer passPoint;
  private String comments;
  private Integer userId;
  private Integer status;
  private Integer sortNumber;
  private Integer tenantId;
  private LocalDateTime createTime;

  public static ExamOperatePaperVO from(ExamOperatePaperEntity entity) {
    ExamOperatePaperVO vo = new ExamOperatePaperVO();
    vo.setId(entity.getId());
    vo.setMakeId(entity.getMakeId());
    vo.setSource(entity.getSource());
    vo.setWorkTypeId(entity.getWorkTypeId());
    vo.setCourseId(entity.getCourseId());
    vo.setNum(entity.getNum());
    vo.setCode(entity.getCode());
    vo.setLevel(entity.getLevel());
    vo.setTitle(entity.getTitle());
    vo.setTime(entity.getTime());
    vo.setTotalPoint(entity.getTotalPoint());
    vo.setPassPoint(entity.getPassPoint());
    vo.setComments(entity.getComments());
    vo.setUserId(entity.getUserId());
    vo.setStatus(entity.getStatus());
    vo.setSortNumber(entity.getSortNumber());
    vo.setTenantId(entity.getTenantId());
    vo.setCreateTime(entity.getCreateTime());
    return vo;
  }

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getMakeId() { return makeId; }
  public void setMakeId(Integer makeId) { this.makeId = makeId; }
  public Integer getSource() { return source; }
  public void setSource(Integer source) { this.source = source; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public Integer getNum() { return num; }
  public void setNum(Integer num) { this.num = num; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getTime() { return time; }
  public void setTime(Integer time) { this.time = time; }
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
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
