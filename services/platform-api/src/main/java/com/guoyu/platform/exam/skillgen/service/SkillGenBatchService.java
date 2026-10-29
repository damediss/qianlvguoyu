package com.guoyu.platform.exam.skillgen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import com.guoyu.platform.exam.skillgen.dto.SkillGenBatchVO;
import com.guoyu.platform.exam.skillgen.dto.SkillModelConfigVO;
import com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintNodeMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenBatchMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenDraftMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
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
 * 操作技能试题生成批次：提交（秒回）→ 异步并发分块（1 考评点 = 1 块，块内逐题 5 次串行 LLM 调用）→ 草稿落库。
 *
 * <p>线程边界与编排机制与理论链路一致（优化方案 v0.3 全部落地）：run_id 编排独占权、
 * 块级并发信号量、多轮自动补跑、熔断锁存、块级事务、断点续跑、写库先提交后投递；
 * tenantId/userId 参数传递，禁止调 SecurityContext，每 Worker 独立 set/clear TenantContextHolder。
 *
 * <p>单题拆分（2026-10-29）：块内由 {@link SkillGenAiService#generateForPointWithRepair} 逐题生成
 * （每次只要求 1 个 JSON 对象，规避 GLM 数组塌缩），块级编排与校验口径不变。
 *
 * <p>发起预检（S9/S10）：点数×5 + 该（工种,等级）累计已入库（全部来源） ≥ 等级下限，不足 400。
 */
@Service
@Slf4j
public class SkillGenBatchService {

  /** 熔断阈值：按块完成顺序连续失败 5 次停止派发 */
  private static final int MAX_CONSECUTIVE_FAILURES = 5;

  /**
   * 批内派发登记（同 JVM 跨编排器共享）：pause 后旧编排器返回时其在飞块仍在跑，
   * 快速 resume 会让新编排器按"现有草稿"计算缺口时漏掉这些在飞块 → 重复生成。
   * 派发时登记、失败时撤销、批终态清理；配合 saveDrafts 的库内去重双保险。
   */
  private static final java.util.Map<Long, Set<Long>> DISPATCHED_NODES =
      new java.util.concurrent.ConcurrentHashMap<>();

  @Value("${platform.skill.gen.max-dispatch-rounds:4}")
  private int maxDispatchRounds = 4;

  private final SkillBlueprintMapper blueprintMapper;
  private final SkillBlueprintNodeMapper nodeMapper;
  private final SkillGenBatchMapper batchMapper;
  private final SkillGenDraftMapper draftMapper;
  private final ExamFaqModelConfigMapper modelConfigMapper;
  private final SkillGenAiService aiService;
  private final SkillQuestionValidator validator;
  private final SkillBlueprintApplicationService blueprintService;
  private final ObjectMapper objectMapper;
  private final ThreadPoolTaskExecutor skillGenExecutor;
  private final ThreadPoolTaskExecutor skillGenBlockExecutor;
  private final TransactionTemplate transactionTemplate;
  private final int blockConcurrency;
  private final long retryBackoffBaseMillis;

  public SkillGenBatchService(SkillBlueprintMapper blueprintMapper,
      SkillBlueprintNodeMapper nodeMapper, SkillGenBatchMapper batchMapper,
      SkillGenDraftMapper draftMapper, ExamFaqModelConfigMapper modelConfigMapper,
      SkillGenAiService aiService, SkillQuestionValidator validator,
      SkillBlueprintApplicationService blueprintService, ObjectMapper objectMapper,
      @Qualifier("skillGenExecutor") ThreadPoolTaskExecutor skillGenExecutor,
      @Qualifier("skillGenBlockExecutor") ThreadPoolTaskExecutor skillGenBlockExecutor,
      TransactionTemplate transactionTemplate,
      @Value("${platform.skill.gen.block-concurrency:5}") int blockConcurrency,
      @Value("${platform.skill.gen.retry-backoff-base-millis:10000}") long retryBackoffBaseMillis) {
    this.blueprintMapper = blueprintMapper;
    this.nodeMapper = nodeMapper;
    this.batchMapper = batchMapper;
    this.draftMapper = draftMapper;
    this.modelConfigMapper = modelConfigMapper;
    this.aiService = aiService;
    this.validator = validator;
    this.blueprintService = blueprintService;
    this.objectMapper = objectMapper;
    this.skillGenExecutor = skillGenExecutor;
    this.skillGenBlockExecutor = skillGenBlockExecutor;
    this.transactionTemplate = transactionTemplate;
    this.blockConcurrency = blockConcurrency;
    this.retryBackoffBaseMillis = retryBackoffBaseMillis;
  }

  /** 提交生成批次（秒回 batchId；实际生成在 Worker 线程）。
   * 无 @Transactional：写库经 TransactionTemplate 先提交后才投递编排线程（生产事故教训）。 */
  public SkillGenBatchVO submit(Long tenantId, Long userId, Long blueprintId, Long modelConfigId) {
    SkillBlueprintEntity bp = blueprintMapper.selectById(blueprintId);
    if (bp == null || !bp.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    Long running = batchMapper.selectCount(new LambdaQueryWrapper<SkillGenBatchEntity>()
        .eq(SkillGenBatchEntity::getTenantId, tenantId)
        .eq(SkillGenBatchEntity::getBlueprintId, blueprintId)
        .in(SkillGenBatchEntity::getStatus,
            SkillGenBatchEntity.STATUS_PENDING, SkillGenBatchEntity.STATUS_RUNNING));
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

    List<SkillBlueprintNodeEntity> points = listPoints(tenantId, blueprintId);
    if (points.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "细目表无考评点，无法生成");
    }
    // 发起预检（S9/S10）：点数×5 + 累计已入库（全部来源） ≥ 等级下限，与入库闸门共用同一统计方法
    int floor = SkillBlueprintApplicationService.MIN_QUESTIONS.getOrDefault(bp.getLevel(), 50);
    long planned = (long) points.size() * SkillBlueprintApplicationService.PER_POINT_QUOTA;
    long cumulative = blueprintService
        .cumulativeStat(tenantId, bp.getWorkTypeId(), bp.getLevel()).total();
    if (planned + cumulative < floor) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, String.format(
          "该细目表按每点 %d 题可产 %d 道，与已入库 %d 道合计 %d 道，仍低于本等级题库总量下限 %d 道（还差 %d 道），"
              + "请补充考评点后再发起",
          SkillBlueprintApplicationService.PER_POINT_QUOTA, planned, cumulative,
          planned + cumulative, floor, floor - planned - cumulative));
    }

    Map<String, Integer> quota = Map.of("perPoint", SkillBlueprintApplicationService.PER_POINT_QUOTA);
    SkillGenBatchEntity batch = new SkillGenBatchEntity();
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
    batch.setQuotaConfig(writeJson(quota));
    batch.setStatus(SkillGenBatchEntity.STATUS_PENDING);
    batch.setRunId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    batch.setTotalPoints(points.size());
    batch.setDonePoints(0);
    batch.setFailedPoints(0);
    batch.setTotalQuestions(0);
    batch.setTokenUsage(0L);
    batch.setPromptTokens(0L);
    batch.setCompletionTokens(0L);
    batch.setCachedTokens(0L);
    batch.setPromptDigest(abbreviate(SkillGenPromptBuilder.buildSystemPrompt(bp.getLevel()), 200));
    batch.setCreatedBy(userId);
    batch.setUpdatedBy(userId);
    batch.setCreatedAt(LocalDateTime.now());
    batch.setUpdatedAt(LocalDateTime.now());
    batch.setDeleted(0);
    transactionTemplate.executeWithoutResult(status -> {
      batchMapper.insert(batch);
      bp.setStatus("generating");
      bp.setUpdatedAt(LocalDateTime.now());
      blueprintMapper.updateById(bp);
    });

    try {
      skillGenExecutor.submit(() -> processBatch(batch.getId(), tenantId, userId, bp.getLevel()));
    } catch (RejectedExecutionException e) {
      batch.setStatus(SkillGenBatchEntity.STATUS_FAILED);
      batch.setFailReason("服务繁忙，请稍后重试");
      batch.setUpdatedAt(LocalDateTime.now());
      batchMapper.updateById(batch);
      throw new BusinessException(ErrorCode.CONFLICT, "服务繁忙，请稍后重试");
    }
    return SkillGenBatchVO.from(batch);
  }

  /** GLM 配置校验：baseUrl 含 bigmodel 时 thinking 不可显式 disabled、effort ∈ {max,high,low} */
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

  /** 编排线程：并发分块 + 多轮自动补跑 + 熔断锁存 + 收尾终态（机制同理论链路） */
  public void processBatch(Long batchId, Long tenantId, Long userId, int level) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      SkillGenBatchEntity batch = batchMapper.selectById(batchId);
      if (batch == null) {
        log.error("操作技能生成批次不存在：batchId={}", batchId);
        return;
      }
      Long myRunId = batch.getRunId();
      batch.setStatus(SkillGenBatchEntity.STATUS_RUNNING);
      batch.setStartedAt(LocalDateTime.now());
      batch.setUpdatedAt(LocalDateTime.now());
      batchMapper.updateById(batch);

      ExamFaqModelConfigEntity cfg = modelConfigMapper.selectById(batch.getModelConfigId());
      if (cfg == null) {
        failBatch(batch, "模型配置不存在");
        return;
      }
      String systemPrompt = SkillGenPromptBuilder.buildSystemPrompt(level);
      List<SkillBlueprintNodeEntity> points = listPoints(tenantId, batch.getBlueprintId());
      // 全量节点一次载入：块内沿父链取一/二级名称、权重、选考方式与技能要求
      Map<Long, SkillBlueprintNodeEntity> nodeMap = new HashMap<>();
      for (SkillBlueprintNodeEntity n : blueprintService.findBlueprintNodes(tenantId, batch.getBlueprintId())) {
        nodeMap.put(n.getId(), n);
      }

      Semaphore slots = new Semaphore(Math.max(1, blockConcurrency));
      AtomicInteger consecutiveFailures = new AtomicInteger(0);
      boolean breakerTripped = false;
      int roundsUsed = 0;
      // 本批派发登记：跨编排器共享，防止旧编排器在飞块被新编排器重复派发（E2E 实测竞态）
      Set<Long> dispatched = DISPATCHED_NODES.computeIfAbsent(batchId,
          k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
      List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
      while (roundsUsed < maxDispatchRounds) {
        Set<Long> doneNodeIds = doneNodeIdsOf(batchId);
        List<SkillBlueprintNodeEntity> pending = pendingNodes(points, doneNodeIds, dispatched);
        if (pending.isEmpty()) {
          break;
        }
        if (roundsUsed > 0) {
          if (!backoffSleep(batchId, myRunId, roundsUsed)) {
            return;
          }
        }
        roundsUsed++;
        consecutiveFailures.set(0);
        futures = new ArrayList<>();
        for (SkillBlueprintNodeEntity point : pending) {
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
            dispatched.add(point.getId());
            futures.add(skillGenBlockExecutor.submit(
                () -> {
                  try {
                    runBlock(batch, cfg, systemPrompt, point, nodeMap, consecutiveFailures);
                  } finally {
                    slots.release();
                  }
                  return null;
                }));
          } catch (RejectedExecutionException e) {
            dispatched.remove(point.getId());
            slots.release();
            log.warn("块任务提交被拒绝：batchId={}, point={}", batchId, point.getFullCode());
            batchMapper.incrementFailed(batchId, 0, 0, 0, 0);
            consecutiveFailures.incrementAndGet();
          }
        }
        for (java.util.concurrent.Future<?> f : futures) {
          try {
            f.get();
          } catch (Exception e) {
            log.error("块任务异常返回：batchId={}", batchId, e);
          }
        }
        if (breakerTripped) {
          break;
        }
      }
      // 收尾：仅当批次仍由本次编排持有且 RUNNING 时落终态
      SkillGenBatchEntity finalState = batchMapper.selectById(batchId);
      if (finalState != null && SkillGenBatchEntity.STATUS_RUNNING.equals(finalState.getStatus())
          && Objects.equals(myRunId, finalState.getRunId())) {
        Set<Long> finalDone = doneNodeIdsOf(batchId);
        if (finalDone.size() >= points.size()) {
          finalState.setStatus(SkillGenBatchEntity.STATUS_DONE);
          finalState.setFinishedAt(LocalDateTime.now());
          finalState.setUpdatedAt(LocalDateTime.now());
          batchMapper.updateById(finalState);
          DISPATCHED_NODES.remove(batchId);
          SkillBlueprintEntity bp = blueprintMapper.selectById(finalState.getBlueprintId());
          if (bp != null) {
            bp.setStatus("done");
            bp.setUpdatedAt(LocalDateTime.now());
            blueprintMapper.updateById(bp);
          }
        } else if (breakerTripped) {
          DISPATCHED_NODES.remove(batchId);
          failBatch(finalState, "连续 " + MAX_CONSECUTIVE_FAILURES + " 个考评点失败，批次终止，可重试失败块");
        } else {
          DISPATCHED_NODES.remove(batchId);
          failBatch(finalState, "自动补跑 " + roundsUsed + " 轮后仍有 "
              + (points.size() - finalDone.size()) + " 个考评点未完成，可重试失败块");
        }
      }
      log.info("操作技能生成批次完成：batchId={}, rounds={}, futures={}", batchId, roundsUsed, futures.size());
    } catch (Exception e) {
      log.error("操作技能生成批次异常终止：batchId={}", batchId, e);
      SkillGenBatchEntity batch = batchMapper.selectById(batchId);
      if (batch != null) {
        failBatch(batch, abbreviate(e.getMessage(), 300));
      }
    } finally {
      TenantContextHolder.clear();
    }
  }

  /**
   * 待派发点 = 全部点 − 已有草稿点 − 已派发未回点（跨编排器在飞登记）。
   * 第三项专门防 pause→快速 resume 竞态：旧编排器在飞块尚未落草稿时，
   * 新编排器按"现有草稿"算缺口会把它们当作待派发 → 重复生成（E2E 实测 27/23）。
   */
  static List<SkillBlueprintNodeEntity> pendingNodes(List<SkillBlueprintNodeEntity> points,
      Set<Long> doneNodeIds, Set<Long> dispatched) {
    List<SkillBlueprintNodeEntity> pending = new ArrayList<>();
    for (SkillBlueprintNodeEntity point : points) {
      if (!doneNodeIds.contains(point.getId()) && !dispatched.contains(point.getId())) {
        pending.add(point);
      }
    }
    return pending;
  }

  private boolean dispatchAllowed(Long batchId, Long myRunId) {
    SkillGenBatchEntity current = batchMapper.selectById(batchId);
    return current != null
        && SkillGenBatchEntity.STATUS_RUNNING.equals(current.getStatus())
        && Objects.equals(myRunId, current.getRunId());
  }

  private Set<Long> doneNodeIdsOf(Long batchId) {
    return draftMapper.selectList(new LambdaQueryWrapper<SkillGenDraftEntity>()
            .eq(SkillGenDraftEntity::getBatchId, batchId)
            .select(SkillGenDraftEntity::getNodeId))
        .stream().map(SkillGenDraftEntity::getNodeId).collect(Collectors.toSet());
  }

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

  /** 块任务：单考评点 LLM 生成 + 校验 + 草稿落库 + 计数（任何异常计失败不外抛） */
  private void runBlock(SkillGenBatchEntity batch, ExamFaqModelConfigEntity cfg, String systemPrompt,
      SkillBlueprintNodeEntity point, Map<Long, SkillBlueprintNodeEntity> nodeMap,
      AtomicInteger consecutiveFailures) {
    Long tenantId = batch.getTenantId();
    TenantContextHolder.setTenantId(tenantId);
    SkillGenAiService.SkillGenGeneration gen = null;
    try {
      long t0 = System.currentTimeMillis();
      SkillBlueprintNodeEntity l2 = parentOf(point, nodeMap);
      SkillBlueprintNodeEntity l1 = l2 == null ? null : parentOf(l2, nodeMap);
      gen = aiService.generateForPointWithRepair(tenantId, cfg.getBaseUrl(), cfg.getApiKey(),
          cfg.getModelId(), cfg.getThinking(), cfg.getReasoningEffort(),
          cfg.getMaxTokens() == null ? 16384 : cfg.getMaxTokens(),
          systemPrompt, SkillGenPromptBuilder.buildBaseUserPrompt(point, l2, l1),
          SkillBlueprintApplicationService.PER_POINT_QUOTA);
      List<SkillQuestionItem> questions = gen.items();
      if (gen.attempts() > SkillBlueprintApplicationService.PER_POINT_QUOTA) {
        log.info("考评点单题拆分含重试完成：batchId={}, point={}, 总调用={}",
            batch.getId(), point.getFullCode(), gen.attempts());
      }
      List<String> errors = validator.validateAll(questions,
          SkillBlueprintApplicationService.PER_POINT_QUOTA);
      if (!errors.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_GATEWAY,
            "考评点 " + point.getFullCode() + " 产出不合法: " + String.join("；", errors));
      }
      boolean saved = saveDrafts(batch, cfg, point, l2, questions, (int) (System.currentTimeMillis() - t0));
      if (!saved) {
        // 该点已有草稿（重复派发的防重双保险）：不落库、不计数（另一块已计数）
        log.info("考评点已有草稿，跳过重复落库：batchId={}, point={}", batch.getId(), point.getFullCode());
        return;
      }
      batchMapper.incrementDone(batch.getId(), questions.size(), gen.totalTokens(),
          gen.promptTokens(), gen.completionTokens(), gen.cachedTokens());
      consecutiveFailures.set(0);
    } catch (Exception e) {
      log.warn("考评点生成失败：batchId={}, point={}, err={}",
          batch.getId(), point.getFullCode(), e.getMessage());
      // 失败撤销派发登记：允许补跑/重试重新派发该点
      Set<Long> dispatched = DISPATCHED_NODES.get(batch.getId());
      if (dispatched != null) {
        dispatched.remove(point.getId());
      }
      if (gen == null) {
        batchMapper.incrementFailed(batch.getId(), 0, 0, 0, 0);
      } else {
        batchMapper.incrementFailed(batch.getId(), gen.totalTokens(), gen.promptTokens(),
            gen.completionTokens(), gen.cachedTokens());
      }
      consecutiveFailures.incrementAndGet();
    } finally {
      TenantContextHolder.clear();
    }
  }

  private SkillBlueprintNodeEntity parentOf(SkillBlueprintNodeEntity node,
      Map<Long, SkillBlueprintNodeEntity> nodeMap) {
    Long pid = node.getParentId();
    return pid == null || pid == 0L ? null : nodeMap.get(pid);
  }

  /** 草稿落库（Worker 线程；块级事务：全落或全不落）。
   *  返回 false = 该点已有草稿（防重复派发的库内双保险），调用方不计数。 */
  boolean saveDrafts(SkillGenBatchEntity batch, ExamFaqModelConfigEntity cfg,
      SkillBlueprintNodeEntity point, SkillBlueprintNodeEntity l2,
      List<SkillQuestionItem> questions, int durationMs) {
    Long userId = batch.getCreatedBy();
    Boolean saved = transactionTemplate.execute(status -> {
      Long existing = draftMapper.selectCount(new LambdaQueryWrapper<SkillGenDraftEntity>()
          .eq(SkillGenDraftEntity::getBatchId, batch.getId())
          .eq(SkillGenDraftEntity::getNodeId, point.getId()));
      if (existing != null && existing > 0) {
        return false;
      }
      for (SkillQuestionItem q : questions) {
        SkillGenDraftEntity d = new SkillGenDraftEntity();
        d.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        d.setTenantId(batch.getTenantId());
        d.setBatchId(batch.getId());
        d.setBlueprintId(batch.getBlueprintId());
        d.setNodeId(point.getId());
        d.setQuestionName(abbreviate(q.getName(), 300));
        d.setScore(new java.math.BigDecimal(100));
        d.setWeight(l2 == null ? null : l2.getWeight());
        d.setDurationMinutes(q.getDurationMinutes());
        d.setExamForm(q.getExamForm() == null || q.getExamForm().isBlank() ? "实操" : q.getExamForm());
        d.setSitePreparation(writeJsonQuietly(q.getSitePrepare()));
        d.setExamineePrepare(q.getExamineePrepare());
        d.setRequirements(q.getRequirements());
        d.setNegatives(q.getNegatives());
        d.setScoringItems(writeJsonQuietly(q.getScoring()));
        d.setDifficulty(3);
        d.setConsistency(5);
        d.setAiModel(cfg.getModelId());
        d.setAiThinking(cfg.getThinking());
        d.setGenDurationMs(durationMs);
        d.setReviewStatus(SkillGenDraftEntity.REVIEW_DRAFT);
        d.setPublishStatus(0);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        d.setCreatedBy(userId);
        d.setUpdatedBy(userId);
        d.setDeleted(0);
        draftMapper.insert(d);
      }
      return true;
    });
    return Boolean.TRUE.equals(saved);
  }

  private void failBatch(SkillGenBatchEntity batch, String reason) {
    batch.setStatus(SkillGenBatchEntity.STATUS_FAILED);
    batch.setFailReason(reason);
    batch.setFinishedAt(LocalDateTime.now());
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
  }

  /** 暂停（HTTP 线程） */
  @Transactional
  public SkillGenBatchVO pause(Long tenantId, Long batchId) {
    SkillGenBatchEntity batch = getOwned(tenantId, batchId);
    if (!SkillGenBatchEntity.STATUS_RUNNING.equals(batch.getStatus())
        && !SkillGenBatchEntity.STATUS_PENDING.equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "批次不在运行中，无法暂停");
    }
    batch.setStatus(SkillGenBatchEntity.STATUS_PAUSED);
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    return SkillGenBatchVO.from(batch);
  }

  /** 继续（新 run_id 重新编排，断点续跑） */
  public SkillGenBatchVO resume(Long tenantId, Long userId, Long batchId) {
    SkillGenBatchEntity batch = getOwned(tenantId, batchId);
    if (!SkillGenBatchEntity.STATUS_PAUSED.equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "批次不在暂停状态，无法继续");
    }
    batch.setStatus(SkillGenBatchEntity.STATUS_PENDING);
    batch.setRunId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    SkillBlueprintEntity bp = blueprintMapper.selectById(batch.getBlueprintId());
    int level = bp != null && bp.getLevel() != null ? bp.getLevel() : 3;
    try {
      skillGenExecutor.submit(() -> processBatch(batchId, tenantId, userId, level));
    } catch (RejectedExecutionException e) {
      batch.setStatus(SkillGenBatchEntity.STATUS_PAUSED);
      batchMapper.updateById(batch);
      throw new BusinessException(ErrorCode.CONFLICT, "服务繁忙，请稍后重试");
    }
    return SkillGenBatchVO.from(batch);
  }

  /** 重试缺口：失败批次或已完成但有缺口的批次均可重试（自动补跑耗尽后的人工兜底） */
  public SkillGenBatchVO retryFailed(Long tenantId, Long userId, Long batchId) {
    SkillGenBatchEntity batch = getOwned(tenantId, batchId);
    boolean failed = SkillGenBatchEntity.STATUS_FAILED.equals(batch.getStatus());
    boolean doneWithGap = SkillGenBatchEntity.STATUS_DONE.equals(batch.getStatus())
        && batch.getTotalPoints() != null && batch.getDonePoints() != null
        && batch.getDonePoints() < batch.getTotalPoints();
    if (!failed && !doneWithGap) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅失败批次或已完成但有缺口的批次可重试");
    }
    batch.setStatus(SkillGenBatchEntity.STATUS_PENDING);
    batch.setRunId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    batch.setFailReason(null);
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    SkillBlueprintEntity bp = blueprintMapper.selectById(batch.getBlueprintId());
    int level = bp != null && bp.getLevel() != null ? bp.getLevel() : 3;
    try {
      skillGenExecutor.submit(() -> processBatch(batchId, tenantId, userId, level));
    } catch (RejectedExecutionException e) {
      batch.setStatus(SkillGenBatchEntity.STATUS_FAILED);
      batchMapper.updateById(batch);
      throw new BusinessException(ErrorCode.CONFLICT, "服务繁忙，请稍后重试");
    }
    return SkillGenBatchVO.from(batch);
  }

  /** 批次分页（status 逗号分隔：工作台进行中区置顶查询） */
  @Transactional(readOnly = true)
  public PageResponse<SkillGenBatchVO> page(Long tenantId, Long blueprintId, List<String> status,
      long page, long pageSize) {
    LambdaQueryWrapper<SkillGenBatchEntity> w = new LambdaQueryWrapper<SkillGenBatchEntity>()
        .eq(SkillGenBatchEntity::getTenantId, tenantId)
        .eq(blueprintId != null, SkillGenBatchEntity::getBlueprintId, blueprintId)
        .in(status != null && !status.isEmpty(), SkillGenBatchEntity::getStatus, status)
        .orderByDesc(SkillGenBatchEntity::getId);
    Page<SkillGenBatchEntity> p = batchMapper.selectPage(new Page<>(page, pageSize), w);
    Map<Long, SkillGenBatchVO.DraftStats> stats = draftStatsOf(
        p.getRecords().stream().map(SkillGenBatchEntity::getId).toList());
    return new PageResponse<>(
        p.getRecords().stream().map(b -> SkillGenBatchVO.from(b, stats.get(b.getId()))).toList(),
        p.getCurrent(), p.getSize(), p.getTotal());
  }

  private Map<Long, SkillGenBatchVO.DraftStats> draftStatsOf(List<Long> batchIds) {
    Map<Long, SkillGenBatchVO.DraftStats> out = new HashMap<>();
    if (batchIds.isEmpty()) {
      return out;
    }
    List<Map<String, Object>> rows = draftMapper.selectMaps(new QueryWrapper<SkillGenDraftEntity>()
        .select("batch_id", "review_status", "publish_status", "COUNT(*) AS cnt")
        .in("batch_id", batchIds)
        .groupBy("batch_id", "review_status", "publish_status"));
    Map<Long, int[]> agg = new HashMap<>();
    for (Map<String, Object> row : rows) {
      Long batchId = row.get("batch_id") instanceof Number n ? n.longValue() : null;
      if (batchId == null) {
        continue;
      }
      int cnt = row.get("cnt") instanceof Number n ? n.intValue() : 0;
      int[] c = agg.computeIfAbsent(batchId, k -> new int[4]);
      String review = String.valueOf(row.get("review_status"));
      switch (review) {
        case "approved" -> c[0] += cnt;
        case "rejected" -> c[2] += cnt;
        default -> c[1] += cnt;
      }
      if (row.get("publish_status") instanceof Number ps && ps.intValue() == 1) {
        c[3] += cnt;
      }
    }
    agg.forEach((id, c) -> out.put(id, new SkillGenBatchVO.DraftStats(c[0], c[1], c[2], c[3])));
    return out;
  }

  /** 批次进度（前端轮询） */
  @Transactional(readOnly = true)
  public SkillGenBatchVO getProgress(Long tenantId, Long batchId) {
    return SkillGenBatchVO.from(getOwned(tenantId, batchId));
  }

  /** 启用中的模型配置（前端下拉推荐值，默认选中 skill-gen-glm） */
  @Transactional(readOnly = true)
  public List<SkillModelConfigVO> listEnabledModelConfigs(Long tenantId) {
    List<ExamFaqModelConfigEntity> list = modelConfigMapper.selectList(
        new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
            .eq(ExamFaqModelConfigEntity::getTenantId, tenantId)
            .eq(ExamFaqModelConfigEntity::getStatus, "enabled")
            .in(ExamFaqModelConfigEntity::getFeatureScope, "common", "skill-gen")
            .orderByDesc(ExamFaqModelConfigEntity::getId));
    return list == null ? List.of() : list.stream().map(SkillModelConfigVO::from).toList();
  }

  private SkillGenBatchEntity getOwned(Long tenantId, Long batchId) {
    SkillGenBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "生成批次不存在");
    }
    return batch;
  }

  private List<SkillBlueprintNodeEntity> listPoints(Long tenantId, Long blueprintId) {
    return nodeMapper.selectList(new LambdaQueryWrapper<SkillBlueprintNodeEntity>()
        .eq(SkillBlueprintNodeEntity::getTenantId, tenantId)
        .eq(SkillBlueprintNodeEntity::getBlueprintId, blueprintId)
        .eq(SkillBlueprintNodeEntity::getNodeLevel, 3)
        .orderByAsc(SkillBlueprintNodeEntity::getSortOrder));
  }

  private String writeJson(Map<String, Integer> map) {
    try {
      return objectMapper.writeValueAsString(map);
    } catch (Exception e) {
      return "{}";
    }
  }

  private String writeJsonQuietly(Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      return null;
    }
  }

  private String abbreviate(String s, int max) {
    if (s == null) {
      return "";
    }
    return s.length() <= max ? s : s.substring(0, max - 1) + "…";
  }
}
