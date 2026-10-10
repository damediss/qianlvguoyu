package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_theory_answer_save_request")
public class CompetitionTheoryAnswerSaveRequestEntity extends BaseEntity {
  private Long attemptId;
  private Long questionSnapshotId;
  private String requestId;
  private Integer acceptedVersion;
  private String answerHash;
  private LocalDateTime savedAt;

  public Long getAttemptId() { return attemptId; }
  public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
  public Long getQuestionSnapshotId() { return questionSnapshotId; }
  public void setQuestionSnapshotId(Long questionSnapshotId) { this.questionSnapshotId = questionSnapshotId; }
  public String getRequestId() { return requestId; }
  public void setRequestId(String requestId) { this.requestId = requestId; }
  public Integer getAcceptedVersion() { return acceptedVersion; }
  public void setAcceptedVersion(Integer acceptedVersion) { this.acceptedVersion = acceptedVersion; }
  public String getAnswerHash() { return answerHash; }
  public void setAnswerHash(String answerHash) { this.answerHash = answerHash; }
  public LocalDateTime getSavedAt() { return savedAt; }
  public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
}
