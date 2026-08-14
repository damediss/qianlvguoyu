package com.guoyu.platform.trainingdepartment.dto;

import java.util.List;

/** 执行结业请求 */
public class GraduationGraduateReq {

  /** all | selected，默认 all */
  private String mode;
  private List<Long> peopleIds;
  /** 是否允许将不合格学员一并标记为 failed；默认 true */
  private Boolean allowUnqualified;
  private String remark;

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public List<Long> getPeopleIds() {
    return peopleIds;
  }

  public void setPeopleIds(List<Long> peopleIds) {
    this.peopleIds = peopleIds;
  }

  public Boolean getAllowUnqualified() {
    return allowUnqualified;
  }

  public void setAllowUnqualified(Boolean allowUnqualified) {
    this.allowUnqualified = allowUnqualified;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }
}
