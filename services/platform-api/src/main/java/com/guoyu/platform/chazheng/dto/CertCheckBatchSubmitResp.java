package com.guoyu.platform.chazheng.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 查证批次提交响应。
 */
@Data
@AllArgsConstructor
public class CertCheckBatchSubmitResp {
  private Long batchId;
  private Integer totalCount;
  private String message;
}
