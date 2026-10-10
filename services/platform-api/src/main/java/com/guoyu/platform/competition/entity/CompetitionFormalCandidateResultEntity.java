package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_formal_candidate_result")
public class CompetitionFormalCandidateResultEntity extends BaseEntity {

  private Long examSessionId;
  private Long candidateExamId;
  private Integer revisionNo;
  private String resultStatus;
  private BigDecimal fullScore;
  private BigDecimal totalScore;
  private Integer questionCount;
  private Integer answeredCount;
  private Integer correctCount;
  private Integer wrongCount;
  private Integer unansweredCount;
  private Integer scoreVersion;
  private Integer currentFlag;
  private String correctionReason;
  private LocalDateTime scoredTime;

  public Long getExamSessionId() { return examSessionId; }
  public void setExamSessionId(Long examSessionId) { this.examSessionId = examSessionId; }
  public Long getCandidateExamId() { return candidateExamId; }
  public void setCandidateExamId(Long candidateExamId) { this.candidateExamId = candidateExamId; }
  public Integer getRevisionNo() { return revisionNo; }
  public void setRevisionNo(Integer revisionNo) { this.revisionNo = revisionNo; }
  public String getResultStatus() { return resultStatus; }
  public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
  public BigDecimal getFullScore() { return fullScore; }
  public void setFullScore(BigDecimal fullScore) { this.fullScore = fullScore; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public Integer getQuestionCount() { return questionCount; }
  public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
  public Integer getAnsweredCount() { return answeredCount; }
  public void setAnsweredCount(Integer answeredCount) { this.answeredCount = answeredCount; }
  public Integer getCorrectCount() { return correctCount; }
  public void setCorrectCount(Integer correctCount) { this.correctCount = correctCount; }
  public Integer getWrongCount() { return wrongCount; }
  public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
  public Integer getUnansweredCount() { return unansweredCount; }
  public void setUnansweredCount(Integer unansweredCount) { this.unansweredCount = unansweredCount; }
  public Integer getScoreVersion() { return scoreVersion; }
  public void setScoreVersion(Integer scoreVersion) { this.scoreVersion = scoreVersion; }
  public Integer getCurrentFlag() { return currentFlag; }
  public void setCurrentFlag(Integer currentFlag) { this.currentFlag = currentFlag; }
  public String getCorrectionReason() { return correctionReason; }
  public void setCorrectionReason(String correctionReason) { this.correctionReason = correctionReason; }
  public LocalDateTime getScoredTime() { return scoredTime; }
  public void setScoredTime(LocalDateTime scoredTime) { this.scoredTime = scoredTime; }
}
