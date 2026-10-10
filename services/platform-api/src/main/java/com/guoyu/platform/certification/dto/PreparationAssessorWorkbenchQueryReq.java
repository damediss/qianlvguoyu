package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;

public class PreparationAssessorWorkbenchQueryReq extends PageQuery {
  private Long assessorId;
  /**
   * 视角：
   * - assessor: 我收到的任务（默认）
   * - leader: 我作为组长需要审核的任务
   */
  private String view;
  private String status;

  public Long getAssessorId() {
    return assessorId;
  }

  public void setAssessorId(Long assessorId) {
    this.assessorId = assessorId;
  }

  public String getView() {
    return view;
  }

  public void setView(String view) {
    this.view = view;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
