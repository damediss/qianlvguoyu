package com.guoyu.platform.competition.messaging.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_player_message")
public class CompetitionPlayerMessageEntity extends BaseEntity {

  private Long playerId;
  private Long eventId;
  private Long noticeId;
  private Long scheduleVersionId;
  private String type;
  private String title;
  private String content;
  private LocalDateTime publishedAt;
  private LocalDateTime receivedAt;
  private LocalDateTime readAt;

  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getNoticeId() { return noticeId; }
  public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }
  public Long getScheduleVersionId() { return scheduleVersionId; }
  public void setScheduleVersionId(Long scheduleVersionId) {
    this.scheduleVersionId = scheduleVersionId;
  }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public LocalDateTime getReceivedAt() { return receivedAt; }
  public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
  public LocalDateTime getReadAt() { return readAt; }
  public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
