package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_exam_work_type_list 创建请求 */
public class TrainExamWorkTypeListCreateReq {

  @Size(max = 255)
  /** 考试计划ID */
  private Long examId;

  @Size(max = 255)
  /** 工种ID */
  private Long workTypeId;

  @Size(max = 255)
  /** 课程ID */
  private Long courseId;

  @Size(max = 255)
  /** 等级 */
  private String level;

  public Long getExamId() {
    return examId;
  }

  public void setExamId(Long examId) {
    this.examId = examId;
  }

  public Long getWorkTypeId() {
    return workTypeId;
  }

  public void setWorkTypeId(Long workTypeId) {
    this.workTypeId = workTypeId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }
}
