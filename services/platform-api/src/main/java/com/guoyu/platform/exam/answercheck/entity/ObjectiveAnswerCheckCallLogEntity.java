package com.guoyu.platform.exam.answercheck.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/** 客观题答案 AI 检查模型调用审计（exam_objective_answer_check_call_log，方案 §12.4）。 */
@Getter
@Setter
@TableName("exam_objective_answer_check_call_log")
public class ObjectiveAnswerCheckCallLogEntity extends BaseEntity {

  private Long batchId;
  private Long itemId;
  private Integer detailId;
  private Long runId;
  private Integer claimAttempt;
  private String callId;
  private String upstreamRequestId;
  private String toolName;
  private String promptVersion;
  private Integer chunkIndex;
  private Integer chunkCount;
  private String questionType;
  private String requestedModel;
  private String responseModel;
  private String confidenceDefinition;
  private String promptSha256;
  private String promptPreview;
  private Long inputTokens;
  private Long outputTokens;
  private Integer usageEstimated;
  private Integer durationMs;
  private String status;
  private String errorCode;
  private String errorMessage;
}
