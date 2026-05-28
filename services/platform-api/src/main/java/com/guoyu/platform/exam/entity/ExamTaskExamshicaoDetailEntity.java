package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("exam_task_examshicao_detail")
public class ExamTaskExamshicaoDetailEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer taskId;
  private String jianding;
  private String content;
  private String pingfen;
  private String kaochang;
  private String kaosheng;
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
  public String getJianding() { return jianding; }
  public void setJianding(String jianding) { this.jianding = jianding; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getPingfen() { return pingfen; }
  public void setPingfen(String pingfen) { this.pingfen = pingfen; }
  public String getKaochang() { return kaochang; }
  public void setKaochang(String kaochang) { this.kaochang = kaochang; }
  public String getKaosheng() { return kaosheng; }
  public void setKaosheng(String kaosheng) { this.kaosheng = kaosheng; }
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
