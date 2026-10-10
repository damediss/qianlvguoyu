package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_exam_staff 更新请求 */
public class TrainExamStaffUpdateReq {

  @Size(max = 255)
  /** 考试计划ID */
  private Long examId;

  @Size(max = 255)
  /** 考场ID */
  private Long examRoomId;

  @Size(max = 255)
  /** 教室ID */
  private Long roomId;

  @Size(max = 255)
  /** 用户ID */
  private Long userId;

  @Size(max = 255)
  /** 姓名 */
  private String realname;

  @Size(max = 255)
  /** 人员类型 */
  private String type;
  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Long getExamRoomId() { return examRoomId; }
  public void setExamRoomId(Long examRoomId) { this.examRoomId = examRoomId; }
  public Long getRoomId() { return roomId; }
  public void setRoomId(Long roomId) { this.roomId = roomId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getRealname() { return realname; }
  public void setRealname(String realname) { this.realname = realname; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
}
