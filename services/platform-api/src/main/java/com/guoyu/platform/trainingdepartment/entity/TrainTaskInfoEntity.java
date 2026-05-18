package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 培训配置任务列表
 */
@TableName("train_task_info")
public class TrainTaskInfoEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 培训ID */
  private Long trainId;
  /** 封面 */
  private String cover;
  /** 课程ID */
  private Long courseId;
  /** 任务ID */
  private Long trainTaskId;
  /** 知识点ID */
  private Long knowledgeId;
  /** 简介 */
  private String intro;
  /** 是否必须 0-否 1-是 */
  private Integer isRequired;
  /** 满分 */
  private Integer score;
  private Long userId;
  /** 排序号 */
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;

  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public String getCover() { return cover; }
  public void setCover(String cover) { this.cover = cover; }
  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }
  public Long getTrainTaskId() { return trainTaskId; }
  public void setTrainTaskId(Long trainTaskId) { this.trainTaskId = trainTaskId; }
  public Long getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
  public String getIntro() { return intro; }
  public void setIntro(String intro) { this.intro = intro; }
  public Integer getIsRequired() { return isRequired; }
  public void setIsRequired(Integer isRequired) { this.isRequired = isRequired; }
  public Integer getScore() { return score; }
  public void setScore(Integer score) { this.score = score; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
