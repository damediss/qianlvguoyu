package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_stage_import_item")
public class CompetitionStageImportItemEntity extends BaseEntity {
  private Long batchId;
  private Integer rowNo;
  private Long contestantMemberId;
  private String contentType;
  private Long sessionId;
  private String validationStatus;
  private String errorMessage;
  private String rawPayload;

  public Long getBatchId() { return batchId; }
  public void setBatchId(Long value) { batchId = value; }
  public Integer getRowNo() { return rowNo; }
  public void setRowNo(Integer value) { rowNo = value; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long value) { contestantMemberId = value; }
  public String getContentType() { return contentType; }
  public void setContentType(String value) { contentType = value; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long value) { sessionId = value; }
  public String getValidationStatus() { return validationStatus; }
  public void setValidationStatus(String value) { validationStatus = value; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String value) { errorMessage = value; }
  public String getRawPayload() { return rawPayload; }
  public void setRawPayload(String value) { rawPayload = value; }
}
