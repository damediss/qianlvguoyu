package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_practical_assessment_sheet")
public class CompetitionPracticalAssessmentSheetEntity extends BaseEntity {
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long paperSnapshotId;
  private Integer versionNo;
  private String sheetCode;
  private String title;
  private Integer questionCount;
  private BigDecimal totalScore;
  private String sheetJson;
  private String sourceHash;
  private String status;

  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long value) { paperSnapshotId = value; }
  public Integer getVersionNo() { return versionNo; }
  public void setVersionNo(Integer value) { versionNo = value; }
  public String getSheetCode() { return sheetCode; }
  public void setSheetCode(String value) { sheetCode = value; }
  public String getTitle() { return title; }
  public void setTitle(String value) { title = value; }
  public Integer getQuestionCount() { return questionCount; }
  public void setQuestionCount(Integer value) { questionCount = value; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal value) { totalScore = value; }
  public String getSheetJson() { return sheetJson; }
  public void setSheetJson(String value) { sheetJson = value; }
  public String getSourceHash() { return sourceHash; }
  public void setSourceHash(String value) { sourceHash = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
}
