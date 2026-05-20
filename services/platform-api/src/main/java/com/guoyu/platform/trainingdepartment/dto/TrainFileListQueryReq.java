package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 文件列表查询请求 */
public class TrainFileListQueryReq extends PageQuery {

  /** 关联表名 */
  private String tableName;
  /** 关联主键 */
  private String pk;
  /** 搜索关键词 */
  private String keyword;

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getPk() {
    return pk;
  }

  public void setPk(String pk) {
    this.pk = pk;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }
}
