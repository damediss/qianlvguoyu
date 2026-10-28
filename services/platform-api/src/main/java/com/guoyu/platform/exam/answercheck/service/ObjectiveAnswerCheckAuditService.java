package com.guoyu.platform.exam.answercheck.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCallOutcome;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckCallLogEntity;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckBatchMapper;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckCallLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 调用审计落库：每个逻辑块一个独立短事务（方案 §6 / §12.4）。
 *
 * <p>审计写入失败必须向上抛出，使当前题 failed/retryable=1，禁止在无审计情况下静默标成功。
 */
@Service
public class ObjectiveAnswerCheckAuditService {

  private final ObjectiveAnswerCheckCallLogMapper callLogMapper;
  private final ObjectiveAnswerCheckBatchMapper batchMapper;

  public ObjectiveAnswerCheckAuditService(ObjectiveAnswerCheckCallLogMapper callLogMapper,
      ObjectiveAnswerCheckBatchMapper batchMapper) {
    this.callLogMapper = callLogMapper;
    this.batchMapper = batchMapper;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(ObjectiveAnswerCallOutcome outcome, Long tenantId, Long userId) {
    ObjectiveAnswerCheckCallLogEntity entity = new ObjectiveAnswerCheckCallLogEntity();
    entity.setId(IdWorker.getId());
    entity.setBatchId(outcome.batchId());
    entity.setItemId(outcome.itemId());
    entity.setDetailId(outcome.detailId());
    entity.setRunId(outcome.runId() == null ? 0L : outcome.runId());
    entity.setClaimAttempt(outcome.claimAttempt() == null ? 0 : outcome.claimAttempt());
    entity.setCallId(outcome.callId());
    entity.setUpstreamRequestId(null);
    entity.setToolName("objective_answer_check");
    entity.setPromptVersion(ObjectiveAnswerCheckSnapshotService.PROMPT_VERSION);
    entity.setChunkIndex(outcome.chunkIndex());
    entity.setChunkCount(outcome.chunkCount());
    entity.setQuestionType(outcome.questionType());
    entity.setRequestedModel(outcome.requestedModel());
    entity.setResponseModel(outcome.responseModel());
    entity.setConfidenceDefinition(truncate(outcome.confidenceDefinition(), 128));
    entity.setPromptSha256(outcome.promptSha256());
    entity.setPromptPreview(outcome.promptPreview());
    entity.setInputTokens(outcome.inputTokens());
    entity.setOutputTokens(outcome.outputTokens());
    entity.setUsageEstimated(outcome.usageEstimated() ? 1 : 0);
    entity.setDurationMs(outcome.durationMs());
    entity.setStatus(outcome.status());
    entity.setErrorCode(outcome.errorCode());
    entity.setErrorMessage(truncate(outcome.errorMessage(), 500));
    callLogMapper.insertExplicit(entity, tenantId, userId);

    // 每个实际发出的逻辑块都计 callCount；token 按精确/估算分桶，网络失败无 usage 时不做估算伪装
    long inputExact = 0L;
    long inputEstimated = 0L;
    int estimatedCalls = 0;
    if (outcome.inputTokens() != null) {
      if (outcome.usageEstimated()) {
        inputEstimated = outcome.inputTokens();
        estimatedCalls = 1;
      } else {
        inputExact = outcome.inputTokens();
      }
    }
    long output = outcome.outputTokens() == null ? 0L : outcome.outputTokens();
    long duration = outcome.durationMs() == null ? 0L : outcome.durationMs();
    batchMapper.addUsage(outcome.batchId(), inputExact, inputEstimated, output, estimatedCalls,
        duration);
  }

  private String truncate(String text, int max) {
    if (text == null) {
      return null;
    }
    return text.length() <= max ? text : text.substring(0, max);
  }
}
