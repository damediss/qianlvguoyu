package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 报名人员批量操作（对齐 peopleselfoptmoreAction） */
public class SignUpPeopleBatchReq {
  @NotEmpty
  private List<Long> ids;
  @NotBlank
  private String field;
  private Integer value;

  public List<Long> getIds() { return ids; }
  public void setIds(List<Long> ids) { this.ids = ids; }
  public String getField() { return field; }
  public void setField(String field) { this.field = field; }
  public Integer getValue() { return value; }
  public void setValue(Integer value) { this.value = value; }
}
