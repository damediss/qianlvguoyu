package com.guoyu.platform.trainingdepartment.dto;

import java.util.List;

/** 批量导入教务人员/负责人请求 */
public class ImportOthersReq {

  private String type;
  private List<Long> ids;

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public List<Long> getIds() { return ids; }
  public void setIds(List<Long> ids) { this.ids = ids; }
}
