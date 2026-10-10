package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 文件详情查询请求 */
public class TrainFileDetailQueryReq extends PageQuery {

  /** 文件ID */
  private Long fileId;

  public Long getFileId() {
    return fileId;
  }

  public void setFileId(Long fileId) {
    this.fileId = fileId;
  }
}
