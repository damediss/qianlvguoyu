package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_notice_asset")
public class CompetitionNoticeAssetEntity extends BaseEntity {

  private Long noticeId;
  private Long fileId;
  private String assetType;
  private Integer sortOrder;

  public Long getNoticeId() { return noticeId; }
  public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }
  public Long getFileId() { return fileId; }
  public void setFileId(Long fileId) { this.fileId = fileId; }
  public String getAssetType() { return assetType; }
  public void setAssetType(String assetType) { this.assetType = assetType; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
