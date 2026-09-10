package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_make_exam_plan")
public class CompetitionMakeExamPlanEntity extends BaseEntity {

  private Long eventId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String contentType;
  private String title;
  private String examType;
  private Integer durationMinutes;
  private BigDecimal totalScore;
  private BigDecimal passScore;
  private Integer paperCount;
  private Integer planVersion;
  private String status;
  private String comments;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getExamType() { return examType; }
  public void setExamType(String examType) { this.examType = examType; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public BigDecimal getPassScore() { return passScore; }
  public void setPassScore(BigDecimal passScore) { this.passScore = passScore; }
  public Integer getPaperCount() { return paperCount; }
  public void setPaperCount(Integer paperCount) { this.paperCount = paperCount; }
  public Integer getPlanVersion() { return planVersion; }
  public void setPlanVersion(Integer planVersion) { this.planVersion = planVersion; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getComments() { return comments; }
  public void setComments(String comments) { this.comments = comments; }
}
