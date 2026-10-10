package com.guoyu.platform.trainingdepartment.dto;


/** train_exam_staff 查询请求 */
public class TrainExamStaffQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  /** 考试计划ID */
  private Long examId;
  /** 考场ID */
  private Long examRoomId;
  /** 教室ID */
  private Long roomId;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Long getExamRoomId() { return examRoomId; }
  public void setExamRoomId(Long examRoomId) { this.examRoomId = examRoomId; }
  public Long getRoomId() { return roomId; }
  public void setRoomId(Long roomId) { this.roomId = roomId; }
}
