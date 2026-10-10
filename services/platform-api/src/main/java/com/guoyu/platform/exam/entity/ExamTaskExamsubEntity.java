package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("task_examsub")
public class ExamTaskExamsubEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer workTypeId;
  private Integer level;
  private Integer nums;
  private Integer status;
  private Integer expertStart;
  private Integer expertEnd;
  private String filePath;
  /** 创建时间（Unix 秒）。task_* 族与 PHP 老系统共用，addtime 列为 int 时间戳口径 */
  private Integer addtime;
  private Integer tenantId;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  private LocalDateTime createTime;
  private LocalDateTime updatedAt;
  private Integer createdBy;
  private Integer updatedBy;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
  public Integer getNums() { return nums; }
  public void setNums(Integer nums) { this.nums = nums; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getExpertStart() { return expertStart; }
  public void setExpertStart(Integer expertStart) { this.expertStart = expertStart; }
  public Integer getExpertEnd() { return expertEnd; }
  public void setExpertEnd(Integer expertEnd) { this.expertEnd = expertEnd; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public Integer getAddtime() { return addtime; }
  public void setAddtime(Integer addtime) { this.addtime = addtime; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public Integer getCreatedBy() { return createdBy; }
  public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
  public Integer getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }
}
