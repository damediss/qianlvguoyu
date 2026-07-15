package com.guoyu.platform.chazheng.dto;

import com.guoyu.platform.chazheng.entity.CertCheckBatchEntity;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 查证批次详细响应(完成后查看结果)。
 */
@Data
public class CertCheckBatchReport {
  private Long batchId;
  private String status;
  private Long triggerBy;
  private String sourceFilename;
  private LocalDateTime triggeredAt;
  private LocalDateTime completedAt;
  private CertCheckSummary summary;
  private List<CertCheckFailedDetail> failedDetails;

  /** 汇总统计 */
  @Data
  public static class CertCheckSummary {
    private Integer total;
    private Long success;
    private Long successNoCert;
    private Long failed;
  }

  /** 失败用户详情 */
  @Data
  public static class CertCheckFailedDetail {
    private Long userId;
    private String userName;
    private String error;
    private Integer retryCount;
  }
}
