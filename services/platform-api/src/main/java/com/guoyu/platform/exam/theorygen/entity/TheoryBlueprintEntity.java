package com.guoyu.platform.exam.theorygen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 理论知识考评要素细目表批次 exam_theory_blueprint */
@TableName("exam_theory_blueprint")
public class TheoryBlueprintEntity {

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Integer workTypeId;
  private Integer level;
  private Integer courseId;
  private String title;
  private String sourceFileName;
  private Long sourceFileId;
  private Integer pointCount;
  private String xyzRatio;
  private Integer weightCheck;
  private String checkReport;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long createdBy;
  private Long updatedBy;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSourceFileName() { return sourceFileName; }
  public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
  public Long getSourceFileId() { return sourceFileId; }
  public void setSourceFileId(Long sourceFileId) { this.sourceFileId = sourceFileId; }
  public Integer getPointCount() { return pointCount; }
  public void setPointCount(Integer pointCount) { this.pointCount = pointCount; }
  public String getXyzRatio() { return xyzRatio; }
  public void setXyzRatio(String xyzRatio) { this.xyzRatio = xyzRatio; }
  public Integer getWeightCheck() { return weightCheck; }
  public void setWeightCheck(Integer weightCheck) { this.weightCheck = weightCheck; }
  public String getCheckReport() { return checkReport; }
  public void setCheckReport(String checkReport) { this.checkReport = checkReport; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
