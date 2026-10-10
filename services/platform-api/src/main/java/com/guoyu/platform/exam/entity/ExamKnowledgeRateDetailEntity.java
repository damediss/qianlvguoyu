package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("exam_knowledge_rate_detail")
public class ExamKnowledgeRateDetailEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer knowledgeRateId;
  private Integer knowledgeId;
  private Integer rate;
  private Integer userId;
  private Integer sortNumber;
  private String comments;
  private Integer status;
  private Integer tenantId;
  private LocalDateTime createTime;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getKnowledgeRateId() { return knowledgeRateId; }
  public void setKnowledgeRateId(Integer knowledgeRateId) { this.knowledgeRateId = knowledgeRateId; }
  public Integer getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Integer knowledgeId) { this.knowledgeId = knowledgeId; }
  public Integer getRate() { return rate; }
  public void setRate(Integer rate) { this.rate = rate; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public String getComments() { return comments; }
  public void setComments(String comments) { this.comments = comments; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
