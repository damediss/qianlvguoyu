package com.guoyu.platform.trainingdepartment.dto;

/**
 * 培训考勤更新请求
 */
public class TrainAttendanceUpdateReq {
  /** 项目ID */
  private Long projectId;
  /** 班级ID */
  private Long classId;
  /** 用户ID */
  private Long userId;
  private String userName;
  private String attendanceDate;
  /** 课次 ID；空=按日考勤 */
  private Long scheduleId;
  /** 状态 */
  private Integer status;
  private String checkInTime;
  private String checkOutTime;
  private String photoPath;
  private String videoPath;
  /** 备注 */
  private String remark;
  /** 排序号 */
  private Integer sortNumber;

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }
  public String getAttendanceDate() { return attendanceDate; }
  public void setAttendanceDate(String attendanceDate) { this.attendanceDate = attendanceDate; }
  public Long getScheduleId() { return scheduleId; }
  public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getCheckInTime() { return checkInTime; }
  public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }
  public String getCheckOutTime() { return checkOutTime; }
  public void setCheckOutTime(String checkOutTime) { this.checkOutTime = checkOutTime; }
  public String getPhotoPath() { return photoPath; }
  public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
  public String getVideoPath() { return videoPath; }
  public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
