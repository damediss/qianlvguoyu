package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_exam_people_in_room 更新请求 */
public class TrainExamPeopleInRoomUpdateReq {

  @Size(max = 255)
  /** 考试计划ID */
  private Long examId;

  @Size(max = 255)
  /** 课程ID */
  private Long courseId;

  @Size(max = 255)
  /** 场次ID */
  private Long sessionId;

  @Size(max = 255)
  /** 考场ID */
  private Long roomId;

  @Size(max = 255)
  /** 考场关联ID */
  private Long examRoomId;

  @Size(max = 255)
  /** 用户ID */
  private Long userId;

  @Size(max = 255)
  /** 姓名 */
  private String realname;

  @Size(max = 255)
  /** 考生人员ID */
  private Long peopleId;
  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public Long getRoomId() { return roomId; }
  public void setRoomId(Long roomId) { this.roomId = roomId; }
  public Long getExamRoomId() { return examRoomId; }
  public void setExamRoomId(Long examRoomId) { this.examRoomId = examRoomId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getRealname() { return realname; }
  public void setRealname(String realname) { this.realname = realname; }
  public Long getPeopleId() { return peopleId; }
  public void setPeopleId(Long peopleId) { this.peopleId = peopleId; }
}
