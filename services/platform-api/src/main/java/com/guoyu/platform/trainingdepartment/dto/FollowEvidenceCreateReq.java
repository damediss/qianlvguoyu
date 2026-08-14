package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

/** 创建课堂影像（文件已上传到 /files） */
public class FollowEvidenceCreateReq {

  private Long scheduleId;
  private String bizDate;
  private String teachFormOverride;
  private String classHoursOverride;
  /** photo / video，默认 photo */
  private String mediaType;
  /** 视频时长（秒）；视频必填，目标约 15 */
  private Integer durationSeconds;
  private Long fileId;
  @NotBlank
  private String filePath;
  private String thumbPath;
  private String caption;
  private Integer sortNo;

  public Long getScheduleId() { return scheduleId; }
  public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
  public String getBizDate() { return bizDate; }
  public void setBizDate(String bizDate) { this.bizDate = bizDate; }
  public String getTeachFormOverride() { return teachFormOverride; }
  public void setTeachFormOverride(String teachFormOverride) { this.teachFormOverride = teachFormOverride; }
  public String getClassHoursOverride() { return classHoursOverride; }
  public void setClassHoursOverride(String classHoursOverride) { this.classHoursOverride = classHoursOverride; }
  public String getMediaType() { return mediaType; }
  public void setMediaType(String mediaType) { this.mediaType = mediaType; }
  public Integer getDurationSeconds() { return durationSeconds; }
  public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
  public Long getFileId() { return fileId; }
  public void setFileId(Long fileId) { this.fileId = fileId; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public String getThumbPath() { return thumbPath; }
  public void setThumbPath(String thumbPath) { this.thumbPath = thumbPath; }
  public String getCaption() { return caption; }
  public void setCaption(String caption) { this.caption = caption; }
  public Integer getSortNo() { return sortNo; }
  public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
}
