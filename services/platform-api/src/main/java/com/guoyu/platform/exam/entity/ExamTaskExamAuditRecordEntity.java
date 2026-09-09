package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("task_exam_audit_records")
public class ExamTaskExamAuditRecordEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer expertId;
  private Integer auditRole;
  private Integer taskId;
  private Integer examId;
  private String examType;
  private String examContent;
  private String examA;
  private String examB;
  private String examC;
  private String examD;
  private String examE;
  private String examF;
  private String examG;
  private String examH;
  private String examAnswer;
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
  public String getExamType() { return examType; }
  public void setExamType(String examType) { this.examType = examType; }
  public String getExamContent() { return examContent; }
  public void setExamContent(String examContent) { this.examContent = examContent; }
  public String getExamA() { return examA; }
  public void setExamA(String examA) { this.examA = examA; }
  public String getExamB() { return examB; }
  public void setExamB(String examB) { this.examB = examB; }
  public String getExamC() { return examC; }
  public void setExamC(String examC) { this.examC = examC; }
  public String getExamD() { return examD; }
  public void setExamD(String examD) { this.examD = examD; }
  public String getExamE() { return examE; }
  public void setExamE(String examE) { this.examE = examE; }
  public String getExamF() { return examF; }
  public void setExamF(String examF) { this.examF = examF; }
  public String getExamG() { return examG; }
  public void setExamG(String examG) { this.examG = examG; }
  public String getExamH() { return examH; }
  public void setExamH(String examH) { this.examH = examH; }
  public String getExamAnswer() { return examAnswer; }
  public void setExamAnswer(String examAnswer) { this.examAnswer = examAnswer; }
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
