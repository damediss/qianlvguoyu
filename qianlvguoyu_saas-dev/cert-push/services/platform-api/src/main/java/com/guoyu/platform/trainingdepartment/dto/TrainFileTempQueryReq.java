package com.guoyu.platform.trainingdepartment.dto;


/** train_file_temp 查询请求 */
public class TrainFileTempQueryReq {

  private String keyword;
  /** 模板类型：0认定 1培训 */
  private Integer type;
  /** 逗号分隔的 ID 列表 */
  private String ids;
  private Integer page = 1;
  private Integer pageSize = 10;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public String getIds() { return ids; }
  public void setIds(String ids) { this.ids = ids; }
}
