package com.guoyu.platform.exam.answercheck.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 客观题答案 AI 检查批次（exam_objective_answer_check_batch，方案 §12.2）。 */
@Getter
@Setter
@TableName("exam_objective_answer_check_batch")
public class ObjectiveAnswerCheckBatchEntity extends BaseEntity {

  /** 活动批次唯一槽；同一租户最多一个持有活动槽的批次，paused/done 与无 processing 的 failed 置 NULL。 */
  private Long activeTenantId;
  /** 编排代次；每次创建/继续/重试都换新值，旧 run 禁止派发与收尾。 */
  private Long runId;
  private String ownerId;
  private LocalDateTime leaseUntil;
  private String selectionMode;
  private String filterJson;
  private String status;
  private Integer total;
  private Integer done;
  private Integer failed;
  private Integer invalid;
  private Integer consistentHigh;
  private Integer mismatchHigh;
  private Integer lowConfidence;
  private Integer contentSuspect;
  private Integer contentInvalid;
  private Integer expectedCalls;
  private Integer callCount;
  private Long inputTokensExact;
  private Long inputTokensEstimated;
  private Long outputTokens;
  private Integer usageEstimatedCalls;
  private Long modelDurationMs;
  private String modelId;
  /** v1.5 JEV 连接配置的非敏感批次快照；真实密钥仍只走环境变量。 */
  private String jevBaseUrl;
  private Integer jevConcurrency;
  private Integer jevReadTimeoutSeconds;
  private String promptVersion;
  private BigDecimal confidenceThreshold;
  private BigDecimal marginThreshold;
  private String failReason;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;
}
