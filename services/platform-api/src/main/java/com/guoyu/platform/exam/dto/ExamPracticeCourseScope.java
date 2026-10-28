package com.guoyu.platform.exam.dto;

/** A course-bank work type and exam_course.level pair used by pre-practice selection. */
public class ExamPracticeCourseScope {

  private Integer workTypeId;
  private Integer courseLevel;

  public ExamPracticeCourseScope() {
  }

  public ExamPracticeCourseScope(Integer workTypeId, Integer courseLevel) {
    this.workTypeId = workTypeId;
    this.courseLevel = courseLevel;
  }

  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public Integer getCourseLevel() { return courseLevel; }
  public void setCourseLevel(Integer courseLevel) { this.courseLevel = courseLevel; }
}
