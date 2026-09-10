package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("task_examshicao_detail")
public class ExamTaskExamshicaoDetailEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer taskId;
  private String xingwei;
  private String kaohe1;
  private String kaohe2;
  private String kaohe3;
  private String kaohe4;
  private String jianding;
  private String nanyicd;
  private String content;
  private String pingfen;
  private String kaochang;
  private String kaosheng;
  private Integer isblod;
  private Integer tenantId;
  /** AI 生成批次 exam_skill_gen_batch.id（操作技能试题生成 V20260910_011 迁移加列） */
  private Long aiBatchId;
  /** 来源 0手工 1导入 2AI生成（同上迁移加列） */
  private Integer source;

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
  public String getNanyicd() { return nanyicd; }
  public void setNanyicd(String nanyicd) { this.nanyicd = nanyicd; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getPingfen() { return pingfen; }
  public void setPingfen(String pingfen) { this.pingfen = pingfen; }
  public String getKaochang() { return kaochang; }
  public void setKaochang(String kaochang) { this.kaochang = kaochang; }
  public String getKaosheng() { return kaosheng; }
  public void setKaosheng(String kaosheng) { this.kaosheng = kaosheng; }
  public Integer getIsblod() { return isblod; }
  public void setIsblod(Integer isblod) { this.isblod = isblod; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public Long getAiBatchId() { return aiBatchId; }
  public void setAiBatchId(Long aiBatchId) { this.aiBatchId = aiBatchId; }
  public Integer getSource() { return source; }
  public void setSource(Integer source) { this.source = source; }
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
