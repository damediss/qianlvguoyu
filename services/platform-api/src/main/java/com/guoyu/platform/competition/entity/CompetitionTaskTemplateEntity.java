package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_task_template")
public class CompetitionTaskTemplateEntity extends BaseEntity {

  private Long stageId;
  private String taskType;
  private String title;
  private String description;
  private String completionRule;
  private Integer requiresReview;
  private String deadlineRule;
  private String allowedFileTypes;
  private Integer sortOrder;
  private String status;

  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public String getTaskType() { return taskType; }
  public void setTaskType(String taskType) { this.taskType = taskType; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getCompletionRule() { return completionRule; }
  public void setCompletionRule(String completionRule) { this.completionRule = completionRule; }
  public Integer getRequiresReview() { return requiresReview; }
  public void setRequiresReview(Integer requiresReview) { this.requiresReview = requiresReview; }
  public String getDeadlineRule() { return deadlineRule; }
  public void setDeadlineRule(String deadlineRule) { this.deadlineRule = deadlineRule; }
  public String getAllowedFileTypes() { return allowedFileTypes; }
  public void setAllowedFileTypes(String allowedFileTypes) { this.allowedFileTypes = allowedFileTypes; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
