package com.guoyu.platform.competition.vo;

public class CompetitionNoticeAssetRow {
  private Long noticeId;
  private Long assetId;
  private String assetType;
  private String originalName;
  private String contentType;
  private Long sizeBytes;

  public Long getNoticeId() { return noticeId; }
  public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }
  public Long getAssetId() { return assetId; }
  public void setAssetId(Long assetId) { this.assetId = assetId; }
  public String getAssetType() { return assetType; }
  public void setAssetType(String assetType) { this.assetType = assetType; }
  public String getOriginalName() { return originalName; }
  public void setOriginalName(String originalName) { this.originalName = originalName; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public Long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
}
