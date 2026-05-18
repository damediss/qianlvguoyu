package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 个人技能矩阵
 */
@TableName("train_user_ability")
public class TrainUserAbilityEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 培训ID */
  private Long trainId;
  /** 课程ID */
  private Long courseId;
  /** 知识点ID */
  private Long knowledgeId;
  /** 考核结果 */
  private String result;
  /** 配置ID */
  private Long configId;
  /** 满分 */
  private Integer score;
  /** 当前得分 */
  private Integer currentScore;
  private Long userId;
  /** 平台用户ID */
  private Long platformUserId;
  /** 评语 */
  private String comments;
  /** 状态 */
  private Integer status;
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
  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }
  public Long getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
  public String getResult() { return result; }
  public void setResult(String result) { this.result = result; }
  public Long getConfigId() { return configId; }
  public void setConfigId(Long configId) { this.configId = configId; }
  public Integer getScore() { return score; }
  public void setScore(Integer score) { this.score = score; }
  public Integer getCurrentScore() { return currentScore; }
  public void setCurrentScore(Integer currentScore) { this.currentScore = currentScore; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
  public String getComments() { return comments; }
  public void setComments(String comments) { this.comments = comments; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
