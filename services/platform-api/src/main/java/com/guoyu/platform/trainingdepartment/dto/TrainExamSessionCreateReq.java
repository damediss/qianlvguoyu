package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_exam_session 创建请求 */
public class TrainExamSessionCreateReq {

  @Size(max = 255)
  /** 考试计划ID */
  private Long examId;

  @Size(max = 255)
  /** 课程ID */
  private Long courseId;

  @Size(max = 255)
  /** 用户ID */
  private Long userId;

  @Size(max = 255)
  /** 开始时间 */
  private String startTime;

  @Size(max = 255)
  /** 结束时间 */
  private String endTime;

  @Size(max = 255)
  /** 课程名称 */
  private String courseName;

  public Long getExamId() {
    return examId;
  }

  public void setExamId(Long examId) {
    this.examId = examId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public String getCourseName() {
    return courseName;
  }

  public void setCourseName(String courseName) {
    this.courseName = courseName;
  }
}
