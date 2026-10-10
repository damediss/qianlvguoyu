package com.guoyu.platform.trainingdepartment.dto;


/** train_exam_shijuan 查询请求 */
public class TrainExamShijuanQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  /** 考试计划ID */
  private Long examId;
  /** 试卷ID */
  private Long paperId;
  /** 考场ID */
  private Long examRoomId;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Long getPaperId() { return paperId; }
  public void setPaperId(Long paperId) { this.paperId = paperId; }
  public Long getExamRoomId() { return examRoomId; }
  public void setExamRoomId(Long examRoomId) { this.examRoomId = examRoomId; }
}
