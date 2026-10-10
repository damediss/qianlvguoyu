package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_formal_candidate_answer")
public class CompetitionFormalCandidateAnswerEntity extends BaseEntity {

  private Long examSessionId;
  private Long candidateExamId;
  private Long questionSnapshotId;
  private String answerJson;
  private Integer isAnswered;
  private Integer isMarked;
  private Integer answerVersion;
  private LocalDateTime firstAnswerTime;
  private LocalDateTime lastAnswerTime;

  public Long getExamSessionId() { return examSessionId; }
  public void setExamSessionId(Long examSessionId) { this.examSessionId = examSessionId; }
  public Long getCandidateExamId() { return candidateExamId; }
  public void setCandidateExamId(Long candidateExamId) { this.candidateExamId = candidateExamId; }
  public Long getQuestionSnapshotId() { return questionSnapshotId; }
  public void setQuestionSnapshotId(Long questionSnapshotId) {
    this.questionSnapshotId = questionSnapshotId;
  }
  public String getAnswerJson() { return answerJson; }
  public void setAnswerJson(String answerJson) { this.answerJson = answerJson; }
  public Integer getIsAnswered() { return isAnswered; }
  public void setIsAnswered(Integer isAnswered) { this.isAnswered = isAnswered; }
  public Integer getIsMarked() { return isMarked; }
  public void setIsMarked(Integer isMarked) { this.isMarked = isMarked; }
  public Integer getAnswerVersion() { return answerVersion; }
  public void setAnswerVersion(Integer answerVersion) { this.answerVersion = answerVersion; }
  public LocalDateTime getFirstAnswerTime() { return firstAnswerTime; }
  public void setFirstAnswerTime(LocalDateTime firstAnswerTime) {
    this.firstAnswerTime = firstAnswerTime;
  }
  public LocalDateTime getLastAnswerTime() { return lastAnswerTime; }
  public void setLastAnswerTime(LocalDateTime lastAnswerTime) {
    this.lastAnswerTime = lastAnswerTime;
  }
}
