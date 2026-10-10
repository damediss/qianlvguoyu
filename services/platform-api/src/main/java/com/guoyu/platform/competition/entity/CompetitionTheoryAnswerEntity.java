package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_theory_answer")
public class CompetitionTheoryAnswerEntity extends BaseEntity {

  private Long attemptId;
  private Long paperSnapshotId;
  private Long questionSnapshotId;
  private String answerJson;
  private Integer markedForReview;
  private Integer saveVersion;
  private String lastRequestId;
  private LocalDateTime lastSavedAt;
  private BigDecimal questionScore;
  private BigDecimal awardedScore;
  private Integer correctFlag;
  private String scoringRuleCode;
  private Integer scoringRuleVersion;
  private LocalDateTime scoredAt;

  public Long getAttemptId() { return attemptId; }
  public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public Long getQuestionSnapshotId() { return questionSnapshotId; }
  public void setQuestionSnapshotId(Long questionSnapshotId) { this.questionSnapshotId = questionSnapshotId; }
  public String getAnswerJson() { return answerJson; }
  public void setAnswerJson(String answerJson) { this.answerJson = answerJson; }
  public Integer getMarkedForReview() { return markedForReview; }
  public void setMarkedForReview(Integer markedForReview) { this.markedForReview = markedForReview; }
  public Integer getSaveVersion() { return saveVersion; }
  public void setSaveVersion(Integer saveVersion) { this.saveVersion = saveVersion; }
  public String getLastRequestId() { return lastRequestId; }
  public void setLastRequestId(String lastRequestId) { this.lastRequestId = lastRequestId; }
  public LocalDateTime getLastSavedAt() { return lastSavedAt; }
  public void setLastSavedAt(LocalDateTime lastSavedAt) { this.lastSavedAt = lastSavedAt; }
  public BigDecimal getQuestionScore() { return questionScore; }
  public void setQuestionScore(BigDecimal questionScore) { this.questionScore = questionScore; }
  public BigDecimal getAwardedScore() { return awardedScore; }
  public void setAwardedScore(BigDecimal awardedScore) { this.awardedScore = awardedScore; }
  public Integer getCorrectFlag() { return correctFlag; }
  public void setCorrectFlag(Integer correctFlag) { this.correctFlag = correctFlag; }
  public String getScoringRuleCode() { return scoringRuleCode; }
  public void setScoringRuleCode(String scoringRuleCode) { this.scoringRuleCode = scoringRuleCode; }
  public Integer getScoringRuleVersion() { return scoringRuleVersion; }
  public void setScoringRuleVersion(Integer scoringRuleVersion) { this.scoringRuleVersion = scoringRuleVersion; }
  public LocalDateTime getScoredAt() { return scoredAt; }
  public void setScoredAt(LocalDateTime scoredAt) { this.scoredAt = scoredAt; }
}
