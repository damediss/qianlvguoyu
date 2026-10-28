package com.guoyu.platform.exam.answercheck.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckCallContext;
import com.guoyu.platform.ai.dto.ObjectiveAnswerDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerOption;
import com.guoyu.platform.ai.dto.ObjectiveAnswerQuestion;
import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiException;
import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiService;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.answercheck.config.ObjectiveAnswerCheckProperties;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckBatchEntity;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckItemEntity;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckBatchMapper;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckItemMapper;
import com.guoyu.platform.exam.answercheck.parser.NormalizedObjectiveQuestion;
import com.guoyu.platform.exam.answercheck.parser.NormalizedOption;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 批次编排（方案 §3 / §11）。
 *
 * <p>线程边界：HTTP 线程只创建快照并投递编排；编排线程负责租约/心跳/派发/收尾；
 * Worker 负责单题认领后的模型调用与结果写入。两条异步链都在 finally 清理租户上下文，
 * 且只使用显式审计字段的自定义 SQL，不读取 SecurityContext。
 */
@Slf4j
@Service
public class ObjectiveAnswerCheckBatchService {

  private static final int MAX_CONSECUTIVE_SYSTEMIC_FAILURES = 5;

  private final ObjectiveAnswerCheckBatchMapper batchMapper;
  private final ObjectiveAnswerCheckItemMapper itemMapper;
  private final ObjectiveAnswerCheckAuditService auditService;
  private final ObjectiveAnswerCheckAiService aiService;
  private final ObjectiveAnswerCheckDomainService domainService;
  private final ObjectiveAnswerCheckProperties properties;
  private final ThreadPoolTaskExecutor orchestrationExecutor;
  private final ThreadPoolTaskExecutor workerExecutor;
  private final ScheduledExecutorService leaseScheduler;
  private final ObjectMapper objectMapper;

  public ObjectiveAnswerCheckBatchService(ObjectiveAnswerCheckBatchMapper batchMapper,
      ObjectiveAnswerCheckItemMapper itemMapper, ObjectiveAnswerCheckAuditService auditService,
      ObjectiveAnswerCheckAiService aiService, ObjectiveAnswerCheckDomainService domainService,
      ObjectiveAnswerCheckProperties properties,
      @Qualifier("objectiveAnswerCheckExecutor") ThreadPoolTaskExecutor orchestrationExecutor,
      @Qualifier("objectiveAnswerCheckWorkerExecutor") ThreadPoolTaskExecutor workerExecutor,
      @Qualifier("objectiveAnswerCheckLeaseScheduler")
      ScheduledExecutorService leaseScheduler, ObjectMapper objectMapper) {
    this.batchMapper = batchMapper;
    this.itemMapper = itemMapper;
    this.auditService = auditService;
    this.aiService = aiService;
    this.domainService = domainService;
    this.properties = properties;
    this.orchestrationExecutor = orchestrationExecutor;
    this.workerExecutor = workerExecutor;
    this.leaseScheduler = leaseScheduler;
    this.objectMapper = objectMapper;
  }

  /** 提交后投递编排；拒绝时持久化 failed 并释放无 processing 的活动槽。 */
  public void dispatch(Long batchId, Long tenantId, Long userId) {
    ObjectiveAnswerCheckBatchEntity batch = batchMapper.findById(batchId);
    if (batch == null || batch.getRunId() == null) {
      return;
    }
    if ("pending".equals(batch.getStatus())) {
      // 创建时只写 pending + run/租约；投递前原子转 running，避免事务内派发抢跑
      if (batchMapper.acquire(batchId, tenantId, batch.getRunId(), batch.getOwnerId(), userId)
          != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "批次状态已变化，请刷新后重试");
      }
      batch = batchMapper.findById(batchId);
    }
    if (batch == null || !"running".equals(batch.getStatus())) {
      return;
    }
    Long runId = batch.getRunId();
    String ownerId = batch.getOwnerId();
    try {
      orchestrationExecutor.execute(() -> orchestrate(batchId, tenantId, userId, runId, ownerId));
    } catch (RejectedExecutionException e) {
      latchFailed(batchId, runId, "系统繁忙，批次未能启动，请稍后继续");
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "系统繁忙，请稍后重试");
    }
  }

  private void orchestrate(Long batchId, Long tenantId, Long userId, Long runId, String ownerId) {
    TenantContextHolder.setTenantId(tenantId);
    AtomicInteger consecutiveFailures = new AtomicInteger(0);
    ScheduledFuture<?> heartbeat = leaseScheduler.scheduleAtFixedRate(() -> {
      try {
        batchMapper.renewLease(batchId, runId, ownerId);
      } catch (Exception e) {
        log.warn("[OAC] 续租失败 batchId={}: {}", batchId, e.getMessage());
      }
    }, 15, 15, TimeUnit.SECONDS);
    try {
      while (true) {
        ObjectiveAnswerCheckBatchEntity batch = batchMapper.findById(batchId);
        if (batch == null || !runId.equals(batch.getRunId())
            || !"running".equals(batch.getStatus())) {
          break;
        }
        if (batchMapper.renewLease(batchId, runId, ownerId) == 0) {
          log.warn("[OAC] 租约所有权失效，停止派发 batchId={}", batchId);
          return;
        }
        List<Long> ids = itemMapper.listPendingIds(batchId, Math.max(1, properties.getConcurrency()));
        if (ids.isEmpty()) {
          break;
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        boolean rejected = false;
        for (Long id : ids) {
          if (itemMapper.claim(id, runId) != 1) {
            continue;
          }
          ObjectiveAnswerCheckItemEntity item = itemMapper.findById(id);
          if (item == null) {
            continue;
          }
          int claimAttempt = item.getAttempts();
          try {
            futures.add(CompletableFuture.runAsync(
                () -> processClaimed(item, claimAttempt, tenantId, userId, runId,
                    batch.getConfidenceThreshold(), batch.getMarginThreshold(),
                    consecutiveFailures), workerExecutor));
          } catch (RejectedExecutionException e) {
            itemMapper.restorePending(id, claimAttempt, userId);
            latchFailed(batchId, runId, "检查任务排队已满，请稍后继续");
            rejected = true;
            break;
          }
        }
        if (!futures.isEmpty()) {
          CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        }
        if (rejected) {
          break;
        }
      }
      finalizeBatch(batchId, runId);
    } catch (Exception e) {
      log.error("[OAC] 编排异常 batchId={}, runId={}", batchId, runId, e);
      latchFailed(batchId, runId, "检查任务异常中断，可继续执行");
    } finally {
      heartbeat.cancel(false);
      TenantContextHolder.clear();
    }
  }

  private void processClaimed(ObjectiveAnswerCheckItemEntity claimed, int claimAttempt,
      Long tenantId, Long userId, Long runId, BigDecimal batchConfidenceThreshold,
      BigDecimal batchMarginThreshold, AtomicInteger consecutiveFailures) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      String questionType = claimed.getQuestionType();
      NormalizedObjectiveQuestion question = toNormalized(claimed);
      ObjectiveAnswerQuestion aiQuestion = new ObjectiveAnswerQuestion(claimed.getDetailId(),
          questionType, question.stemText(), question.contextPath(), toAiOptions(question));
      ObjectiveAnswerCheckCallContext context = new ObjectiveAnswerCheckCallContext(tenantId, userId,
          claimed.getBatchId(), claimed.getId(), runId, claimAttempt);
      ObjectiveAnswerDecision decision = aiService.check(aiQuestion, context,
          outcome -> auditService.record(outcome, tenantId, userId));
      // 判定阈值取批次创建时的固化快照（方案 §9.1/§17.3：配置变更不影响历史批次口径）
      BigDecimal theta = batchConfidenceThreshold != null ? batchConfidenceThreshold
          : BigDecimal.valueOf(properties.getConfidenceThreshold());
      BigDecimal delta = batchMarginThreshold != null ? batchMarginThreshold
          : BigDecimal.valueOf(properties.getMarginThreshold());
      ObjectiveAnswerCheckResult result = domainService.judge(question, decision, theta, delta);

      ObjectiveAnswerCheckItemEntity update = new ObjectiveAnswerCheckItemEntity();
      update.setId(claimed.getId());
      update.setResultCode(result.resultCode());
      update.setConsistent(result.consistent() ? 1 : 0);
      update.setOriginalAnswerKeys(truncate(result.originalAnswerKeys(), 128));
      update.setAiAnswerKeys(truncate(result.aiAnswerKeys(), 128));
      update.setReferenceConfidence(result.referenceConfidence());
      update.setAiConfidence(result.aiConfidence());
      update.setMinMargin(result.minMargin());
      update.setDecisionWarning(result.decisionWarning());
      update.setOptionResultsJson(objectMapper.writeValueAsString(result.optionResults()));
      int updated = itemMapper.writeSuccess(update, runId, claimAttempt, userId);
      if (updated == 1) {
        batchMapper.incrementResult(claimed.getBatchId(), runId, result.resultCode());
        consecutiveFailures.set(0);
      } else {
        log.warn("[OAC] 晚到结果被 fencing batchId={}, itemId={}", claimed.getBatchId(),
            claimed.getId());
      }
    } catch (ObjectiveAnswerCheckAiException e) {
      handleFailure(claimed, claimAttempt, userId, runId, e.isRetryable(), e.getErrorCode(),
          e.getMessage(), e.isSystemic(), consecutiveFailures);
    } catch (Exception e) {
      log.warn("[OAC] 单题检查异常 itemId={}: {}", claimed.getId(), e.getMessage());
      handleFailure(claimed, claimAttempt, userId, runId, true, "INTERNAL_ERROR",
          e.getMessage(), true, consecutiveFailures);
    } finally {
      TenantContextHolder.clear();
    }
  }

  private void handleFailure(ObjectiveAnswerCheckItemEntity item, int claimAttempt, Long userId,
      Long runId, boolean retryable, String errorCode, String message, boolean systemic,
      AtomicInteger consecutiveFailures) {
    ObjectiveAnswerCheckItemEntity update = new ObjectiveAnswerCheckItemEntity();
    update.setId(item.getId());
    update.setRetryable(retryable ? 1 : 0);
    update.setErrorCode(errorCode);
    update.setErrorMessage(truncate(message, 500));
    int updated = itemMapper.writeFailed(update, runId, claimAttempt, userId);
    if (updated == 1) {
      batchMapper.incrementResult(item.getBatchId(), runId, ObjectiveAnswerCheckDomainService.RESULT_FAILED);
    }
    if (!systemic) {
      return;
    }
    int failures = consecutiveFailures.incrementAndGet();
    if ("UNAUTHORIZED".equals(errorCode) || failures >= MAX_CONSECUTIVE_SYSTEMIC_FAILURES) {
      latchFailed(item.getBatchId(), runId,
          "UNAUTHORIZED".equals(errorCode) ? "模型服务鉴权失败，批次已停止" : "连续多次调用失败，批次已停止");
    }
  }

  private void latchFailed(Long batchId, Long runId, String reason) {
    try {
      int updated = batchMapper.markFailed(batchId, runId, reason);
      if (updated == 1
          && itemMapper.countByProcessStatus(batchId, "processing") == 0) {
        batchMapper.releaseActivity(batchId);
      }
    } catch (Exception e) {
      log.warn("[OAC] 失败锁存异常 batchId={}: {}", batchId, e.getMessage());
    }
  }

  private void finalizeBatch(Long batchId, Long runId) {
    ObjectiveAnswerCheckBatchEntity batch = batchMapper.findById(batchId);
    if (batch == null || !runId.equals(batch.getRunId())) {
      return;
    }
    if (itemMapper.countByProcessStatus(batchId, "processing") > 0) {
      return;
    }
    if ("failed".equals(batch.getStatus())) {
      batchMapper.releaseActivity(batchId);
      return;
    }
    int pending = itemMapper.countByProcessStatus(batchId, "pending");
    String target = "pausing".equals(batch.getStatus())
        ? (pending > 0 ? "paused" : "done")
        : "done";
    if (batchMapper.finish(batchId, runId, target) == 0) {
      batchMapper.releaseActivity(batchId);
    }
  }

  /** 暂停：只阻止新派发，不强杀已开始的调用。 */
  public void pause(Long batchId) {
    ObjectiveAnswerCheckBatchEntity batch = requireBatch(batchId);
    if (!"pending".equals(batch.getStatus()) && !"running".equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前状态不能暂停");
    }
    if (batchMapper.requestPause(batchId) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前状态不能暂停");
    }
    batchMapper.finishPausedIfIdle(batchId);
  }

  @Transactional
  public void resume(Long batchId, Long tenantId, Long userId) {
    ObjectiveAnswerCheckBatchEntity batch = requireBatch(batchId);
    int processing = itemMapper.countByProcessStatus(batchId, "processing");
    boolean allowed = "paused".equals(batch.getStatus())
        || ("failed".equals(batch.getStatus()) && processing == 0);
    if (!allowed) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前状态不能继续");
    }
    long runId = IdWorker.getId();
    try {
      int acquired = batchMapper.acquire(batchId, tenantId, runId, ownerId(), userId);
      if (acquired != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "已有正在执行的检查批次，请稍后重试");
      }
    } catch (DuplicateKeyException e) {
      throw new BusinessException(ErrorCode.CONFLICT, "已有正在执行的检查批次，请稍后重试");
    }
  }

  /**
   * 失败重试：只复位 retryable=1 的 failed 行，同事务回减 done/failed 并占用新 run。
   *
   * @return 实际复位行数
   */
  @Transactional
  public int retryFailed(Long batchId, Long tenantId, Long userId) {
    ObjectiveAnswerCheckBatchEntity batch = requireBatch(batchId);
    int processing = itemMapper.countByProcessStatus(batchId, "processing");
    boolean allowed = "paused".equals(batch.getStatus()) || "done".equals(batch.getStatus())
        || ("failed".equals(batch.getStatus()) && processing == 0);
    if (!allowed) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前状态不能重试失败项");
    }
    int reset = itemMapper.resetFailedToPending(batchId, userId);
    if (reset == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可重试的失败试题");
    }
    batchMapper.decrementForRetry(batchId, reset);
    long runId = IdWorker.getId();
    try {
      if (batchMapper.acquire(batchId, tenantId, runId, ownerId(), userId) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "已有正在执行的检查批次，请稍后重试");
      }
    } catch (DuplicateKeyException e) {
      throw new BusinessException(ErrorCode.CONFLICT, "已有正在执行的检查批次，请稍后重试");
    }
    return reset;
  }

  public ObjectiveAnswerCheckBatchEntity requireBatch(Long batchId) {
    ObjectiveAnswerCheckBatchEntity batch = batchMapper.findById(batchId);
    if (batch == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在");
    }
    return batch;
  }

  /**
   * 过期接管与启动恢复：租约有效的批次绝不重置；failed 保持 failed，遗留 processing 回 pending。
   * 由 ApplicationReadyEvent 与 30 秒固定调度共同触发；单测直接调用本方法保证确定性。
   */
  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    if (properties.isRecoveryEnabled()) {
      recoverExpired();
    }
  }

  @Scheduled(fixedDelay = 30_000L)
  public void recoverExpired() {
    if (!properties.isRecoveryEnabled()) {
      return;
    }
    List<ObjectiveAnswerCheckBatchMapper.RecoveryCandidate> candidates;
    try {
      candidates = batchMapper.listExpired();
    } catch (Exception e) {
      log.warn("[OAC] 恢复扫描失败: {}", e.getMessage());
      return;
    }
    for (ObjectiveAnswerCheckBatchMapper.RecoveryCandidate candidate : candidates) {
      TenantContextHolder.setTenantId(candidate.getTenantId());
      try {
        itemMapper.recoverProcessing(candidate.getId(), 0L);
        int pending = itemMapper.countByProcessStatus(candidate.getId(), "pending");
        batchMapper.resetExpired(candidate.getId(), pending == 0 ? "done" : "paused");
        log.info("[OAC] 已接管过期批次 batchId={}, pending={}", candidate.getId(), pending);
      } catch (Exception e) {
        log.warn("[OAC] 接管批次失败 batchId={}: {}", candidate.getId(), e.getMessage());
      } finally {
        TenantContextHolder.clear();
      }
    }
  }

  private NormalizedObjectiveQuestion toNormalized(ObjectiveAnswerCheckItemEntity item) {
    List<NormalizedOption> options = new ArrayList<>();
    try {
      List<Map<String, Object>> raw = objectMapper.readValue(item.getOptionSnapshotJson(),
          new TypeReference<>() {
          });
      for (Map<String, Object> map : raw) {
        options.add(new NormalizedOption(String.valueOf(map.get("optionId")),
            ((Number) map.getOrDefault("originalIndex", 0)).intValue(),
            String.valueOf(map.get("label")), String.valueOf(map.get("text")),
            Boolean.TRUE.equals(map.get("originalCorrect"))));
      }
    } catch (Exception e) {
      throw new IllegalStateException("选项快照无法解析", e);
    }
    String contextPath = item.getKnowledgePath();
    return new NormalizedObjectiveQuestion(item.getDetailId(), item.getQuestionType(),
        item.getStemText(), contextPath, options, item.getSourceHash());
  }

  private List<ObjectiveAnswerOption> toAiOptions(NormalizedObjectiveQuestion question) {
    List<ObjectiveAnswerOption> options = new ArrayList<>();
    for (NormalizedOption option : question.options()) {
      options.add(new ObjectiveAnswerOption(option.optionId(), option.originalIndex(), option.label(),
          option.text(), option.originalCorrect()));
    }
    return options;
  }

  private String ownerId() {
    return System.getenv().getOrDefault("HOSTNAME", "local") + ":" + ProcessHandle.current().pid()
        + ":" + java.util.UUID.randomUUID();
  }

  private String truncate(String text, int max) {
    if (text == null) {
      return null;
    }
    return text.length() <= max ? text : text.substring(0, max);
  }
}
