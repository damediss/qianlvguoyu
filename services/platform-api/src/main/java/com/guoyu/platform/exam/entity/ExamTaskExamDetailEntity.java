package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("exam_task_exam_detail")
public class ExamTaskExamDetailEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer taskId;
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
  private Integer tenantId;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  private LocalDateTime createTime;
  private LocalDateTime updatedAt;
  private Integer createdBy;
  private Integer updatedBy;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
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
