package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 学习记录
 */
@TableName("train_user_learn_log")
public class TrainUserLearnLogEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 培训ID */
  private Long trainId;
  /** 比赛ID */
  private Long matchId;
  /** 培训任务ID */
  private Long trainTaskId;
  /** 学习时长(分钟) */
  private Integer learnTime;
  /** 得分 */
  private Integer score;
  /** 用户ID */
  private Long userId;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public Long getMatchId() { return matchId; }
  public void setMatchId(Long matchId) { this.matchId = matchId; }
  public Long getTrainTaskId() { return trainTaskId; }
  public void setTrainTaskId(Long trainTaskId) { this.trainTaskId = trainTaskId; }
  public Integer getLearnTime() { return learnTime; }
  public void setLearnTime(Integer learnTime) { this.learnTime = learnTime; }
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
