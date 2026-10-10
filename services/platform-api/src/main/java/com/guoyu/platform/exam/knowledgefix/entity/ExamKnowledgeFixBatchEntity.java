package com.guoyu.platform.exam.knowledgefix.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 知识归属修正 LLM 分类批次（方案 §7.2） */
@TableName("exam_knowledge_fix_batch")
public class ExamKnowledgeFixBatchEntity {

  public static final String STATUS_PENDING = "pending";
  public static final String STATUS_RUNNING = "running";
  public static final String STATUS_DONE = "done";
  public static final String STATUS_FAILED = "failed";
  /** 人工暂停（FIX-10/v1.7）：停派发新行，已派发行跑完后收尾；剩余行保持 pending，可再次提交接着跑 */
  public static final String STATUS_PAUSED = "paused";
  /** 暂停过渡态（v0.8 §10 加固）：暂停已登记（内存 + 批次表双判），在飞行行未收尾；reset/重复 submit 均拒绝 */
  public static final String STATUS_PAUSING = "pausing";

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Long batchNo;
  private Integer total;
  private Integer done;
  private Integer failed;
  private Integer applyCnt;
  private Integer keepCnt;
  private Integer flagCnt;
  private BigDecimal selfConsistencyRate;
  /** Jev 通道分类行数（apply/keep/flag/jev-l2-direct，§8.5） */
  private Integer jevClassified;
  /** 走 LLM 全流程兜底行数（含通道关闭/未过双门/Jev 失败降级） */
  private Integer llmFallback;
  /** Jev 行平均置信度（信息项，不卡门禁） */
  private BigDecimal jevAvgConfidence;
  private String status;
  private String failReason;
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
  public Long getBatchNo() { return batchNo; }
  public void setBatchNo(Long batchNo) { this.batchNo = batchNo; }
  public Integer getTotal() { return total; }
  public void setTotal(Integer total) { this.total = total; }
  public Integer getDone() { return done; }
  public void setDone(Integer done) { this.done = done; }
  public Integer getFailed() { return failed; }
  public void setFailed(Integer failed) { this.failed = failed; }
  public Integer getApplyCnt() { return applyCnt; }
  public void setApplyCnt(Integer applyCnt) { this.applyCnt = applyCnt; }
  public Integer getKeepCnt() { return keepCnt; }
  public void setKeepCnt(Integer keepCnt) { this.keepCnt = keepCnt; }
  public Integer getFlagCnt() { return flagCnt; }
  public void setFlagCnt(Integer flagCnt) { this.flagCnt = flagCnt; }
  public BigDecimal getSelfConsistencyRate() { return selfConsistencyRate; }
  public void setSelfConsistencyRate(BigDecimal selfConsistencyRate) { this.selfConsistencyRate = selfConsistencyRate; }
  public Integer getJevClassified() { return jevClassified; }
  public void setJevClassified(Integer jevClassified) { this.jevClassified = jevClassified; }
  public Integer getLlmFallback() { return llmFallback; }
  public void setLlmFallback(Integer llmFallback) { this.llmFallback = llmFallback; }
  public BigDecimal getJevAvgConfidence() { return jevAvgConfidence; }
  public void setJevAvgConfidence(BigDecimal jevAvgConfidence) { this.jevAvgConfidence = jevAvgConfidence; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getFailReason() { return failReason; }
  public void setFailReason(String failReason) { this.failReason = failReason; }
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
