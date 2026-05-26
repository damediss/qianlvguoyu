package com.guoyu.platform.trainingdepartment.dto;


/** train_sign_up_self 查询请求 */
public class TrainSignUpSelfQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  /** 组织ID */
  private Long orgId;
  /** 培训信息ID */
  private Long trainInfoId;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
}
