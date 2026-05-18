package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务详情
 */
@TableName("train_task_info_detail")
public class TrainTaskInfoDetailEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 培训ID */
  private Long trainId;
  /** 任务列表ID */
  private Long taskListId;
  /** 工种ID */
  private Long workTypeId;
  /** 课程ID */
  private Long courseId;
  /** 知识点ID */
  private Long knowledgeId;
  /** 任务标题 */
  private String title;
  /** 任务类型 */
  private String type;
  /** 任务内容 */
  private String content;
  /** 获取学习时长 */
  private BigDecimal getLearnTime;
  /** 最小分钟数 */
  private Integer minMinute;
  /** 是否必须 0-否 1-是 */
  private Integer isRequired;
  private Long userId;
  /** 排序号 */
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;
  /** 满分 */
  private Integer score;

  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public Long getTaskListId() { return taskListId; }
  public void setTaskListId(Long taskListId) { this.taskListId = taskListId; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }
  public Long getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public BigDecimal getGetLearnTime() { return getLearnTime; }
  public void setGetLearnTime(BigDecimal getLearnTime) { this.getLearnTime = getLearnTime; }
  public Integer getMinMinute() { return minMinute; }
  public void setMinMinute(Integer minMinute) { this.minMinute = minMinute; }
  public Integer getIsRequired() { return isRequired; }
  public void setIsRequired(Integer isRequired) { this.isRequired = isRequired; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getScore() { return score; }
  public void setScore(Integer score) { this.score = score; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
