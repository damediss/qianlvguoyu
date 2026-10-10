package com.guoyu.platform.trainingdepartment.dto;

import java.util.List;

/** 从报名表导入学员请求 */
public class ImportStudentsReq {

  private List<Long> studentIds;

  public List<Long> getStudentIds() { return studentIds; }
  public void setStudentIds(List<Long> studentIds) { this.studentIds = studentIds; }
}
