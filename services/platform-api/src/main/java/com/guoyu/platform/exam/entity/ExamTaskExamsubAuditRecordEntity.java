package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("task_examsub_audit_records")
public class ExamTaskExamsubAuditRecordEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer expertId;
  private Integer auditRole;
  private Integer taskId;
  private Integer examId;
  private String content;
  private String answer;
  private String pic;
  private Integer auditStatus;
  private String ymd;
  /** 提交时间（Unix 秒）。task_* 族与 PHP 老系统共用，addtime 列为 int 时间戳口径 */
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
  public Integer getExpertId() { return expertId; }
  public void setExpertId(Integer expertId) { this.expertId = expertId; }
  public Integer getAuditRole() { return auditRole; }
  public void setAuditRole(Integer auditRole) { this.auditRole = auditRole; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public Integer getExamId() { return examId; }
  public void setExamId(Integer examId) { this.examId = examId; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getPic() { return pic; }
  public void setPic(String pic) { this.pic = pic; }
  public Integer getAuditStatus() { return auditStatus; }
  public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }
  public String getYmd() { return ymd; }
  public void setYmd(String ymd) { this.ymd = ymd; }
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
