package com.guoyu.platform.exam.knowledgefix.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.knowledgefix.dto.FixBatchVO;
import com.guoyu.platform.exam.knowledgefix.dto.KnowledgeNodeRow;
import com.guoyu.platform.exam.knowledgefix.dto.PendingFixRow;
import com.guoyu.platform.exam.knowledgefix.entity.ExamDetailKnowledgeFixEntity;
import com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixBatchEntity;
import com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailKnowledgeFixMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamExamDetailFixMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamKnowledgeFixBatchMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamKnowledgeFixLlmLogMapper;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * 知识归属修正 LLM 分类批次（方案 §4.4 / Phase 3）。
 *
 * <p>线程边界：submit 走 HTTP 线程；processBatch 跑 knowledge-fix- 编排线程；单行 LLM 分类任务跑
 * knowledge-fix-llm- Worker（并发 platform.knowledgefix.llm-concurrency，默认 5）。
 * 全链路不调 SecurityContext；Worker 线程自行 set/clear TenantContextHolder。
 *
 * <p>准确率设计（无人工验收，方案 §4.4）：两阶段分类（L2 粗分 1 次 → L3 细分 3 采样自一致），
 * 2/3 多数 → apply/flag 分层；三方各不同 → 升级旗舰模型复判；确定性卫兵拦截零相关选择；
 * 模型选中当前节点 → keep（防过度修正）。
 */
@Service
@Slf4j
public class KnowledgeFixClassifyService {

  private static final String SYSTEM_PROMPT =
      "你是题库数据治理专家，负责判断试题归属的知识点。只输出 JSON：第一阶段输出 {\"l2_id\":<整数>}，"
          + "第二阶段输出 {\"node_id\":<整数>,\"confidence\":<0~1 小数>,\"reason\":\"不超过50字\"}。"
          + "必须从候选列表中选择，禁止输出候选之外的 id，禁止输出任何其它文字。";

  private static final String MAIN_MODEL_CODE = "theory-gen-glm";
  private static final String PRO_MODEL_CODE = "knowledge-fix-pro";
  private static final int MAX_DISPATCH_ROUNDS = 3;
  private static final int MAX_CONSECUTIVE_FAILURES = 5;
  private static final int SAMPLES = 3;
  private static final double APPLY_CONFIDENCE = 0.7;
  private static final int RESPONSE_TIMEOUT_SECONDS = 120;
  /** 自一致率门槛（FIX-1.4）：低于则批次判 failed，禁止 done */
  private static final BigDecimal SELF_CONSISTENCY_GATE = new BigDecimal("0.80");
  /** 未分类占比门槛（FIX-1.4）：高于则批次判 failed */
  private static final BigDecimal UNCLASSIFIED_RATIO_GATE = new BigDecimal("0.05");

  private final ExamKnowledgeFixBatchMapper batchMapper;
  private final ExamDetailKnowledgeFixMapper fixMapper;
  private final ExamExamDetailFixMapper queryMapper;
  /**
   * 人工暂停请求（FIX-10/v1.7）：batchNo 集合，编排线程在每行派发前检查。
   * 内存态（进程重启即失效，届时批次需按 §11.10.9 C 的 SQL 收尾）；在飞行的行不中断，跑完即收尾为 paused。
   */
  private final java.util.Set<Long> pauseRequests = java.util.concurrent.ConcurrentHashMap.newKeySet();
  private final ExamFaqModelConfigMapper modelConfigMapper;
  private final FaqLlmClient llmClient;
  private final ObjectMapper objectMapper;
  private final ThreadPoolTaskExecutor knowledgeFixExecutor;
  private final ThreadPoolTaskExecutor llmExecutor;
  private final int llmConcurrency;
  private final long retryBackoffBaseMillis;
  private final ExamKnowledgeFixLlmLogMapper llmLogMapper;

  public KnowledgeFixClassifyService(ExamKnowledgeFixBatchMapper batchMapper,
      ExamDetailKnowledgeFixMapper fixMapper, ExamExamDetailFixMapper queryMapper,
      ExamFaqModelConfigMapper modelConfigMapper, FaqLlmClient llmClient,
      ObjectMapper objectMapper,
      @Qualifier("knowledgeFixExecutor") ThreadPoolTaskExecutor knowledgeFixExecutor,
      @Qualifier("knowledgeFixLlmExecutor") ThreadPoolTaskExecutor llmExecutor,
      ExamKnowledgeFixLlmLogMapper llmLogMapper,
      @Value("${platform.knowledgefix.llm-concurrency:5}") int llmConcurrency,
      @Value("${platform.knowledgefix.retry-backoff-base-millis:5000}") long retryBackoffBaseMillis) {
    this.batchMapper = batchMapper;
    this.fixMapper = fixMapper;
    this.queryMapper = queryMapper;
    this.modelConfigMapper = modelConfigMapper;
    this.llmClient = llmClient;
    this.objectMapper = objectMapper;
    this.knowledgeFixExecutor = knowledgeFixExecutor;
    this.llmExecutor = llmExecutor;
    this.llmLogMapper = llmLogMapper;
    this.llmConcurrency = llmConcurrency;
    this.retryBackoffBaseMillis = retryBackoffBaseMillis;
  }

  /** 提交分类批次（秒回 batchNo；装配修正清单 + 异步编排） */
  public FixBatchVO submit(Long tenantId, Long userId) {
    Long running = batchMapper.selectCount(new LambdaQueryWrapper<ExamKnowledgeFixBatchEntity>()
        .in(ExamKnowledgeFixBatchEntity::getStatus,
            ExamKnowledgeFixBatchEntity.STATUS_PENDING, ExamKnowledgeFixBatchEntity.STATUS_RUNNING));
    if (running != null && running > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "已有进行中的分类批次");
    }
    ExamFaqModelConfigEntity cfgFlash = requireConfig(MAIN_MODEL_CODE);
    ExamFaqModelConfigEntity cfgPro = requireConfig(PRO_MODEL_CODE);
    requireApiKey(cfgFlash);
    requireApiKey(cfgPro);

    int populated = queryMapper.populateFixList();
    List<PendingFixRow> pending = fixMapper.selectPendingWithContent();
    if (pending.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "无待分类的修正行（可能已全部处理）");
    }

    ExamKnowledgeFixBatchEntity batch = new ExamKnowledgeFixBatchEntity();
    batch.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    batch.setTenantId(tenantId);
    batch.setBatchNo(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    batch.setTotal(pending.size());
    batch.setStatus(ExamKnowledgeFixBatchEntity.STATUS_PENDING);
    batch.setCreatedBy(userId);
    batch.setUpdatedBy(userId);
    batch.setCreatedAt(LocalDateTime.now());
    batch.setUpdatedAt(LocalDateTime.now());
    batch.setDeleted(0);
    batchMapper.insert(batch);

    long batchNo = batch.getBatchNo();
    knowledgeFixExecutor.submit(() -> processBatch(batch.getId(), batchNo, tenantId, userId,
        cfgFlash, cfgPro));
    log.info("知识修正批次已提交：batchNo={}, total={}, 新装入={}", batchNo, pending.size(), populated);
    return FixBatchVO.from(batch);
  }

  /**
   * 重跑通道（FIX-3/DEF-6）：把上一批 flag 行复位为 pending，使「提交分类批次」可再次完整执行。
   * 已写回（applied）与待写回（apply）行不动；有进行中的批次时拒绝。
   */
  public int resetClassify() {
    Long running = batchMapper.selectCount(new LambdaQueryWrapper<ExamKnowledgeFixBatchEntity>()
        .in(ExamKnowledgeFixBatchEntity::getStatus,
            ExamKnowledgeFixBatchEntity.STATUS_PENDING, ExamKnowledgeFixBatchEntity.STATUS_RUNNING));
    if (running != null && running > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "已有进行中的分类批次，不能重置");
    }
    int reset = fixMapper.resetFlagRows();
    log.info("知识修正重跑通道：flag 行复位 pending = {}", reset);
    return reset;
  }

  /** 待分类行数（重跑前预估，展示在页面） */
  public long pendingCount() {
    return fixMapper.countByStatus("pending");
  }

  /**
   * 人工暂停（FIX-10/v1.7）：登记暂停标志，编排线程在下一行派发边界停止派发新行；
   * 已派发（在飞行）的行跑完后收尾为 paused——未跑的行保持 pending，再次提交即可接着跑。
   * 继续 = 再点「提交 LLM 分类批次」（新批次只覆盖剩余 pending 行，已完成行结论不受影响）。
   */
  public long pause() {
    ExamKnowledgeFixBatchEntity batch = currentInFlight();
    if (batch == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前没有进行中的分类批次，无需暂停");
    }
    pauseRequests.add(batch.getBatchNo());
    log.info("知识修正批次暂停请求已登记：batchNo={}, done={}/{}", batch.getBatchNo(),
        batch.getDone(), batch.getTotal());
    return batch.getBatchNo();
  }

  /** 进行中的批次（pending/running，取最新一条）；无则返回 null */
  private ExamKnowledgeFixBatchEntity currentInFlight() {
    List<ExamKnowledgeFixBatchEntity> list = batchMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeFixBatchEntity>()
            .in(ExamKnowledgeFixBatchEntity::getStatus,
                ExamKnowledgeFixBatchEntity.STATUS_PENDING,
                ExamKnowledgeFixBatchEntity.STATUS_RUNNING)
            .orderByDesc(ExamKnowledgeFixBatchEntity::getId)
            .last("LIMIT 1"));
    return list == null || list.isEmpty() ? null : list.get(0);
  }

  /** 编排线程：两阶段分类 + 三采样自一致 + 旗舰升级 + 自动补跑（方案 §4.4/§6.4） */
  public void processBatch(Long batchId, Long batchNo, Long tenantId, Long userId,
      ExamFaqModelConfigEntity cfgFlash, ExamFaqModelConfigEntity cfgPro) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      ExamKnowledgeFixBatchEntity batch = batchMapper.selectById(batchId);
      if (batch == null) {
        log.error("知识修正批次不存在：batchId={}", batchId);
        return;
      }
      batch.setStatus(ExamKnowledgeFixBatchEntity.STATUS_RUNNING);
      batch.setUpdatedAt(LocalDateTime.now());
      batchMapper.updateById(batch);

      // 课程树快照（批次内不变数据一次性加载，踩坑 #18）
      Map<Integer, List<KnowledgeNodeRow>> courseL2 = new HashMap<>();
      Map<Integer, Map<Integer, List<KnowledgeNodeRow>>> courseL3ByParent = new HashMap<>();
      Map<Integer, KnowledgeNodeRow> nodeById = new HashMap<>();
      for (KnowledgeNodeRow n : queryMapper.selectAllNodes()) {
        nodeById.put(n.getId(), n);
        if (n.getLevel() != null && n.getLevel() == 2) {
          courseL2.computeIfAbsent(n.getCourseId(), x -> new ArrayList<>()).add(n);
        }
        if (n.getLevel() != null && n.getLevel() == 3) {
          courseL3ByParent.computeIfAbsent(n.getCourseId(), x -> new HashMap<>())
              .computeIfAbsent(n.getParentId(), x -> new ArrayList<>()).add(n);
        }
      }

      Semaphore slots = new Semaphore(Math.max(1, llmConcurrency));
      AtomicInteger consecutiveFailures = new AtomicInteger(0);
      AtomicInteger guardCnt = new AtomicInteger(0);
      int roundsUsed = 0;
      AtomicInteger classified = new AtomicInteger(0);
      AtomicInteger unanimous = new AtomicInteger(0);
      int totalDispatched = 0;
      boolean batchFailed = false;
      while (roundsUsed < MAX_DISPATCH_ROUNDS) {
        List<PendingFixRow> pending = fixMapper.selectPendingWithContent();
        if (pending.isEmpty()) {
          break;
        }
        if (roundsUsed > 0) {
          Thread.sleep(Math.max(0, retryBackoffBaseMillis) * roundsUsed);
        }
        roundsUsed++;
        consecutiveFailures.set(0);
        // FIX-1/DEF-2：熔断只终止"本轮继续派发"，不放弃整批——本轮结束后进入下一轮补跑
        boolean breakerTrippedThisRound = false;
        // FIX-10/v1.7：人工暂停——只停"继续派发"，在飞行已提交的行走完
        boolean pauseTripped = false;
        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (PendingFixRow row : pending) {
          if (pauseRequests.contains(batchNo)) {
            pauseTripped = true;
            break;
          }
          if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            breakerTrippedThisRound = true;
            break;
          }
          try {
            slots.acquire();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failBatch(batch, "编排被中断");
            return;
          }
          try {
            futures.add(llmExecutor.submit(() -> {
              try {
                classifyRow(row, batchNo, tenantId, cfgFlash, cfgPro, courseL2, courseL3ByParent,
                    nodeById, consecutiveFailures, guardCnt, classified, unanimous);
              } finally {
                slots.release();
              }
              return null;
            }));
          } catch (RejectedExecutionException e) {
            slots.release();
            log.warn("分类任务提交被拒绝：batchNo={}, detailId={}", batchNo, row.getDetailId());
            batchMapper.incrementFailed(batchNo);
            consecutiveFailures.incrementAndGet();
          }
        }
        for (java.util.concurrent.Future<?> f : futures) {
          try {
            f.get();
          } catch (Exception e) {
            log.error("分类任务异常返回：batchNo={}", batchNo, e);
          }
        }
        if (pauseTripped) {
          finalizePaused(batchId, batchNo);
          return;
        }
        totalDispatched += futures.size();
        if (futures.isEmpty()) {
          // 一条都派不出去（如首行即触发熔断）→ 无法继续，按失败收尾
          batchFailed = true;
          break;
        }
        if (breakerTrippedThisRound) {
          log.warn("批次本轮熔断：batchNo={}, round={}, 已派发={}，进入下一轮补跑",
              batchNo, roundsUsed, futures.size());
        }
      }
      // 收尾未分类行（FIX-1/DEF-3）：写 basis=unclassified 与"补跑后仍失败"分列，且不计入 done
      List<PendingFixRow> remain = fixMapper.selectPendingWithContent();
      int unclassified = remain.size();
      for (PendingFixRow row : remain) {
        fixMapper.updateClassifyResult(row.getRowId(), row.getOldKnowledgeId(), BigDecimal.ZERO,
            "unclassified", "批次中断未分类", ExamDetailKnowledgeFixEntity.STATUS_FLAG);
      }
      if (unclassified > 0) {
        batchMapper.incrementUnclassified(batchNo, unclassified);
      }
      // 收尾（FIX-1.4 质量门禁）：未分类占比 >5% 或自一致率 <0.8 → failed，禁止 done
      ExamKnowledgeFixBatchEntity finalState = batchMapper.selectById(batchId);
      if (finalState != null
          && !ExamKnowledgeFixBatchEntity.STATUS_FAILED.equals(finalState.getStatus())) {
        int total = finalState.getTotal() == null ? 0 : finalState.getTotal();
        // 未分类 = 收尾时仍未获得结论的行数（failed 是"尝试失败次数"，可能因重试大于行数，不能相减）
        int unclassifiedCount = unclassified;
        BigDecimal rate = classified.get() > 0
            ? BigDecimal.valueOf(unanimous.get()).divide(BigDecimal.valueOf(classified.get()),
                4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        finalState.setSelfConsistencyRate(rate);
        finalState.setUpdatedAt(LocalDateTime.now());
        boolean rateBelowGate = rate.compareTo(SELF_CONSISTENCY_GATE) < 0;
        boolean unclassifiedAboveGate =
            total > 0 && BigDecimal.valueOf(unclassifiedCount)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .compareTo(UNCLASSIFIED_RATIO_GATE) > 0;
        if (batchFailed || rateBelowGate || unclassifiedAboveGate) {
          finalState.setStatus(ExamKnowledgeFixBatchEntity.STATUS_FAILED);
          finalState.setFailReason(buildGateReason(batchFailed, unclassifiedCount, total, rate));
        } else {
          finalState.setStatus(ExamKnowledgeFixBatchEntity.STATUS_DONE);
        }
        batchMapper.updateById(finalState);
      }
      log.info("知识修正批次结束：batchNo={}, rounds={}, dispatched={}, classified={}, "
              + "unanimous={}, guard={}, unclassified={}, status={}",
          batchNo, roundsUsed, totalDispatched, classified.get(), unanimous.get(), guardCnt.get(),
          unclassified, finalState == null ? "?" : finalState.getStatus());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("知识修正批次被中断：batchNo={}", batchNo);
      ExamKnowledgeFixBatchEntity batch = batchMapper.selectById(batchId);
      if (batch != null) {
        failBatch(batch, "编排被中断");
      }
    } catch (Exception e) {
      log.error("知识修正批次异常终止：batchNo={}", batchNo, e);
      ExamKnowledgeFixBatchEntity batch = batchMapper.selectById(batchId);
      if (batch != null) {
        failBatch(batch, e.getMessage() == null ? "未知异常" : e.getMessage());
      }
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** 单行分类：两阶段（L2 粗分 → L3 细分×3 自一致）+ 旗舰升级 + 卫兵（KF-09~15） */
  private void classifyRow(PendingFixRow row, Long batchNo, Long tenantId,
      ExamFaqModelConfigEntity cfgFlash, ExamFaqModelConfigEntity cfgPro,
      Map<Integer, List<KnowledgeNodeRow>> courseL2,
      Map<Integer, Map<Integer, List<KnowledgeNodeRow>>> courseL3ByParent,
      Map<Integer, KnowledgeNodeRow> nodeById, AtomicInteger consecutiveFailures,
      AtomicInteger guardCnt, AtomicInteger classified, AtomicInteger unanimous)
      throws InterruptedException {
    TenantContextHolder.setTenantId(tenantId);
    try {
      Integer courseId = row.getCourseId();
      List<KnowledgeNodeRow> l2List = courseL2.getOrDefault(courseId, List.of());
      Map<Integer, List<KnowledgeNodeRow>> l3Map = courseL3ByParent.getOrDefault(courseId, Map.of());
      String questionText = buildQuestionText(row.getContent(), row.getAnswer(), row.getResolve());
      KnowledgeNodeRow oldNode = nodeById.get(row.getOldKnowledgeId());

      // 阶段 1：L2 粗分（1 次）
      String s1 = callLlm(cfgFlash, tenantId, batchNo, row.getDetailId(), "stage1",
          buildStage1Prompt(questionText, l2List), 1024).content();
      int l2Id = extractIntField(s1, "l2_id");
      boolean l2Valid = l2List.stream().anyMatch(n -> n.getId() != null && n.getId() == l2Id);
      if (!l2Valid) {
        throw new BusinessException(ErrorCode.BAD_GATEWAY, "L2 越界: " + l2Id);
      }
      List<KnowledgeNodeRow> l3List = l3Map.getOrDefault(l2Id, List.of());
      if (l3List.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_GATEWAY, "L2 下无 L3 鉴定点: " + l2Id);
      }

      // 阶段 2：三采样自一致
      // FIX-11/v1.7：候选顺序每行随机一次（种子=detailId），3 次采样共用同一顺序——
      // 第 2、3 次采样 prompt 与第 1 次逐字节一致，可整段命中前缀缓存（同课程行亦共享候选清单前缀）。
      // 代价：行内位置偏差检测退化为行间（全批顺序仍随机）；位置偏差兜底 = 确定性卫兵。
      List<KnowledgeNodeRow> ordered = new ArrayList<>(l3List);
      Collections.shuffle(ordered, new java.util.Random(row.getDetailId()));
      List<Integer> nodeVotes = new ArrayList<>();
      List<Double> confVotes = new ArrayList<>();
      List<String> reasons = new ArrayList<>();
      for (int i = 0; i < SAMPLES; i++) {
        String s2 = callLlm(cfgFlash, tenantId, batchNo, row.getDetailId(), "stage2",
            buildStage2Prompt(questionText, ordered, oldNode), 1024).content();
        JsonNode json = parseObject(s2);
        int nodeId = json.path("node_id").asInt(-1);
        double conf = json.path("confidence").asDouble(0);
        String reason = json.path("reason").asText("");
        KnowledgeNodeRow picked = nodeById.get(nodeId);
        if (picked == null || !Objects.equals(picked.getCourseId(), courseId)) {
          throw new BusinessException(ErrorCode.BAD_GATEWAY, "node_id 越界: " + nodeId);
        }
        nodeVotes.add(nodeId);
        confVotes.add(conf);
        reasons.add(reason);
      }
      classified.incrementAndGet();
      // 多数投票
      Map<Integer, Integer> counts = new HashMap<>();
      for (int v : nodeVotes) {
        counts.merge(v, 1, Integer::sum);
      }
      int majorityNode = -1;
      int majorityCount = 0;
      for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
        if (e.getValue() > majorityCount) {
          majorityCount = e.getValue();
          majorityNode = e.getKey();
        }
      }
      String outcome;
      int finalNodeId;
      double finalConf;
      String basis = "llm";
      String reason;
      if (majorityCount >= 2) {
        finalNodeId = majorityNode;
        double avg = 0;
        for (int i = 0; i < SAMPLES; i++) {
          if (nodeVotes.get(i) == majorityNode) {
            avg += confVotes.get(i);
          }
        }
        finalConf = avg / majorityCount;
        if (majorityCount == SAMPLES) {
          unanimous.incrementAndGet();
        }
        reason = reasons.get(nodeVotes.indexOf(majorityNode) >= 0
            ? nodeVotes.indexOf(majorityNode) : 0);
      } else {
        // 三方各不同 → 旗舰复判（basis=llm-pro）
        String s3 = callLlm(cfgPro, tenantId, batchNo, row.getDetailId(), "escalate",
            buildStage2Prompt(questionText, ordered, oldNode), 2048).content();
        JsonNode json = parseObject(s3);
        finalNodeId = json.path("node_id").asInt(-1);
        finalConf = json.path("confidence").asDouble(0);
        reason = "旗舰复判:" + json.path("reason").asText("");
        basis = "llm-pro";
        if (nodeById.get(finalNodeId) == null) {
          throw new BusinessException(ErrorCode.BAD_GATEWAY, "旗舰复判 node_id 越界");
        }
      }

      KnowledgeNodeRow chosen = nodeById.get(finalNodeId);
      // keep：模型选中当前节点（防过度修正）
      if (Objects.equals(finalNodeId, row.getOldKnowledgeId())) {
        fixMapper.updateClassifyResult(row.getRowId(), row.getOldKnowledgeId(),
            BigDecimal.valueOf(finalConf).setScale(3, RoundingMode.HALF_UP), basis, reason,
            ExamDetailKnowledgeFixEntity.STATUS_KEEP);
        batchMapper.incrementClassified(batchNo, "keep");
        consecutiveFailures.set(0);
        return;
      }
      // 确定性卫兵：零关键词重合且非素养节点 → flag（FIX-1/DEF-1：卫兵拦截属业务判定，
      // 不计入连续失败，避免与真失败凑满阈值误触发熔断）
      if (!KnowledgeNormUtil.hasBigramOverlap(chosen.getTitle(), questionText)
          && !KnowledgeNormUtil.isQualityNode(chosen.getTitle())) {
        fixMapper.updateClassifyResult(row.getRowId(), finalNodeId,
            BigDecimal.valueOf(finalConf).setScale(3, RoundingMode.HALF_UP), basis,
            "卫兵拦截:" + reason, ExamDetailKnowledgeFixEntity.STATUS_FLAG);
        batchMapper.incrementClassified(batchNo, "flag");
        guardCnt.incrementAndGet();
        return;
      }
      // 置信分层
      if (finalConf >= APPLY_CONFIDENCE) {
        outcome = ExamDetailKnowledgeFixEntity.STATUS_APPLY;
      } else {
        outcome = ExamDetailKnowledgeFixEntity.STATUS_FLAG;
      }
      fixMapper.updateClassifyResult(row.getRowId(), finalNodeId,
          BigDecimal.valueOf(finalConf).setScale(3, RoundingMode.HALF_UP), basis, reason, outcome);
      batchMapper.incrementClassified(batchNo, outcome);
      consecutiveFailures.set(0);
    } catch (Exception e) {
      log.warn("知识修正分类失败：detailId={}, err={}", row.getDetailId(), e.getMessage());
      batchMapper.incrementFailed(batchNo);
      consecutiveFailures.incrementAndGet();
    } finally {
      TenantContextHolder.clear();
    }
  }

  private void failBatch(ExamKnowledgeFixBatchEntity batch, String reason) {
    batch.setStatus(ExamKnowledgeFixBatchEntity.STATUS_FAILED);
    batch.setFailReason(reason);
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
  }

  /**
   * 人工暂停收尾（FIX-10/v1.7）：不写"批次中断未分类"兜底（剩余行保持 pending，供下次提交接着跑），
   * 批次置 paused。跳过质量门禁——暂停是人工控制而非质量判定。
   */
  private void finalizePaused(Long batchId, Long batchNo) {
    pauseRequests.remove(batchNo);
    ExamKnowledgeFixBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null) {
      return;
    }
    int total = batch.getTotal() == null ? 0 : batch.getTotal();
    int done = batch.getDone() == null ? 0 : batch.getDone();
    batch.setStatus(ExamKnowledgeFixBatchEntity.STATUS_PAUSED);
    batch.setFailReason("人工暂停：已分类 " + done + "/" + total + "，剩余 "
        + Math.max(0, total - done) + " 行保持待分类；再次点「提交 LLM 分类批次」即可接着跑");
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    log.info("知识修正批次已暂停：batchNo={}, done={}/{}", batchNo, done, total);
  }

  /** 质量门禁说明（FIX-1.4）：写清楚未达标原因，供页面红色提示与事后复核 */
  private static String buildGateReason(boolean noDispatch, int unclassified, int total,
      BigDecimal rate) {
    StringBuilder sb = new StringBuilder("未达质量门禁：");
    if (noDispatch) {
      sb.append("本轮一条都未能派发；");
    }
    if (total > 0 && unclassified > 0) {
      sb.append("未分类 ").append(unclassified).append("/").append(total)
          .append("（上限 5%）；");
    }
    if (rate.compareTo(SELF_CONSISTENCY_GATE) < 0) {
      sb.append("自一致率 ").append(rate).append("（下限 0.80）；");
    }
    sb.append("未分类行可经 reset 后重跑");
    return sb.toString();
  }

  /** LLM 调用 + 审计落库（LLM 规范 #10，KF-19）：usage/耗时/摘要写入审计表，落库失败不影响主流程 */
  private com.guoyu.platform.ai.dto.LlmChatCompletionResult callLlm(
      ExamFaqModelConfigEntity cfg, Long tenantId, Long batchNo, Integer detailId,
      String stage, String prompt, int maxTokens) {
    long t0 = System.currentTimeMillis();
    var result = llmClient.chatCompletionDetailed(cfg.getBaseUrl(), cfg.getApiKey(),
        cfg.getModelId(), SYSTEM_PROMPT, prompt, maxTokens, cfg.getThinking(),
        cfg.getReasoningEffort(), false, tenantId, RESPONSE_TIMEOUT_SECONDS);
    int duration = (int) (System.currentTimeMillis() - t0);
    try {
      ExamKnowledgeFixLlmLogEntity logRow = new ExamKnowledgeFixLlmLogEntity();
      logRow.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
      logRow.setTenantId(tenantId);
      logRow.setBatchNo(batchNo);
      logRow.setDetailId(detailId);
      logRow.setStage(stage);
      logRow.setModelId(cfg.getModelId());
      // FIX-11：题干已移至 prompt 末尾，摘要改存末尾 200 字（审计排障用）
      logRow.setPromptDigest(prompt.length() > 200 ? prompt.substring(prompt.length() - 200) : prompt);
      logRow.setPromptTokens((int) result.promptTokens());
      logRow.setCompletionTokens((int) result.completionTokens());
      logRow.setTotalTokens((int) result.totalTokens());
      logRow.setCachedTokens((int) result.cachedTokens());
      logRow.setDurationMs(duration);
      logRow.setCreatedAt(LocalDateTime.now());
      logRow.setCreatedBy(0L);
      logRow.setUpdatedBy(0L);
      logRow.setDeleted(0);
      llmLogMapper.insert(logRow);
    } catch (Exception logEx) {
      log.warn("LLM 审计落库失败（不影响主流程）：{}", logEx.getMessage());
    }
    return result;
  }

  private String buildQuestionText(String content, String answer, String resolve) {
    StringBuilder sb = new StringBuilder();
    sb.append(stripTags(content));
    if (answer != null && !answer.isBlank()) {
      sb.append("\n选项与答案：").append(stripTags(answer));
    }
    if (resolve != null && !resolve.isBlank()) {
      sb.append("\n解析：").append(stripTags(resolve));
    }
    return sb.toString();
  }

  private String stripTags(String s) {
    if (s == null) {
      return "";
    }
    return s.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim();
  }

  /**
   * 阶段 1 prompt。FIX-11/v1.7 稳定段前置：候选清单 + 输出要求在前（同课程各行共享同一前缀，
   * 便于前缀缓存命中），【试题】放末尾（行级变量）。
   */
  private String buildStage1Prompt(String questionText, List<KnowledgeNodeRow> l2List) {
    StringBuilder sb = new StringBuilder();
    sb.append("【候选分类（L2）】\n");
    for (KnowledgeNodeRow n : l2List) {
      sb.append(n.getId()).append("|").append(n.getTitle()).append("\n");
    }
    sb.append("请从候选中选出该试题所属的 L2 分类，只输出 JSON：{\"l2_id\":<整数>}。禁止输出候选之外的 id，禁止输出任何其它文字。\n\n");
    sb.append("【试题】\n").append(questionText);
    return sb.toString();
  }

  /**
   * 阶段 2 prompt。FIX-11/v1.7 稳定段前置（同 buildStage1Prompt）；
   * 【当前标记】属行级变量，随【试题】放末尾。
   */
  private String buildStage2Prompt(String questionText, List<KnowledgeNodeRow> l3List,
      KnowledgeNodeRow oldNode) {
    StringBuilder sb = new StringBuilder();
    sb.append("【候选鉴定点（L3）】\n");
    for (KnowledgeNodeRow n : l3List) {
      sb.append(n.getId()).append("|").append(n.getTitle());
      if (n.getImportant() != null && !n.getImportant().isBlank()) {
        sb.append("（重要程度 ").append(n.getImportant()).append("）");
      }
      sb.append("\n");
    }
    sb.append("请从候选中选出该试题真正归属的鉴定点，只输出 JSON：")
        .append("{\"node_id\":<整数>,\"confidence\":<0~1 小数>,\"reason\":\"不超过50字\"}。")
        .append("禁止输出候选之外的 id，禁止输出任何其它文字。\n\n");
    sb.append("【试题】\n").append(questionText);
    if (oldNode != null) {
      sb.append("\n【当前标记】该试题当前挂在「").append(oldNode.getTitle())
          .append("」，若试题确实属于它，请选择它（node_id=").append(oldNode.getId()).append("）。");
    }
    return sb.toString();
  }

  private JsonNode parseObject(String content) {
    try {
      String t = content == null ? "" : content.trim();
      int start = t.indexOf('{');
      int end = t.lastIndexOf('}');
      if (start < 0 || end <= start) {
        throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出中未找到 JSON 对象");
      }
      return objectMapper.readTree(t.substring(start, end + 1));
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "模型输出 JSON 解析失败: " + e.getMessage());
    }
  }

  private int extractIntField(String content, String field) {
    JsonNode json = parseObject(content);
    return json.path(field).asInt(-1);
  }


  private List<PendingFixRow> selectPending() {
    return fixMapper.selectPendingWithContent();
  }

  private ExamFaqModelConfigEntity requireConfig(String code) {
    List<ExamFaqModelConfigEntity> list = modelConfigMapper.selectList(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamFaqModelConfigEntity>()
            .eq(ExamFaqModelConfigEntity::getModelCode, code)
            .eq(ExamFaqModelConfigEntity::getStatus, "enabled"));
    if (list == null || list.isEmpty()) {
      throw new com.guoyu.platform.common.exception.BusinessException(
          com.guoyu.platform.common.exception.ErrorCode.NOT_FOUND, "模型配置不存在或未启用: " + code);
    }
    return list.get(0);
  }

  private void requireApiKey(ExamFaqModelConfigEntity cfg) {
    if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()
        || "PENDING_USER_INPUT".equals(cfg.getApiKey())) {
      throw new com.guoyu.platform.common.exception.BusinessException(
          com.guoyu.platform.common.exception.ErrorCode.CONFLICT,
          "模型 api_key 未配置，请先在模型配置中回填: " + cfg.getModelCode());
    }
  }
}
