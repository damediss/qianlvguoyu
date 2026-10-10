package com.guoyu.platform.trainingdepartment.dto;

public class TrainExamPeopleListQueryReq {

  private Integer page = 1;
  private Integer pageSize = 10;
  private String keyword;
  private Long examId;
  private Long platformUserId;
  private String phone;

  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
}
