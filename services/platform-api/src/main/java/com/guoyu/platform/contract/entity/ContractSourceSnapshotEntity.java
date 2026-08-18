package com.guoyu.platform.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("biz_contract_source_snapshot")
public class ContractSourceSnapshotEntity extends BaseEntity {

  private String provider;
  private String knowledgeBaseId;
  private String knowledgeId;
  private Long sourceFileId;
  private String originalFilename;
  private String sourceMimeType;
  private String sourceFileHash;
  private Long sourceFileSize;
  private String derivationStatus;
  private String sourceParseStatus;
  private String sourceEnableStatus;

  public String getProvider() { return provider; }
  public void setProvider(String provider) { this.provider = provider; }
  public String getKnowledgeBaseId() { return knowledgeBaseId; }
  public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
  public String getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
  public Long getSourceFileId() { return sourceFileId; }
  public void setSourceFileId(Long sourceFileId) { this.sourceFileId = sourceFileId; }
  public String getOriginalFilename() { return originalFilename; }
  public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
  public String getSourceMimeType() { return sourceMimeType; }
  public void setSourceMimeType(String sourceMimeType) { this.sourceMimeType = sourceMimeType; }
  public String getSourceFileHash() { return sourceFileHash; }
  public void setSourceFileHash(String sourceFileHash) { this.sourceFileHash = sourceFileHash; }
  public Long getSourceFileSize() { return sourceFileSize; }
  public void setSourceFileSize(Long sourceFileSize) { this.sourceFileSize = sourceFileSize; }
  public String getDerivationStatus() { return derivationStatus; }
  public void setDerivationStatus(String derivationStatus) { this.derivationStatus = derivationStatus; }
  public String getSourceParseStatus() { return sourceParseStatus; }
  public void setSourceParseStatus(String sourceParseStatus) { this.sourceParseStatus = sourceParseStatus; }
  public String getSourceEnableStatus() { return sourceEnableStatus; }
  public void setSourceEnableStatus(String sourceEnableStatus) { this.sourceEnableStatus = sourceEnableStatus; }
}
