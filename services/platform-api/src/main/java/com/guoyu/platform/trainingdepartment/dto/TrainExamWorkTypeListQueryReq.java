package com.guoyu.platform.trainingdepartment.dto;


/** train_exam_work_type_list 查询请求 */
public class TrainExamWorkTypeListQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  /** 考试计划ID */
  private Long examId;
  /** 工种ID */
  private Long workTypeId;
  /** 课程ID */
  private Long courseId;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }
}
