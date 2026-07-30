package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_exam_shijuan 创建请求 */
public class TrainExamShijuanCreateReq {

  @Size(max = 255)
  /** 考试计划ID */
  private Long examId;

  @Size(max = 255)
  /** 试卷ID */
  private Long paperId;

  @Size(max = 255)
  /** 考场ID */
  private Long examRoomId;

  @Size(max = 255)
  /** 实操试卷ID */
  private Long operatePaperId;

  @Size(max = 255)
  /** 教室ID */
  private Long roomId;

  @Size(max = 255)
  /** 用户ID */
  private Long userId;

  public Long getExamId() {
    return examId;
  }

  public void setExamId(Long examId) {
    this.examId = examId;
  }

  public Long getPaperId() {
    return paperId;
  }

  public void setPaperId(Long paperId) {
    this.paperId = paperId;
  }

  public Long getExamRoomId() {
    return examRoomId;
  }

  public void setExamRoomId(Long examRoomId) {
    this.examRoomId = examRoomId;
  }

  public Long getOperatePaperId() {
    return operatePaperId;
  }

  public void setOperatePaperId(Long operatePaperId) {
    this.operatePaperId = operatePaperId;
  }

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long roomId) {
    this.roomId = roomId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }
}
