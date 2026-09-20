package com.guoyu.platform.competition.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionQuestionQueryReq extends PageQuery {

  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String contentType;
  private String questionType;
  private String keyword;
  private Integer status;
  private Long blueprintId;
  private Long blueprintNodeId;
  private String bindingStatus;

  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Long getBlueprintId() { return blueprintId; }
  public void setBlueprintId(Long blueprintId) { this.blueprintId = blueprintId; }
  public Long getBlueprintNodeId() { return blueprintNodeId; }
  public void setBlueprintNodeId(Long blueprintNodeId) { this.blueprintNodeId = blueprintNodeId; }
  public String getBindingStatus() { return bindingStatus; }
  public void setBindingStatus(String bindingStatus) { this.bindingStatus = bindingStatus; }
}
