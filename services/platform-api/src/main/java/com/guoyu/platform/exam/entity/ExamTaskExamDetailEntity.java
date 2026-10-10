package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("task_exam_detail")
public class ExamTaskExamDetailEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer taskId;
  private String xingwei;
  private String kaohe1;
  private String kaohe2;
  private String kaohe3;
  private String kaohe4;
  private String jianding;
  private String level;
  private String nanyicd;
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
  private Integer isblod;
  private Long knowledgeId;
  private Long aiBatchId;
  private Integer source;
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
  public String getXingwei() { return xingwei; }
  public void setXingwei(String xingwei) { this.xingwei = xingwei; }
  public String getKaohe1() { return kaohe1; }
  public void setKaohe1(String kaohe1) { this.kaohe1 = kaohe1; }
  public String getKaohe2() { return kaohe2; }
  public void setKaohe2(String kaohe2) { this.kaohe2 = kaohe2; }
  public String getKaohe3() { return kaohe3; }
  public void setKaohe3(String kaohe3) { this.kaohe3 = kaohe3; }
  public String getKaohe4() { return kaohe4; }
  public void setKaohe4(String kaohe4) { this.kaohe4 = kaohe4; }
  public String getJianding() { return jianding; }
  public void setJianding(String jianding) { this.jianding = jianding; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getNanyicd() { return nanyicd; }
  public void setNanyicd(String nanyicd) { this.nanyicd = nanyicd; }
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
  public Integer getIsblod() { return isblod; }
  public void setIsblod(Integer isblod) { this.isblod = isblod; }
  public Long getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
  public Long getAiBatchId() { return aiBatchId; }
  public void setAiBatchId(Long aiBatchId) { this.aiBatchId = aiBatchId; }
  public Integer getSource() { return source; }
  public void setSource(Integer source) { this.source = source; }
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
