package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompetitionNoticeVO {

  private Long id;
  private Long eventId;
  private String title;
  private String contentType;
  private String content;
  private List<CompetitionNoticeAssetVO> assets = new ArrayList<>();
  private Integer version;
  private String status;
  private Long publishedBy;
  private LocalDateTime publishedAt;
  private Long createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
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
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
