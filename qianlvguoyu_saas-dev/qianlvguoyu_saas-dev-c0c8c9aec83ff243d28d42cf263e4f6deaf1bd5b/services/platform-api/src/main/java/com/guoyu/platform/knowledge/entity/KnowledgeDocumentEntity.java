package com.guoyu.platform.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("kb_document")
public class KnowledgeDocumentEntity extends BaseEntity {

  private Long spaceId;
  private Long fileId;
  private String title;
  private String categoryCode;
  private String sourceType;
  private String accessScope;
  private String sensitiveLevel;
  private String parserStatus;
  private String indexStatus;
  private Integer chunkCount;
  private String textSha256;
  private String errorMessage;
  private String status;

  public Long getSpaceId() {
    return spaceId;
  }

  public void setSpaceId(Long spaceId) {
    this.spaceId = spaceId;
  }

  public Long getFileId() {
    return fileId;
  }

  public void setFileId(Long fileId) {
    this.fileId = fileId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCategoryCode() {
    return categoryCode;
  }

  public void setCategoryCode(String categoryCode) {
    this.categoryCode = categoryCode;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public String getAccessScope() {
    return accessScope;
  }

  public void setAccessScope(String accessScope) {
    this.accessScope = accessScope;
  }

  public String getSensitiveLevel() {
    return sensitiveLevel;
  }

  public void setSensitiveLevel(String sensitiveLevel) {
    this.sensitiveLevel = sensitiveLevel;
  }

  public String getParserStatus() {
    return parserStatus;
  }

  public void setParserStatus(String parserStatus) {
    this.parserStatus = parserStatus;
  }

  public String getIndexStatus() {
    return indexStatus;
  }

  public void setIndexStatus(String indexStatus) {
    this.indexStatus = indexStatus;
  }

  public Integer getChunkCount() {
    return chunkCount;
  }

  public void setChunkCount(Integer chunkCount) {
    this.chunkCount = chunkCount;
  }

  public String getTextSha256() {
    return textSha256;
  }

  public void setTextSha256(String textSha256) {
    this.textSha256 = textSha256;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
