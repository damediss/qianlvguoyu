package com.guoyu.platform.trainingdepartment.dto;

/**
 * 培训考勤查询请求
 */
public class TrainAttendanceQueryReq {
  private Integer page = 1;
  private Integer pageSize = 20;
  /** 项目ID */
  private Long projectId;
  /** 班级ID */
  private Long classId;
  /** 用户ID */
  private Long userId;
  private String attendanceDate;
  /** 状态 */
  private Integer status;
  /** 搜索关键词 */
  private String keyword;

  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getAttendanceDate() { return attendanceDate; }
  public void setAttendanceDate(String attendanceDate) { this.attendanceDate = attendanceDate; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
