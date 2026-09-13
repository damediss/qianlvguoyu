package com.guoyu.platform.exam.knowledgefix.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** exam_exam_detail 知识修正清单（工作底表+审计，方案 §7.2） */
@TableName("exam_detail_knowledge_fix")
public class ExamDetailKnowledgeFixEntity {

  public static final String STATUS_PENDING = "pending";
  public static final String STATUS_APPLY = "apply";
  public static final String STATUS_KEEP = "keep";
  public static final String STATUS_FLAG = "flag";
  public static final String STATUS_APPLIED = "applied";

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Integer detailId;
  private Integer oldKnowledgeId;
  private Integer courseId;
  private Integer newKnowledgeId;
  private BigDecimal confidence;
  private String basis;
  private String llmReason;
  private String status;
  private Long batchNo;
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
  public Integer getDetailId() { return detailId; }
  public void setDetailId(Integer detailId) { this.detailId = detailId; }
  public Integer getOldKnowledgeId() { return oldKnowledgeId; }
  public void setOldKnowledgeId(Integer oldKnowledgeId) { this.oldKnowledgeId = oldKnowledgeId; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public Integer getNewKnowledgeId() { return newKnowledgeId; }
  public void setNewKnowledgeId(Integer newKnowledgeId) { this.newKnowledgeId = newKnowledgeId; }
  public BigDecimal getConfidence() { return confidence; }
  public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
  public String getBasis() { return basis; }
  public void setBasis(String basis) { this.basis = basis; }
  public String getLlmReason() { return llmReason; }
  public void setLlmReason(String llmReason) { this.llmReason = llmReason; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getBatchNo() { return batchNo; }
  public void setBatchNo(Long batchNo) { this.batchNo = batchNo; }
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
