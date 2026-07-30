package com.guoyu.platform.competition.player.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.guoyu.platform.competition.vo.CompetitionNoticeAssetVO;

public class CompetitionPlayerMessageVO {

  private Long messageId;
  private Long eventId;
  private Long noticeId;
  private String type;
  private String title;
  private String contentType;
  private String content;
  private List<CompetitionNoticeAssetVO> assets = new ArrayList<>();
  private LocalDateTime publishedAt;
  private LocalDateTime receivedAt;
  private LocalDateTime readAt;

  @JsonSerialize(using = ToStringSerializer.class)
  public Long getMessageId() { return messageId; }
  public void setMessageId(Long messageId) { this.messageId = messageId; }
  @JsonSerialize(using = ToStringSerializer.class)
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  @JsonSerialize(using = ToStringSerializer.class)
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
  public List<CompetitionNoticeAssetVO> getAssets() { return assets; }
  public void setAssets(List<CompetitionNoticeAssetVO> assets) {
    this.assets = assets == null ? new ArrayList<>() : new ArrayList<>(assets);
  }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public LocalDateTime getReceivedAt() { return receivedAt; }
  public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
  public LocalDateTime getReadAt() { return readAt; }
  public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
