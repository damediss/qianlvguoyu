package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.List;

@TableName("exam_knowledge_rate")
public class ExamKnowledgeRateEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer courseId;
  private Integer type;
  private Integer isStandard;
  private String title;
  private Integer sortNumber;
  private String comments;
  private Integer status;
  private Integer userId;
  private Integer tenantId;
  private LocalDateTime createTime;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  @TableField(exist = false)
  private List<ExamKnowledgeRateDetailEntity> detailList;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public Integer getIsStandard() { return isStandard; }
  public void setIsStandard(Integer isStandard) { this.isStandard = isStandard; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public String getComments() { return comments; }
  public void setComments(String comments) { this.comments = comments; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public List<ExamKnowledgeRateDetailEntity> getDetailList() { return detailList; }
  public void setDetailList(List<ExamKnowledgeRateDetailEntity> detailList) { this.detailList = detailList; }
}
