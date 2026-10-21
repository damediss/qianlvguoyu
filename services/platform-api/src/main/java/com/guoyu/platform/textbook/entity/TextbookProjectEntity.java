package com.guoyu.platform.textbook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

/**
 * 教材工程（textbook_project）：工种 × 方向 × 等级 三元组分册。
 */
@TableName("textbook_project")
public class TextbookProjectEntity extends BaseEntity {

  private String workTypeTitle;
  private String seriesNo;
  private String standardVersion;
  private Long fileId;
  private String fileHash;
  private String direction;
  /** 5=五级/初级工 … 1=一级/高级技师（API 与库同口径） */
  private Integer levelCode;
  /** 方向→职业功能序号 JSON 数组，如 [1,2,12] */
  private String functionNosJson;
  private Integer trainingHours;
  private Integer wordsPerHour;
  private Integer totalWords;
  /** parsed/outlined/reviewed（generating/done 二期预留） */
  private String status;
  /** 解析告警 JSON 数组 */
  private String parseWarnings;

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

  public String getStandardVersion() {
    return standardVersion;
  }

  public void setStandardVersion(String standardVersion) {
    this.standardVersion = standardVersion;
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

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public Integer getLevelCode() {
    return levelCode;
  }

  public void setLevelCode(Integer levelCode) {
    this.levelCode = levelCode;
  }

  public String getFunctionNosJson() {
    return functionNosJson;
  }

  public void setFunctionNosJson(String functionNosJson) {
    this.functionNosJson = functionNosJson;
  }

  public Integer getTrainingHours() {
    return trainingHours;
  }

  public void setTrainingHours(Integer trainingHours) {
    this.trainingHours = trainingHours;
  }

  public Integer getWordsPerHour() {
    return wordsPerHour;
  }

  public void setWordsPerHour(Integer wordsPerHour) {
    this.wordsPerHour = wordsPerHour;
  }

  public Integer getTotalWords() {
    return totalWords;
  }

  public void setTotalWords(Integer totalWords) {
    this.totalWords = totalWords;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getParseWarnings() {
    return parseWarnings;
  }

  public void setParseWarnings(String parseWarnings) {
    this.parseWarnings = parseWarnings;
  }
}
