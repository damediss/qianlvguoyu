package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_make_exam_paper_snapshot")
public class CompetitionMakeExamPaperSnapshotEntity extends BaseEntity {

  private Long planId;
  private Long eventId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String contentType;
  private String paperCode;
  private Integer versionNo;
  private String title;
  private Integer durationMinutes;
  private Integer questionCount;
  private BigDecimal totalScore;
  private BigDecimal passScore;
  private String status;
  private String randomSeed;
  private String sourceHash;

  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public String getPaperCode() { return paperCode; }
  public void setPaperCode(String paperCode) { this.paperCode = paperCode; }
  public Integer getVersionNo() { return versionNo; }
  public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
  public Integer getQuestionCount() { return questionCount; }
  public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public BigDecimal getPassScore() { return passScore; }
  public void setPassScore(BigDecimal passScore) { this.passScore = passScore; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRandomSeed() { return randomSeed; }
  public void setRandomSeed(String randomSeed) { this.randomSeed = randomSeed; }
  public String getSourceHash() { return sourceHash; }
  public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }
}
