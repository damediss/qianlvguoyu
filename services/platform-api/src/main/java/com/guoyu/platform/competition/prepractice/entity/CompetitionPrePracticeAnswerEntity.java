package com.guoyu.platform.competition.prepractice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_pre_practice_answer")
public class CompetitionPrePracticeAnswerEntity extends BaseEntity {

  private Long deliveryId;
  private Long taskId;
  private Long taskQuestionId;
  private Long playerId;
  private String answerJson;
  private LocalDateTime answeredAt;

  public Long getDeliveryId() { return deliveryId; }
  public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }
  public Long getTaskId() { return taskId; }
  public void setTaskId(Long taskId) { this.taskId = taskId; }
  public Long getTaskQuestionId() { return taskQuestionId; }
  public void setTaskQuestionId(Long taskQuestionId) { this.taskQuestionId = taskQuestionId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public String getAnswerJson() { return answerJson; }
  public void setAnswerJson(String answerJson) { this.answerJson = answerJson; }
  public LocalDateTime getAnsweredAt() { return answeredAt; }
  public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}
