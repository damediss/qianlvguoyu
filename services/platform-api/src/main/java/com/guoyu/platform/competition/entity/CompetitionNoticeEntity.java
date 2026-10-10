package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_notice")
public class CompetitionNoticeEntity extends BaseEntity {

  private Long eventId;
  private String title;
  private String contentType;
  private String content;
  private Integer version;
  private String status;
  private Long publishedBy;
  private LocalDateTime publishedAt;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
