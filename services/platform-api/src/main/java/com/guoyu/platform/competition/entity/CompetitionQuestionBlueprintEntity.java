package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

/** 竞赛理论/实操细目表的赛事级版本。 */
@TableName("competition_question_blueprint")
public class CompetitionQuestionBlueprintEntity extends BaseEntity {

  private Long eventId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String contentType;
  private Integer versionNo;
  private String title;
  private String sourceFileName;
  private Long sourceFileId;
  private Integer pointCount;
  private Integer topicCount;
  private String xyzRatio;
  private Integer weightCheck;
  private String checkReport;
  private String status;
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public Integer getVersionNo() { return versionNo; }
  public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSourceFileName() { return sourceFileName; }
  public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
  public Long getSourceFileId() { return sourceFileId; }
  public void setSourceFileId(Long sourceFileId) { this.sourceFileId = sourceFileId; }
  public Integer getPointCount() { return pointCount; }
  public void setPointCount(Integer pointCount) { this.pointCount = pointCount; }
  public Integer getTopicCount() { return topicCount; }
  public void setTopicCount(Integer topicCount) { this.topicCount = topicCount; }
  public String getXyzRatio() { return xyzRatio; }
  public void setXyzRatio(String xyzRatio) { this.xyzRatio = xyzRatio; }
  public Integer getWeightCheck() { return weightCheck; }
  public void setWeightCheck(Integer weightCheck) { this.weightCheck = weightCheck; }
  public String getCheckReport() { return checkReport; }
  public void setCheckReport(String checkReport) { this.checkReport = checkReport; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
