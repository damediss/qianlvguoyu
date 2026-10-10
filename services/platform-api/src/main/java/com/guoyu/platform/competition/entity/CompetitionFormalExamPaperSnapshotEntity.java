package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_formal_exam_paper_snapshot")
public class CompetitionFormalExamPaperSnapshotEntity extends BaseEntity {

  private Long examSessionId;
  private Long sourcePaperId;
  private Long sourceOriginPaperId;
  private Long scoreRuleId;
  private String paperName;
  private String paperCode;
  private BigDecimal totalScore;
  private Integer questionCount;
  private Integer durationMinutes;
  private Integer snapshotVersion;

  public Long getExamSessionId() { return examSessionId; }
  public void setExamSessionId(Long examSessionId) { this.examSessionId = examSessionId; }
  public Long getSourcePaperId() { return sourcePaperId; }
  public void setSourcePaperId(Long sourcePaperId) { this.sourcePaperId = sourcePaperId; }
  public Long getSourceOriginPaperId() { return sourceOriginPaperId; }
  public void setSourceOriginPaperId(Long sourceOriginPaperId) {
    this.sourceOriginPaperId = sourceOriginPaperId;
  }
  public Long getScoreRuleId() { return scoreRuleId; }
  public void setScoreRuleId(Long scoreRuleId) { this.scoreRuleId = scoreRuleId; }
  public String getPaperName() { return paperName; }
  public void setPaperName(String paperName) { this.paperName = paperName; }
  public String getPaperCode() { return paperCode; }
  public void setPaperCode(String paperCode) { this.paperCode = paperCode; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public Integer getQuestionCount() { return questionCount; }
  public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
  public Integer getSnapshotVersion() { return snapshotVersion; }
  public void setSnapshotVersion(Integer snapshotVersion) { this.snapshotVersion = snapshotVersion; }
}
