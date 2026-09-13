package com.guoyu.platform.exam.knowledgefix.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** exam_exam_detail 去重清单（工作底表+审计，方案 §7.2） */
@TableName("exam_detail_dedup_list")
public class ExamDetailDedupListEntity {

  public static final String STATUS_PENDING = "pending";
  public static final String STATUS_MANUAL_REVIEW = "manual_review";
  public static final String STATUS_APPROVED = "approved";
  public static final String STATUS_DONE = "done";
  public static final String STATUS_ROLLED_BACK = "rolled_back";

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Integer detailId;
  private String groupKey;
  private Integer isKeeper;
  private Integer keeperId;
  private String createWave;
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
  public Integer getDetailId() { return detailId; }
  public void setDetailId(Integer detailId) { this.detailId = detailId; }
  public String getGroupKey() { return groupKey; }
  public void setGroupKey(String groupKey) { this.groupKey = groupKey; }
  public Integer getIsKeeper() { return isKeeper; }
  public void setIsKeeper(Integer isKeeper) { this.isKeeper = isKeeper; }
  public Integer getKeeperId() { return keeperId; }
  public void setKeeperId(Integer keeperId) { this.keeperId = keeperId; }
  public String getCreateWave() { return createWave; }
  public void setCreateWave(String createWave) { this.createWave = createWave; }
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
