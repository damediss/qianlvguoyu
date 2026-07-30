package com.guoyu.platform.chazheng.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 查证批次进度响应(前端轮询)。
 */
@Data
@Builder
public class CertCheckBatchProgress {
  private Long batchId;
  private Integer total;
  private Integer completed;
  private Long success;
  private Long successNoCert;
  private Long failed;
  private Long pending;
  private Integer percent;
  private String status;
  private String elapsed;
  private String estimateRemaining;
}
