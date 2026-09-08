package com.guoyu.platform.exam.theorygen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import com.guoyu.platform.exam.theorygen.dto.TheoryGenBatchVO;
import com.guoyu.platform.exam.theorygen.dto.TheoryQuestionItem;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintNodeEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenBatchEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenDraftEntity;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintNodeMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryGenBatchMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryGenDraftMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 理论试题生成批次：提交（秒回）→ 异步并发分块（1 考评点 = 1 次 LLM 调用）→ 草稿落库。
 *
 * <p>线程边界（优化方案 v0.3）：submit/pause/resume 走 HTTP 线程；processBatch 跑 theory-gen- 编排线程，
 * 块任务跑 theory-gen-block- Worker（并发度 theory.gen.block-concurrency，默认 5）。
 * tenantId/userId 参数传递，禁止调 SecurityContext（陷阱 #2、踩坑 #15/#17）；
 * 每 Worker 线程独立 set/clear TenantContextHolder。
 *
 * <p>编排独占权（R8）：run_id 每次 submit/resume/retryFailed 重新生成，编排线程逐块校验
 * 状态 = RUNNING 且 run_id 一致才继续派发，旧编排器读到新 run_id 即自行退出，杜绝双编排器。
 *
 * <p>断点续跑（P1）：循环前一次性查本批次已有草稿的考评点并跳过，恢复/重试只补缺口；
 * 块落库经 TransactionTemplate 原子化，"该点有草稿"="该块完整完成"。
 */
@Service
@Slf4j
public class TheoryGenBatchService {

  /** 熔断阈值：按块完成顺序连续失败 5 次停止派发（与串行版语义一致，R7） */
  private static final int MAX_CONSECUTIVE_FAILURES = 5;

  /** 派发轮次上限（首轮 + 自动补跑共 N 轮），可配置；默认 4（2026-09-08 依需求方要求由 3 调至 4）。
   * 轮次耗尽仍有缺口 → FAILED，人工"重试失败块"兜底 */
  @Value("${platform.theory.gen.max-dispatch-rounds:4}")
  private int maxDispatchRounds = 4; // 字段默认值：单测直接 new 构造（无 Spring 注入）时保持 4 轮

  private final TheoryBlueprintMapper blueprintMapper;
  private final TheoryBlueprintNodeMapper nodeMapper;
  private final TheoryGenBatchMapper batchMapper;
  private final TheoryGenDraftMapper draftMapper;
  private final ExamFaqModelConfigMapper modelConfigMapper;
  private final TheoryGenAiService aiService;
  private final TheoryQuestionValidator validator;
  private final ObjectMapper objectMapper;
  private final ThreadPoolTaskExecutor theoryGenExecutor;
  private final ThreadPoolTaskExecutor theoryGenBlockExecutor;
  private final TransactionTemplate transactionTemplate;
  private final int blockConcurrency;
  /** 补跑轮间退避基数：第 n 轮补跑前退避 n×基数，等待限流/超时等瞬时故障恢复 */
  private final long retryBackoffBaseMillis;

  public TheoryGenBatchService(TheoryBlueprintMapper blueprintMapper,
      TheoryBlueprintNodeMapper nodeMapper, TheoryGenBatchMapper batchMapper,
      TheoryGenDraftMapper draftMapper, ExamFaqModelConfigMapper modelConfigMapper,
      TheoryGenAiService aiService, TheoryQuestionValidator validator,
      ObjectMapper objectMapper,
      @Qualifier("theoryGenExecutor") ThreadPoolTaskExecutor theoryGenExecutor,
      @Qualifier("theoryGenBlockExecutor") ThreadPoolTaskExecutor theoryGenBlockExecutor,
      TransactionTemplate transactionTemplate,
      @Value("${platform.theory.gen.block-concurrency:5}") int blockConcurrency,
      @Value("${platform.theory.gen.retry-backoff-base-millis:10000}") long retryBackoffBaseMillis) {
    this.blueprintMapper = blueprintMapper;
    this.nodeMapper = nodeMapper;
    this.batchMapper = batchMapper;
    this.draftMapper = draftMapper;
    this.modelConfigMapper = modelConfigMapper;
    this.aiService = aiService;
    this.validator = validator;
    this.objectMapper = objectMapper;
    this.theoryGenExecutor = theoryGenExecutor;
    this.theoryGenBlockExecutor = theoryGenBlockExecutor;
    this.transactionTemplate = transactionTemplate;
    this.blockConcurrency = blockConcurrency;
    this.retryBackoffBaseMillis = retryBackoffBaseMillis;
  }

  /** 提交生成批次（秒回 batchId；实际生成在 Worker 线程）。
   * 注意：本方法不带 @Transactional——写库经 TransactionTemplate 先提交，之后才投递编排线程。
   * 此前写库与 submit 同处一个事务，Worker 在提交前抢跑 selectById 读不到新批次，
   * 报"理论生成批次不存在"后静默返回，批次永远停在 pending（生产 09-08 事故）。 */
  public TheoryGenBatchVO submit(Long tenantId, Long userId, Long blueprintId, Long modelConfigId,
      Map<String, Integer> quota) {
    TheoryBlueprintEntity bp = blueprintMapper.selectById(blueprintId);
    if (bp == null || !bp.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    // 已有进行中的批次则拒绝重复提交（同蓝图串行）
    Long running = batchMapper.selectCount(new LambdaQueryWrapper<TheoryGenBatchEntity>()
        .eq(TheoryGenBatchEntity::getTenantId, tenantId)
        .eq(TheoryGenBatchEntity::getBlueprintId, blueprintId)
        .in(TheoryGenBatchEntity::getStatus,
            TheoryGenBatchEntity.STATUS_PENDING, TheoryGenBatchEntity.STATUS_RUNNING));
    if (running != null && running > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "该细目表已有进行中的生成批次");
    }
    ExamFaqModelConfigEntity cfg = modelConfigMapper.selectById(modelConfigId);
    if (cfg == null || !"enabled".equals(cfg.getStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在或未启用");
    }
    if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()
        || "PENDING_USER_INPUT".equals(cfg.getApiKey())) {
      throw new BusinessException(ErrorCode.CONFLICT, "模型 api_key 未配置，请先在模型配置中回填");
    }
    validateGlmThinkingConfig(cfg);
    // 题量配额：不传用默认（指引下限）；低于下限拒绝
    Map<String, Integer> effectiveQuota = quota == null || quota.isEmpty()
        ? TheoryGenPromptBuilder.defaultQuota(bp.getLevel()) : quota;
    Map<String, Integer> minQuota = TheoryGenPromptBuilder.defaultQuota(bp.getLevel());
    for (Map.Entry<String, Integer> e : minQuota.entrySet()) {
      int actual = effectiveQuota.getOrDefault(e.getKey(), 0);
      if (actual < e.getValue()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "题型 " + e.getKey() + " 题量 " + actual + " 低于指引下限 " + e.getValue());
      }
    }

    List<TheoryBlueprintNodeEntity> points = listPoints(tenantId, blueprintId);
    if (points.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "细目表无考评点，无法生成");
    }

    TheoryGenBatchEntity batch = new TheoryGenBatchEntity();
    batch.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    batch.setTenantId(tenantId);
    batch.setBlueprintId(blueprintId);
    batch.setWorkTypeId(bp.getWorkTypeId());
    batch.setLevel(bp.getLevel());
    batch.setModelConfigId(cfg.getId());
    batch.setModelId(cfg.getModelId());
    batch.setModelCode(cfg.getModelCode());
    batch.setThinking(cfg.getThinking());
    batch.setReasoningEffort(cfg.getReasoningEffort());
    batch.setQuotaConfig(writeJson(effectiveQuota));
    batch.setStatus(TheoryGenBatchEntity.STATUS_PENDING);
    batch.setRunId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    batch.setTotalPoints(points.size());
    batch.setDonePoints(0);
    batch.setFailedPoints(0);
    batch.setTotalQuestions(0);
    batch.setTokenUsage(0L);
    batch.setPromptDigest(abbreviate(TheoryGenPromptBuilder.buildSystemPrompt(bp.getLevel(), effectiveQuota), 200));
    batch.setCreatedBy(userId);
    batch.setUpdatedBy(userId);
    batch.setCreatedAt(LocalDateTime.now());
    batch.setUpdatedAt(LocalDateTime.now());
    batch.setDeleted(0);
    // 写库经 TransactionTemplate 提交完毕后再投递编排线程，消除"Worker 抢跑读不到新批次"竞态
    transactionTemplate.executeWithoutResult(status -> {
      batchMapper.insert(batch);
      bp.setStatus("generating");
      bp.setUpdatedAt(LocalDateTime.now());
      blueprintMapper.updateById(bp);
    });

    // 提交异步编排（AbortPolicy：池满抛 RejectedExecutionException → 503 由全局异常兜底）。
    // 此时写库已提交、行对外可见；拒绝路径的失败标记为无事务自动提交，不受外层事务影响。
    try {
      theoryGenExecutor.submit(() -> processBatch(batch.getId(), tenantId, userId, bp.getLevel()));
    } catch (RejectedExecutionException e) {
      batch.setStatus(TheoryGenBatchEntity.STATUS_FAILED);
      batch.setFailReason("服务繁忙，请稍后重试");
      batch.setUpdatedAt(LocalDateTime.now());
      batchMapper.updateById(batch);
      throw new BusinessException(ErrorCode.CONFLICT, "服务繁忙，请稍后重试");
    }
    return TheoryGenBatchVO.from(batch);
  }

  /**
   * GLM 模型配置校验（优化方案 v0.3 P0/R9）：仅 baseUrl 含 bigmodel 时生效。
   * GLM-5.3 系列不可关闭思考（官方：thinking.type 仅支持 enabled，传 disabled 报错）；
   * reasoning_effort 仅支持 max/high/low（非空才校验）。
   */
  private void validateGlmThinkingConfig(ExamFaqModelConfigEntity cfg) {
    String baseUrl = cfg.getBaseUrl();
    if (baseUrl == null || !baseUrl.toLowerCase().contains("bigmodel")) {
      return;
    }
    if ("disabled".equals(cfg.getThinking())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "GLM-5.3 系列不可关闭思考（thinking.type 仅支持 enabled），请调整模型配置");
    }
    String effort = cfg.getReasoningEffort();
    if (effort != null && !effort.isBlank()
        && !"max".equals(effort) && !"high".equals(effort) && !"low".equals(effort)) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "GLM reasoning_effort 仅支持 max/high/low，当前配置：" + effort);
    }
  }

  /**
   * 编排线程：批次编排（P3 并发分块 + 自动补跑）。每考评点一次 LLM 调用；暂停只挡新块（踩坑 #18）。
   * 全链路不调 SecurityContext；编排/Worker 线程各自 set/clear TenantContextHolder。
   *
   * <p>自动补跑：一轮派发结束后若仍有考评点缺草稿（限流/超时等瞬时失败），退避后自动再跑，
   * 最多 {@link #maxDispatchRounds} 轮；连续失败熔断每轮独立计数，触发即整体终止（系统性故障不再浪费调用）。
   * 轮次耗尽仍有缺口 → FAILED，"重试失败块"可人工兜底。
   */
  public void processBatch(Long batchId, Long tenantId, Long userId, int level) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      TheoryGenBatchEntity batch = batchMapper.selectById(batchId);
      if (batch == null) {
        log.error("理论生成批次不存在：batchId={}", batchId);
        return;
      }
      Long myRunId = batch.getRunId();
      batch.setStatus(TheoryGenBatchEntity.STATUS_RUNNING);
      batch.setStartedAt(LocalDateTime.now());
      batch.setUpdatedAt(LocalDateTime.now());
      batchMapper.updateById(batch);

      // 批次启动时读取一次并全程固化：base_url/api_key/model_id/thinking/reasoning_effort 均以此为准，
      // 改配置对下一个批次生效（进行中的批次保持参数一致，不受中途改库影响）
      ExamFaqModelConfigEntity cfg = modelConfigMapper.selectById(batch.getModelConfigId());
      if (cfg == null) {
        failBatch(batch, "模型配置不存在");
        return;
      }
      Map<String, Integer> quota = readJson(batch.getQuotaConfig());
      String systemPrompt = TheoryGenPromptBuilder.buildSystemPrompt(level, quota);
      List<TheoryBlueprintNodeEntity> points = listPoints(tenantId, batch.getBlueprintId());

      Semaphore slots = new Semaphore(Math.max(1, blockConcurrency));
      AtomicInteger consecutiveFailures = new AtomicInteger(0);
      // 熔断锁存：break 后 join 阶段在飞块可能成功并重置计数器，
      // 收尾判定必须用锁存标志而非实时计数（E2E 2096462034129305601 发现的竞态）
      boolean breakerTripped = false;
      int roundsUsed = 0;
      List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
      while (roundsUsed < maxDispatchRounds) {
        // 本轮待跑点 = 全部点 − 已有草稿点（断点续跑/自动补跑共用同一跳过机制）
        Set<Long> doneNodeIds = doneNodeIdsOf(batchId);
        List<TheoryBlueprintNodeEntity> pending = new ArrayList<>();
        for (TheoryBlueprintNodeEntity point : points) {
          if (!doneNodeIds.contains(point.getId())) {
            pending.add(point);
          }
        }
        if (pending.isEmpty()) {
          break; // 全部完成
        }
        if (roundsUsed > 0) {
          // 补跑退避：等待限流/超时等瞬时故障恢复；期间响应暂停与编排接替
          if (!backoffSleep(batchId, myRunId, roundsUsed)) {
            return;
          }
        }
        roundsUsed++;
        consecutiveFailures.set(0); // 每轮独立计数：上一轮散点失败不代表本轮系统性故障
        futures = new ArrayList<>();
        for (TheoryBlueprintNodeEntity point : pending) {
          // 派发前检查：状态 + run_id（R8，旧编排器读到新 run_id 自行退出）
          if (!dispatchAllowed(batchId, myRunId)) {
            log.info("批次已暂停/终止/被新编排接替，停止派发新块：batchId={}", batchId);
            return;
          }
          if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            log.warn("连续失败 {} 块，停止派发：batchId={}", MAX_CONSECUTIVE_FAILURES, batchId);
            breakerTripped = true;
            break;
          }
          try {
            slots.acquire();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failBatch(batch, "编排被中断");
            return;
          }
          // acquire 等待期间状态/熔断计数可能已变化，二次检查
          if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            slots.release();
            log.warn("连续失败 {} 块，停止派发：batchId={}", MAX_CONSECUTIVE_FAILURES, batchId);
            breakerTripped = true;
            break;
          }
          if (!dispatchAllowed(batchId, myRunId)) {
            slots.release();
            log.info("批次已暂停/终止/被新编排接替，停止派发新块：batchId={}", batchId);
            return;
          }
          try {
            futures.add(theoryGenBlockExecutor.submit(
                () -> {
                  try {
                    runBlock(batch, cfg, systemPrompt, point, quota, consecutiveFailures);
                  } finally {
                    slots.release();
                  }
                  return null;
                }));
          } catch (RejectedExecutionException e) {
            // 池满极端情况：释放名额并计失败（R7：限流/异常计块失败，重试失败块兜底）
            slots.release();
            log.warn("块任务提交被拒绝：batchId={}, point={}", batchId, point.getFullCode());
            batchMapper.incrementFailed(batchId, 0, 0, 0, 0);
            consecutiveFailures.incrementAndGet();
          }
        }
        // join 本轮全部在飞块（块任务自身不抛异常，get 仅兜底）
        for (java.util.concurrent.Future<?> f : futures) {
          try {
            f.get();
          } catch (Exception e) {
            log.error("块任务异常返回：batchId={}", batchId, e);
          }
        }
        if (breakerTripped) {
          break; // 系统性故障，不进入下一轮补跑
        }
      }
      // 收尾：仅当批次仍由本次编排持有且 RUNNING 时落终态
      TheoryGenBatchEntity finalState = batchMapper.selectById(batchId);
      if (finalState != null && TheoryGenBatchEntity.STATUS_RUNNING.equals(finalState.getStatus())
          && Objects.equals(myRunId, finalState.getRunId())) {
        Set<Long> finalDone = doneNodeIdsOf(batchId);
        if (finalDone.size() >= points.size()) {
          finalState.setStatus(TheoryGenBatchEntity.STATUS_DONE);
          finalState.setFinishedAt(LocalDateTime.now());
          finalState.setUpdatedAt(LocalDateTime.now());
          batchMapper.updateById(finalState);
          TheoryBlueprintEntity bp = blueprintMapper.selectById(finalState.getBlueprintId());
          if (bp != null) {
            bp.setStatus("done");
            bp.setUpdatedAt(LocalDateTime.now());
            blueprintMapper.updateById(bp);
          }
        } else if (breakerTripped) {
          // 熔断终止：存在未完成考评点，必须置 FAILED 以便"重试失败块"补缺口
          // （不能用实时计数判定——join 阶段在飞块的成功会把计数器清零，E2E 实测踩中）
          failBatch(finalState, "连续 " + MAX_CONSECUTIVE_FAILURES + " 个考评点失败，批次终止，可重试失败块");
        } else {
          failBatch(finalState, "自动补跑 " + roundsUsed + " 轮后仍有 "
              + (points.size() - finalDone.size()) + " 个考评点未完成，可重试失败块");
        }
      }
      log.info("理论生成批次完成：batchId={}, rounds={}, futures={}", batchId, roundsUsed, futures.size());
    } catch (Exception e) {
      log.error("理论生成批次异常终止：batchId={}", batchId, e);
      TheoryGenBatchEntity batch = batchMapper.selectById(batchId);
      if (batch != null) {
        failBatch(batch, abbreviate(e.getMessage(), 300));
      }
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** 派发许可：批次 RUNNING 且 run_id 与本次编排一致（R8 编排独占权） */
  private boolean dispatchAllowed(Long batchId, Long myRunId) {
    TheoryGenBatchEntity current = batchMapper.selectById(batchId);
    return current != null
        && TheoryGenBatchEntity.STATUS_RUNNING.equals(current.getStatus())
        && Objects.equals(myRunId, current.getRunId());
  }

  /** 本批次已有草稿的考评点集合（断点续跑/自动补跑/收尾判定共用） */
  private Set<Long> doneNodeIdsOf(Long batchId) {
    return draftMapper.selectList(new LambdaQueryWrapper<TheoryGenDraftEntity>()
            .eq(TheoryGenDraftEntity::getBatchId, batchId)
            .select(TheoryGenDraftEntity::getNodeId))
        .stream().map(TheoryGenDraftEntity::getNodeId).collect(Collectors.toSet());
  }

  /** 补跑退避：分片睡眠并持续监视暂停/编排接替；返回 false 表示应立即退出编排 */
  private boolean backoffSleep(Long batchId, Long myRunId, int round) {
    long remaining = Math.max(0, retryBackoffBaseMillis) * round;
    log.info("第 {} 轮补跑前退避 {}ms：batchId={}", round + 1, remaining, batchId);
    while (remaining > 0) {
      if (!dispatchAllowed(batchId, myRunId)) {
        return false;
      }
      long step = Math.min(1000, remaining);
      try {
        Thread.sleep(step);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
      remaining -= step;
    }
    return dispatchAllowed(batchId, myRunId);
  }

  /**
   * 块任务（theory-gen-block- Worker）：单考评点 LLM 生成 + 校验 + 草稿落库 + 计数。
   * 任何异常计失败不外抛（熔断计数按完成顺序，R7）；失败块的已产生 usage 同样累计（P2）。
   */
  private void runBlock(TheoryGenBatchEntity batch, ExamFaqModelConfigEntity cfg, String systemPrompt,
      TheoryBlueprintNodeEntity point, Map<String, Integer> quota, AtomicInteger consecutiveFailures) {
    Long tenantId = batch.getTenantId();
    TenantContextHolder.setTenantId(tenantId);
    // usage 提到 try 外：解析/校验失败时 LLM 花费已产生，同样计入批次（P2）
    LlmChatCompletionResult result = null;
    try {
      long t0 = System.currentTimeMillis();
      result = aiService.generateForPoint(tenantId, cfg.getBaseUrl(), cfg.getApiKey(),
          cfg.getModelId(), cfg.getThinking(), cfg.getReasoningEffort(),
          cfg.getMaxTokens() == null ? 4096 : cfg.getMaxTokens(),
          systemPrompt, TheoryGenPromptBuilder.buildUserPrompt(point, quota));
      List<TheoryQuestionItem> questions = aiService.parseQuestions(result.content());
      // 校验每题；任一题不合法 → 整块失败（零信任，不猜不兜底）
      for (TheoryQuestionItem q : questions) {
        List<String> errors = validator.validate(q);
        if (!errors.isEmpty()) {
          throw new BusinessException(ErrorCode.BAD_GATEWAY,
              "考评点 " + point.getFullCode() + " 产出不合法: " + String.join("；", errors));
        }
      }
      List<String> dup = validator.checkDuplicates(questions);
      if (!dup.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_GATEWAY,
            "考评点 " + point.getFullCode() + " 题干重复: " + String.join("；", dup));
      }
      // 答案字母分布均匀属确定性规则：校验通过后洗牌重排选项并同步改写答案字母
      // （prompt 禁令对 LLM 位置偏差无效，E2E 实测 D 选项仅 3%，AGENTS.md LLM 规范 #6）
      TheoryOptionShuffler.shuffle(questions, ThreadLocalRandom.current());
      saveDrafts(batch, cfg, point, questions, (int) (System.currentTimeMillis() - t0));
      batchMapper.incrementDone(batch.getId(), questions.size(), result.totalTokens(),
          result.promptTokens(), result.completionTokens(), result.cachedTokens());
      consecutiveFailures.set(0);
    } catch (Exception e) {
      log.warn("考评点生成失败：batchId={}, point={}, err={}",
          batch.getId(), point.getFullCode(), e.getMessage());
      if (result == null) {
        batchMapper.incrementFailed(batch.getId(), 0, 0, 0, 0);
      } else {
        batchMapper.incrementFailed(batch.getId(), result.totalTokens(), result.promptTokens(),
            result.completionTokens(), result.cachedTokens());
      }
      consecutiveFailures.incrementAndGet();
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** 草稿落库（Worker 线程；tenantId 已在 ThreadLocal）。块级事务：全落或全不落（P1/R4） */
  private void saveDrafts(TheoryGenBatchEntity batch, ExamFaqModelConfigEntity cfg,
      TheoryBlueprintNodeEntity point, List<TheoryQuestionItem> questions, int durationMs) {
    Long userId = batch.getCreatedBy();
    transactionTemplate.executeWithoutResult(status -> {
      for (TheoryQuestionItem q : questions) {
        TheoryGenDraftEntity d = new TheoryGenDraftEntity();
        d.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        d.setTenantId(batch.getTenantId());
        d.setBatchId(batch.getId());
        d.setBlueprintId(batch.getBlueprintId());
        d.setNodeId(point.getId());
        d.setQuestionType(q.getQuestionType());
        d.setTypeCode(TheoryGenPromptBuilder.typeCodeOf(q.getQuestionType()));
        d.setDifficulty(q.getDifficulty() == null ? 3 : q.getDifficulty());
        d.setConsistency(5);
        d.setContent(q.getContent());
        d.setOptions(serializeOptions(q));
        d.setAnswer(q.getAnswer() == null ? "" : normalizeAnswer(q));
        d.setAnalysis(q.getAnalysis());
        d.setReferenceAnswer(q.getReferenceAnswer());
        d.setScorePoints(q.getScorePoints());
        d.setScoringStandard(q.getScoringStandard());
        d.setScore(q.getScore());
        d.setAiModel(cfg.getModelId());
        d.setAiThinking(cfg.getThinking());
        d.setGenDurationMs(durationMs);
        d.setReviewStatus(TheoryGenDraftEntity.REVIEW_DRAFT);
        d.setPublishStatus(0);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        d.setCreatedBy(userId);
        d.setUpdatedBy(userId);
        d.setDeleted(0);
        draftMapper.insert(d);
      }
    });
  }

  /** 判断题答案归一：true/false → √/× */
  private String normalizeAnswer(TheoryQuestionItem q) {
    String a = q.getAnswer().trim();
    if ("judge".equals(q.getQuestionType())) {
      if (a.equalsIgnoreCase("true")) {
        return "√";
      }
      if (a.equalsIgnoreCase("false")) {
        return "×";
      }
    }
    return a.toUpperCase();
  }

  private String serializeOptions(TheoryQuestionItem q) {
    if (q.getOptionList() == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(q.getOptionList());
    } catch (Exception e) {
      return null;
    }
  }

  private void failBatch(TheoryGenBatchEntity batch, String reason) {
    batch.setStatus(TheoryGenBatchEntity.STATUS_FAILED);
    batch.setFailReason(reason);
    batch.setFinishedAt(LocalDateTime.now());
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
  }

  /** 暂停（HTTP 线程） */
  @Transactional
  public TheoryGenBatchVO pause(Long tenantId, Long batchId) {
    TheoryGenBatchEntity batch = getOwned(tenantId, batchId);
    if (!TheoryGenBatchEntity.STATUS_RUNNING.equals(batch.getStatus())
        && !TheoryGenBatchEntity.STATUS_PENDING.equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "批次不在运行中，无法暂停");
    }
    batch.setStatus(TheoryGenBatchEntity.STATUS_PAUSED);
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    return TheoryGenBatchVO.from(batch);
  }

  /** 继续（HTTP 线程 → 新 run_id 重新提交编排，断点续跑）。
   * 无 @Transactional：状态更新即时提交后才投递编排线程，避免 Worker 抢跑读到旧状态。 */
  public TheoryGenBatchVO resume(Long tenantId, Long userId, Long batchId) {
    TheoryGenBatchEntity batch = getOwned(tenantId, batchId);
    if (!TheoryGenBatchEntity.STATUS_PAUSED.equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "批次不在暂停状态，无法继续");
    }
    batch.setStatus(TheoryGenBatchEntity.STATUS_PENDING);
    batch.setRunId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    TheoryBlueprintEntity bp = blueprintMapper.selectById(batch.getBlueprintId());
    int level = bp != null && bp.getLevel() != null ? bp.getLevel() : 3;
    try {
      theoryGenExecutor.submit(() -> processBatch(batchId, tenantId, userId, level));
    } catch (RejectedExecutionException e) {
      batch.setStatus(TheoryGenBatchEntity.STATUS_PAUSED);
      batchMapper.updateById(batch);
      throw new BusinessException(ErrorCode.CONFLICT, "服务繁忙，请稍后重试");
    }
    return TheoryGenBatchVO.from(batch);
  }

  /** 重试缺口：失败批次或已完成但有缺口的批次均可重试（自动补跑轮次耗尽后的人工兜底）。
   * 无 @Transactional：状态更新即时提交后才投递编排线程，避免 Worker 抢跑读到旧状态。 */
  public TheoryGenBatchVO retryFailed(Long tenantId, Long userId, Long batchId) {
    TheoryGenBatchEntity batch = getOwned(tenantId, batchId);
    boolean failed = TheoryGenBatchEntity.STATUS_FAILED.equals(batch.getStatus());
    boolean doneWithGap = TheoryGenBatchEntity.STATUS_DONE.equals(batch.getStatus())
        && batch.getTotalPoints() != null && batch.getDonePoints() != null
        && batch.getDonePoints() < batch.getTotalPoints();
    if (!failed && !doneWithGap) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅失败批次或已完成但有缺口的批次可重试");
    }
    batch.setStatus(TheoryGenBatchEntity.STATUS_PENDING);
    batch.setRunId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    batch.setFailReason(null);
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    TheoryBlueprintEntity bp = blueprintMapper.selectById(batch.getBlueprintId());
    int level = bp != null && bp.getLevel() != null ? bp.getLevel() : 3;
    try {
      theoryGenExecutor.submit(() -> processBatch(batchId, tenantId, userId, level));
    } catch (RejectedExecutionException e) {
      batch.setStatus(TheoryGenBatchEntity.STATUS_FAILED);
      batchMapper.updateById(batch);
      throw new BusinessException(ErrorCode.CONFLICT, "服务繁忙，请稍后重试");
    }
    return TheoryGenBatchVO.from(batch);
  }

  /** 批次分页 */
  @Transactional(readOnly = true)
  public PageResponse<TheoryGenBatchVO> page(Long tenantId, Long blueprintId, long page, long pageSize) {
    LambdaQueryWrapper<TheoryGenBatchEntity> w = new LambdaQueryWrapper<TheoryGenBatchEntity>()
        .eq(TheoryGenBatchEntity::getTenantId, tenantId)
        .eq(blueprintId != null, TheoryGenBatchEntity::getBlueprintId, blueprintId)
        .orderByDesc(TheoryGenBatchEntity::getId);
    Page<TheoryGenBatchEntity> p = batchMapper.selectPage(new Page<>(page, pageSize), w);
    return new PageResponse<>(
        p.getRecords().stream().map(TheoryGenBatchVO::from).toList(),
        p.getCurrent(), p.getSize(), p.getTotal());
  }

  /** 批次详情/进度（前端轮询） */
  @Transactional(readOnly = true)
  public TheoryGenBatchVO getProgress(Long tenantId, Long batchId) {
    return TheoryGenBatchVO.from(getOwned(tenantId, batchId));
  }

  /** 启用中的模型配置（前端下拉推荐值） */
  @Transactional(readOnly = true)
  public List<com.guoyu.platform.exam.theorygen.dto.TheoryModelConfigVO> listEnabledModelConfigs(Long tenantId) {
    List<ExamFaqModelConfigEntity> list = modelConfigMapper.selectList(
        new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
            .eq(ExamFaqModelConfigEntity::getTenantId, tenantId)
            .eq(ExamFaqModelConfigEntity::getStatus, "enabled")
            .orderByDesc(ExamFaqModelConfigEntity::getId));
    return list == null ? List.of()
        : list.stream().map(com.guoyu.platform.exam.theorygen.dto.TheoryModelConfigVO::from).toList();
  }

  private TheoryGenBatchEntity getOwned(Long tenantId, Long batchId) {
    TheoryGenBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "生成批次不存在");
    }
    return batch;
  }

  private List<TheoryBlueprintNodeEntity> listPoints(Long tenantId, Long blueprintId) {
    return nodeMapper.selectList(new LambdaQueryWrapper<TheoryBlueprintNodeEntity>()
        .eq(TheoryBlueprintNodeEntity::getTenantId, tenantId)
        .eq(TheoryBlueprintNodeEntity::getBlueprintId, blueprintId)
        .eq(TheoryBlueprintNodeEntity::getNodeLevel, 4)
        .orderByAsc(TheoryBlueprintNodeEntity::getSortOrder));
  }

  private String writeJson(Map<String, Integer> quota) {
    try {
      return objectMapper.writeValueAsString(quota);
    } catch (Exception e) {
      return "{}";
    }
  }

  private Map<String, Integer> readJson(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() { });
    } catch (Exception e) {
      return new HashMap<>();
    }
  }

  private String abbreviate(String s, int max) {
    if (s == null) {
      return "";
    }
    // 预留 1 个字符给省略号，确保结果不超过 max（prompt_digest VARCHAR(200) 曾因 200+1 超长）
    return s.length() <= max ? s : s.substring(0, max - 1) + "…";
  }
}
