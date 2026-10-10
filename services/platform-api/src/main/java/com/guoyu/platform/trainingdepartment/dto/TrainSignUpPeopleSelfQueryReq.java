package com.guoyu.platform.trainingdepartment.dto;


/** train_sign_up_people_self 查询请求 */
public class TrainSignUpPeopleSelfQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  /** 报名计划ID */
  private Long signUpId;
  /** 平台用户ID */
  private Long platformUserId;
  /** 审核状态 0待审核 1通过 2拒绝 */
  private Integer status;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getSignUpId() { return signUpId; }
  public void setSignUpId(Long signUpId) { this.signUpId = signUpId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
}
