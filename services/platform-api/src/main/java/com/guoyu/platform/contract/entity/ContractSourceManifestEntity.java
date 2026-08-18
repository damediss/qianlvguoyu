package com.guoyu.platform.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("biz_contract_source_manifest")
public class ContractSourceManifestEntity extends BaseEntity {

  private Long sourceSnapshotId;
  private Integer version;
  private String manifestJson;
  private Integer anchorCount;
  private Integer warningCount;
  private String status;
  private String rendererVersion;

  public Long getSourceSnapshotId() { return sourceSnapshotId; }
  public void setSourceSnapshotId(Long sourceSnapshotId) { this.sourceSnapshotId = sourceSnapshotId; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public String getManifestJson() { return manifestJson; }
  public void setManifestJson(String manifestJson) { this.manifestJson = manifestJson; }
  public Integer getAnchorCount() { return anchorCount; }
  public void setAnchorCount(Integer anchorCount) { this.anchorCount = anchorCount; }
  public Integer getWarningCount() { return warningCount; }
  public void setWarningCount(Integer warningCount) { this.warningCount = warningCount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRendererVersion() { return rendererVersion; }
  public void setRendererVersion(String rendererVersion) { this.rendererVersion = rendererVersion; }
}
