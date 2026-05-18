package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/** 考试结果创建请求 */
public class ExamResultCreateReq {

  @NotBlank
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

  private Long userId;

  private Long platformUserId;

  @NotBlank
  private String userAnswer;

  private Integer sortNumber;

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
}
