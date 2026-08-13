package com.guoyu.platform.exam.standard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

/**
 * 职业技能标准解析任务（staging），对应 exam_standard_task 表。
 */
@TableName("exam_standard_task")
public class ExamStandardTaskEntity extends BaseEntity {

  private String workTypeTitle;
  private String seriesNo;
  private String version;
  private Long fileId;
  private String fileHash;
  /** uploaded/parsed/llm_running/reviewing/confirmed/exported */
  private String status;
  private String directionsJson;
  private String levelsJson;
  /** 双权重表 JSON（导出/权重校验用） */
  private String weightsJson;
  private String parseWarnings;
  private String llmModelId;
  private LocalDateTime llmFinishedAt;

  public String getWorkTypeTitle() {
    return workTypeTitle;
  }

  public void setWorkTypeTitle(String workTypeTitle) {
    this.workTypeTitle = workTypeTitle;
  }

  public String getSeriesNo() {
    return seriesNo;
  }

  public void setSeriesNo(String seriesNo) {
    this.seriesNo = seriesNo;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Long getFileId() {
    return fileId;
  }

  public void setFileId(Long fileId) {
    this.fileId = fileId;
  }

  public String getFileHash() {
    return fileHash;
  }

  public void setFileHash(String fileHash) {
    this.fileHash = fileHash;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDirectionsJson() {
    return directionsJson;
  }

  public void setDirectionsJson(String directionsJson) {
    this.directionsJson = directionsJson;
  }

  public String getLevelsJson() {
    return levelsJson;
  }

  public void setLevelsJson(String levelsJson) {
    this.levelsJson = levelsJson;
  }

  public String getWeightsJson() {
    return weightsJson;
  }

  public void setWeightsJson(String weightsJson) {
    this.weightsJson = weightsJson;
  }

  public String getParseWarnings() {
    return parseWarnings;
  }

  public void setParseWarnings(String parseWarnings) {
    this.parseWarnings = parseWarnings;
  }

  public String getLlmModelId() {
    return llmModelId;
  }

  public void setLlmModelId(String llmModelId) {
    this.llmModelId = llmModelId;
  }

  public LocalDateTime getLlmFinishedAt() {
    return llmFinishedAt;
  }

  public void setLlmFinishedAt(LocalDateTime llmFinishedAt) {
    this.llmFinishedAt = llmFinishedAt;
  }
}
