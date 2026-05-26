package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_exam_review 更新请求 */
public class TrainExamReviewUpdateReq {

  @Size(max = 255)
  /** 考试计划ID */
  private Long examId;

  @Size(max = 255)
  /** 试卷ID */
  private Long paperId;

  @Size(max = 255)
  /** 用户ID */
  private Long userId;

  @Size(max = 255)
  /** 复习题数 */
  private Integer num;
  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Long getPaperId() { return paperId; }
  public void setPaperId(Long paperId) { this.paperId = paperId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getNum() { return num; }
  public void setNum(Integer num) { this.num = num; }
}
