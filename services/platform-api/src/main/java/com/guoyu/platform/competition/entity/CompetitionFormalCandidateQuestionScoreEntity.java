package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_formal_candidate_question_score")
public class CompetitionFormalCandidateQuestionScoreEntity extends BaseEntity {

  private Long candidateResultId;
  private Long examSessionId;
  private Long candidateExamId;
  private Long questionSnapshotId;
  private Integer questionOrder;
  private String questionType;
  private Integer answerVersion;
  private Integer isAnswered;
  private Integer isCorrect;
  private BigDecimal maxScore;
  private BigDecimal awardedScore;
  private Integer scoringRuleVersion;
  private LocalDateTime scoredTime;

  public Long getCandidateResultId() { return candidateResultId; }
  public void setCandidateResultId(Long candidateResultId) { this.candidateResultId = candidateResultId; }
  public Long getExamSessionId() { return examSessionId; }
  public void setExamSessionId(Long examSessionId) { this.examSessionId = examSessionId; }
  public Long getCandidateExamId() { return candidateExamId; }
  public void setCandidateExamId(Long candidateExamId) { this.candidateExamId = candidateExamId; }
  public Long getQuestionSnapshotId() { return questionSnapshotId; }
  public void setQuestionSnapshotId(Long questionSnapshotId) { this.questionSnapshotId = questionSnapshotId; }
  public Integer getQuestionOrder() { return questionOrder; }
  public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public Integer getAnswerVersion() { return answerVersion; }
  public void setAnswerVersion(Integer answerVersion) { this.answerVersion = answerVersion; }
  public Integer getIsAnswered() { return isAnswered; }
  public void setIsAnswered(Integer isAnswered) { this.isAnswered = isAnswered; }
  public Integer getIsCorrect() { return isCorrect; }
  public void setIsCorrect(Integer isCorrect) { this.isCorrect = isCorrect; }
  public BigDecimal getMaxScore() { return maxScore; }
  public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
  public BigDecimal getAwardedScore() { return awardedScore; }
  public void setAwardedScore(BigDecimal awardedScore) { this.awardedScore = awardedScore; }
  public Integer getScoringRuleVersion() { return scoringRuleVersion; }
  public void setScoringRuleVersion(Integer scoringRuleVersion) { this.scoringRuleVersion = scoringRuleVersion; }
  public LocalDateTime getScoredTime() { return scoredTime; }
  public void setScoredTime(LocalDateTime scoredTime) { this.scoredTime = scoredTime; }
}
