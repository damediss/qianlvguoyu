package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 课时列表创建请求 */
public class TrainCourseCreateReq {

  /** 培训ID */
  private Long trainId;

  /** 教师身份证 */
  @Size(max = 255)
  private String teacherIdCard;

  /** 教师姓名 */
  @Size(max = 20)
  /** 讲师姓名 */
  private String teacherName;

  /** 教师用户ID */
  private Long teacherUserId;

  /** 类型 */
  private Integer type;

  /** 课程日期 */
  @Size(max = 20)
  private String courseDate;

  @Size(max = 500)
  /** 标题 */
  private String title;

  /** 开始时间 */
  @Size(max = 20)
  /** 开始时间 */
  private String startTime;

  /** 结束时间 */
  @Size(max = 20)
  /** 结束时间 */
  private String endTime;

  /** 理论课时 */
  private Integer theoryTime;

  /** 实操课时 */
  private Integer operationTime;

  /** 排序号 */
  private Integer sortNumber;

  public Long getTrainId() {
    return trainId;
  }

  public void setTrainId(Long trainId) {
    this.trainId = trainId;
  }

  public String getTeacherIdCard() {
    return teacherIdCard;
  }

  public void setTeacherIdCard(String teacherIdCard) {
    this.teacherIdCard = teacherIdCard;
  }

  public String getTeacherName() {
    return teacherName;
  }

  public void setTeacherName(String teacherName) {
    this.teacherName = teacherName;
  }

  public Long getTeacherUserId() {
    return teacherUserId;
  }

  public void setTeacherUserId(Long teacherUserId) {
    this.teacherUserId = teacherUserId;
  }

  public Integer getType() {
    return type;
  }

  public void setType(Integer type) {
    this.type = type;
  }

  public String getCourseDate() {
    return courseDate;
  }

  public void setCourseDate(String courseDate) {
    this.courseDate = courseDate;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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

  public Integer getTheoryTime() {
    return theoryTime;
  }

  public void setTheoryTime(Integer theoryTime) {
    this.theoryTime = theoryTime;
  }

  public Integer getOperationTime() {
    return operationTime;
  }

  public void setOperationTime(Integer operationTime) {
    this.operationTime = operationTime;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
