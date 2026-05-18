package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 考试结果查询请求 */
public class ExamResultQueryReq extends PageQuery {

  /** train_exam_list表id */
  private Long examPlanId;

  /** 试卷id */
  private Long paperId;

  /** 0正考 1模拟考 2能力测评 3顺序练习 4随机练习 */
  private Integer examType;

  private Long userId;

  private Long platformUserId;

  public Long getExamPlanId() { return examPlanId; }
  public void setExamPlanId(Long examPlanId) { this.examPlanId = examPlanId; }
  public Long getPaperId() { return paperId; }
  public void setPaperId(Long paperId) { this.paperId = paperId; }
  public Integer getExamType() { return examType; }
  public void setExamType(Integer examType) { this.examType = examType; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
}
