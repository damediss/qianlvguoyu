package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_formal_exam_question_snapshot")
public class CompetitionFormalExamQuestionSnapshotEntity extends BaseEntity {

  private Long examSessionId;
  private Long paperSnapshotId;
  private Long sourceQuestionId;
  private Long sourcePaperQuestionId;
  private Integer questionOrder;
  private String questionType;
  private String stem;
  private BigDecimal score;
  private String optionsJson;
  private String correctAnswersJson;
  private String fillAnswer;
  private String scoringRuleCode;
  private Integer scoringRuleVersion;

  public Long getExamSessionId() { return examSessionId; }
  public void setExamSessionId(Long examSessionId) { this.examSessionId = examSessionId; }
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public Long getSourceQuestionId() { return sourceQuestionId; }
  public void setSourceQuestionId(Long sourceQuestionId) { this.sourceQuestionId = sourceQuestionId; }
  public Long getSourcePaperQuestionId() { return sourcePaperQuestionId; }
  public void setSourcePaperQuestionId(Long sourcePaperQuestionId) {
    this.sourcePaperQuestionId = sourcePaperQuestionId;
  }
  public Integer getQuestionOrder() { return questionOrder; }
  public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public String getStem() { return stem; }
  public void setStem(String stem) { this.stem = stem; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
  public String getOptionsJson() { return optionsJson; }
  public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
  public String getCorrectAnswersJson() { return correctAnswersJson; }
  public void setCorrectAnswersJson(String correctAnswersJson) {
    this.correctAnswersJson = correctAnswersJson;
  }
  public String getFillAnswer() { return fillAnswer; }
  public void setFillAnswer(String fillAnswer) { this.fillAnswer = fillAnswer; }
  public String getScoringRuleCode() { return scoringRuleCode; }
  public void setScoringRuleCode(String scoringRuleCode) { this.scoringRuleCode = scoringRuleCode; }
  public Integer getScoringRuleVersion() { return scoringRuleVersion; }
  public void setScoringRuleVersion(Integer scoringRuleVersion) {
    this.scoringRuleVersion = scoringRuleVersion;
  }
}
