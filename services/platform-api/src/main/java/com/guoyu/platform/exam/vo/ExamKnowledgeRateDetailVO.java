package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamKnowledgeRateDetailEntity;
import java.time.LocalDateTime;

public class ExamKnowledgeRateDetailVO {

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

  public static ExamKnowledgeRateDetailVO from(ExamKnowledgeRateDetailEntity entity) {
    ExamKnowledgeRateDetailVO vo = new ExamKnowledgeRateDetailVO();
    vo.setId(entity.getId());
    vo.setKnowledgeRateId(entity.getKnowledgeRateId());
    vo.setKnowledgeId(entity.getKnowledgeId());
    vo.setRate(entity.getRate());
    vo.setUserId(entity.getUserId());
    vo.setSortNumber(entity.getSortNumber());
    vo.setComments(entity.getComments());
    vo.setStatus(entity.getStatus());
    vo.setTenantId(entity.getTenantId());
    vo.setCreateTime(entity.getCreateTime());
    return vo;
  }

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
}
