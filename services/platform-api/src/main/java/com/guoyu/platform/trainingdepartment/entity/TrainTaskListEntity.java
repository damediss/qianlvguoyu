package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识点任务列表
 */
@TableName("train_task_list")
public class TrainTaskListEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

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
  /** 用户ID */
  private Long userId;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 满分 */
  private Integer score;
  /** 获得积分 */
  private BigDecimal getPoint;
  /** 审核状态 */
  private Integer checkStatus;
  /** 终审状态 */
  private Integer finalCheckStatus;
  /** 审核时间 */
  private LocalDateTime checkTime;
  /** 审核人ID */
  private Long checkUserId;
  /** 审核人姓名 */
  private String checkUsername;
  /** 终审时间 */
  private LocalDateTime finalCheckTime;
  /** 终审人ID */
  private Long finalCheckUserId;
  /** 终审人姓名 */
  private String finalCheckUsername;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
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
  public BigDecimal getGetPoint() { return getPoint; }
  public void setGetPoint(BigDecimal getPoint) { this.getPoint = getPoint; }
  public Integer getCheckStatus() { return checkStatus; }
  public void setCheckStatus(Integer checkStatus) { this.checkStatus = checkStatus; }
  public Integer getFinalCheckStatus() { return finalCheckStatus; }
  public void setFinalCheckStatus(Integer finalCheckStatus) { this.finalCheckStatus = finalCheckStatus; }
  public LocalDateTime getCheckTime() { return checkTime; }
  public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
  public Long getCheckUserId() { return checkUserId; }
  public void setCheckUserId(Long checkUserId) { this.checkUserId = checkUserId; }
  public String getCheckUsername() { return checkUsername; }
  public void setCheckUsername(String checkUsername) { this.checkUsername = checkUsername; }
  public LocalDateTime getFinalCheckTime() { return finalCheckTime; }
  public void setFinalCheckTime(LocalDateTime finalCheckTime) { this.finalCheckTime = finalCheckTime; }
  public Long getFinalCheckUserId() { return finalCheckUserId; }
  public void setFinalCheckUserId(Long finalCheckUserId) { this.finalCheckUserId = finalCheckUserId; }
  public String getFinalCheckUsername() { return finalCheckUsername; }
  public void setFinalCheckUsername(String finalCheckUsername) { this.finalCheckUsername = finalCheckUsername; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
