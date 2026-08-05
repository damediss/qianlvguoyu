package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public class CompetitionStageContentVO {

  private Long id;
  private Long eventId;
  private Long stageId;
  private String contentType;
  private String status;
  private BigDecimal weight;
  private Integer sortOrder;
  private String providerCode;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public BigDecimal getWeight() { return weight; }
  public void setWeight(BigDecimal weight) { this.weight = weight; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public String getProviderCode() { return providerCode; }
  public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
}
