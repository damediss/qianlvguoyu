package com.guoyu.platform.competition.messaging.vo;

import java.time.LocalDateTime;

public class CompetitionPlayerMessageRow {

  private Long messageId;
  private Long eventId;
  private Long noticeId;
  private String type;
  private String title;
  private String contentType;
  private String content;
  private LocalDateTime publishedAt;
  private LocalDateTime receivedAt;
  private LocalDateTime readAt;

  public Long getMessageId() { return messageId; }
  public void setMessageId(Long messageId) { this.messageId = messageId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getNoticeId() { return noticeId; }
  public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public LocalDateTime getReceivedAt() { return receivedAt; }
  public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
  public LocalDateTime getReadAt() { return readAt; }
  public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
