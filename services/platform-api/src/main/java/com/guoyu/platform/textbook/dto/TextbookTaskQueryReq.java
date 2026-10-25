package com.guoyu.platform.textbook.dto;

import com.guoyu.platform.common.api.PageQuery;

/** T1/T2 我的任务查询（方案 §6.2）：status/keyword/overdueOnly + 分页。 */
public class TextbookTaskQueryReq extends PageQuery {

  /** writing 任务：assigned/writing/reviewing/approved/returned/overdue；review 任务：pending/returned/approved/all */
  private String status;
  private String keyword;
  private Boolean overdueOnly;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public Boolean getOverdueOnly() {
    return overdueOnly;
  }

  public void setOverdueOnly(Boolean overdueOnly) {
    this.overdueOnly = overdueOnly;
  }
}
