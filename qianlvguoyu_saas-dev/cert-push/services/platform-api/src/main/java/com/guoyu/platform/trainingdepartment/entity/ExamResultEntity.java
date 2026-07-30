package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试结果
 */
@TableName("exam_result")
public class ExamResultEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 题型 */
  private String type;
  /** train_exam_list表id */
  private Long examPlanId;
  /** 试卷id */
  private Long paperId;
  /** 题目id */
  private Long detailId;
  /** 获得分数 */
  private BigDecimal getScore;
  /** 0正考 1模拟考 2能力测评 3顺序练习 4随机练习 */
  private Integer examType;
  /** 配置试卷的主键 */
  private Long examId;
  /** 用户ID */
  private Long userId;
  private Long platformUserId;
  private String userAnswer;
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
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Long getExamPlanId() { return examPlanId; }
  public void setExamPlanId(Long examPlanId) { this.examPlanId = examPlanId; }
  public Long getPaperId() { return paperId; }
  public void setPaperId(Long paperId) { this.paperId = paperId; }
  public Long getDetailId() { return detailId; }
  public void setDetailId(Long detailId) { this.detailId = detailId; }
  public BigDecimal getGetScore() { return getScore; }
  public void setGetScore(BigDecimal getScore) { this.getScore = getScore; }
  public Integer getExamType() { return examType; }
  public void setExamType(Integer examType) { this.examType = examType; }
  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
  public String getUserAnswer() { return userAnswer; }
  public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
