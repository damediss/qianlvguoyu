package com.guoyu.platform.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("kb_chunk")
public class KnowledgeChunkEntity extends BaseEntity {

  private Long spaceId;
  private Long documentId;
  private Integer chunkNo;
  private String heading;
  private Integer pageNo;
  private String content;
  private Integer tokenCount;
  private String embeddingProvider;
  private String embeddingModel;
  private String vectorStatus;
  private String vectorId;
  private String metadataJson;

  public Long getSpaceId() {
    return spaceId;
  }

  public void setSpaceId(Long spaceId) {
    this.spaceId = spaceId;
  }

  public Long getDocumentId() {
    return documentId;
  }

  public void setDocumentId(Long documentId) {
    this.documentId = documentId;
  }

  public Integer getChunkNo() {
    return chunkNo;
  }

  public void setChunkNo(Integer chunkNo) {
    this.chunkNo = chunkNo;
  }

  public String getHeading() {
    return heading;
  }

  public void setHeading(String heading) {
    this.heading = heading;
  }

  public Integer getPageNo() {
    return pageNo;
  }

  public void setPageNo(Integer pageNo) {
    this.pageNo = pageNo;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Integer getTokenCount() {
    return tokenCount;
  }

  public void setTokenCount(Integer tokenCount) {
    this.tokenCount = tokenCount;
  }

  public String getEmbeddingProvider() {
    return embeddingProvider;
  }

  public void setEmbeddingProvider(String embeddingProvider) {
    this.embeddingProvider = embeddingProvider;
  }

  public String getEmbeddingModel() {
    return embeddingModel;
  }

  public void setEmbeddingModel(String embeddingModel) {
    this.embeddingModel = embeddingModel;
  }

  public String getVectorStatus() {
    return vectorStatus;
  }

  public void setVectorStatus(String vectorStatus) {
    this.vectorStatus = vectorStatus;
  }

  public String getVectorId() {
    return vectorId;
  }

  public void setVectorId(String vectorId) {
    this.vectorId = vectorId;
  }

  public String getMetadataJson() {
    return metadataJson;
  }

  public void setMetadataJson(String metadataJson) {
    this.metadataJson = metadataJson;
  }
}
