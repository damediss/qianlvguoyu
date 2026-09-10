package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_theory_paper_question")
public class CompetitionTheoryPaperQuestionEntity extends BaseEntity {

  private Long paperSnapshotId;
  private Long sourceQuestionId;
  private Integer sortOrder;
  private String questionType;
  private String content;
  private String optionsJson;
  private String correctAnswersJson;
  private String fillAnswer;
  private BigDecimal score;
  private String resolveContent;
  private Integer difficulty;
  private String scoringRuleCode;
  private Integer scoringRuleVersion;
  private Long sourceOrgId;
  private String sourceOrgName;
  private Long sourceAuthorId;
  private LocalDateTime sourceUpdatedAt;

  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public Long getSourceQuestionId() { return sourceQuestionId; }
  public void setSourceQuestionId(Long sourceQuestionId) { this.sourceQuestionId = sourceQuestionId; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getOptionsJson() { return optionsJson; }
  public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
  public String getCorrectAnswersJson() { return correctAnswersJson; }
  public void setCorrectAnswersJson(String correctAnswersJson) {
    this.correctAnswersJson = correctAnswersJson;
  }
  public String getFillAnswer() { return fillAnswer; }
  public void setFillAnswer(String fillAnswer) { this.fillAnswer = fillAnswer; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
  public String getResolveContent() { return resolveContent; }
  public void setResolveContent(String resolveContent) { this.resolveContent = resolveContent; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public String getScoringRuleCode() { return scoringRuleCode; }
  public void setScoringRuleCode(String scoringRuleCode) { this.scoringRuleCode = scoringRuleCode; }
  public Integer getScoringRuleVersion() { return scoringRuleVersion; }
  public void setScoringRuleVersion(Integer scoringRuleVersion) {
    this.scoringRuleVersion = scoringRuleVersion;
  }
  public Long getSourceOrgId() { return sourceOrgId; }
  public void setSourceOrgId(Long sourceOrgId) { this.sourceOrgId = sourceOrgId; }
  public String getSourceOrgName() { return sourceOrgName; }
  public void setSourceOrgName(String sourceOrgName) { this.sourceOrgName = sourceOrgName; }
  public Long getSourceAuthorId() { return sourceAuthorId; }
  public void setSourceAuthorId(Long sourceAuthorId) { this.sourceAuthorId = sourceAuthorId; }
  public LocalDateTime getSourceUpdatedAt() { return sourceUpdatedAt; }
  public void setSourceUpdatedAt(LocalDateTime sourceUpdatedAt) {
    this.sourceUpdatedAt = sourceUpdatedAt;
  }
}
