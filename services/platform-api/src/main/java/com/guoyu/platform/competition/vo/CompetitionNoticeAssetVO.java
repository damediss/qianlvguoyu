package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class CompetitionNoticeAssetVO {
  private Long assetId;
  private String assetType;
  private String originalName;
  private String contentType;
  private Long sizeBytes;
  private String sourceUri;

  @JsonSerialize(using = ToStringSerializer.class)
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
  public String getSourceUri() { return sourceUri; }
  public void setSourceUri(String sourceUri) { this.sourceUri = sourceUri; }
}
