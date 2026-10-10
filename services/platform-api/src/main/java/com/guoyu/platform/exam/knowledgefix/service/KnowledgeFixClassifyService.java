package com.guoyu.platform.exam.knowledgefix.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.infrastructure.JevDecisionClient;
import com.guoyu.platform.ai.infrastructure.JevDecisionException;
import com.guoyu.platform.ai.infrastructure.JevUnauthorizedException;
import com.guoyu.platform.ai.infrastructure.JevValidationException;
import com.guoyu.platform.ai.dto.JevDecisionResult;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
  /** Jev 决策模型配置编码（方案 §8.2；配置行 status≠enabled/缺失 = 通道关闭，安静全走 LLM） */
  private static final String JEV_MODEL_CODE = "knowledge-fix-jev";
  /** Jev criteria 最小选项数（接口实测 min_length=2，空/单选项必 422 → 前置拦截） */
  private static final int MIN_JEV_CRITERIA = 2;
  /** Jev criteria 最大选项数（接口实测 255 通过 / 256 被拒 422 → 前置拦截） */
  private static final int MAX_JEV_CRITERIA = 255;
  /** θ₂ 硬底线（§7.1）：配置低于 0.3 拒绝提交 */
  private static final BigDecimal JEV_MIN_THETA2 = new BigDecimal("0.3");
  /**
   * Jev 422 细分标记（v1.4 补充，2026-10-04 生产实测）：TokenDance 对候选做展开后校验
   * expanded_input_tokens ≤ 32,768——候选过多（实测 53 个 L2 全量展开 33,480）必 422。
   * 该类 422 该行由 LLM 可正常分类 → 降级 LLM；其余 422 = 请求构造 bug → SKIP_ROW（§5）。
   */
  private static final String JEV_BUDGET_EXCEEDED_MARKER = "token_budget_exceeded";
  private static final BigDecimal DEFAULT_JEV_THETA = new BigDecimal("0.5");
  private static final int DEFAULT_JEV_CONCURRENCY = 5;
  private static final int DEFAULT_JEV_READ_TIMEOUT_SECONDS = 30;
  private static final int MAX_DISPATCH_ROUNDS = 3;
  private static final int MAX_CONSECUTIVE_FAILURES = 5;
  private static final int SAMPLES = 3;
  private static final double APPLY_CONFIDENCE = 0.7;
  private static final int RESPONSE_TIMEOUT_SECONDS = 120;
  /** 未分类占比门槛（FIX-1.4）：高于则批次判 failed（§6 调整后唯一门禁；自一致率降为信息项） */
  private static final BigDecimal UNCLASSIFIED_RATIO_GATE = new BigDecimal("0.05");
  /** L2 直收尾固定置信度（业务裁定"题可挂 L2"，无三采样）——0.5 中性值，basis=l2-direct */
  private static final BigDecimal L2_DIRECT_CONFIDENCE = new BigDecimal("0.500");

  /**
   * 确定性守卫拒绝（模型输出越界等校验失败）：业务判定而非服务故障——
   * 不计入失败重试次数，也不计入熔断连续失败（A2；重试同样会被拒，凑满阈值会误杀整批）。
   */
  private static class GuardRejectException extends BusinessException {

    GuardRejectException(String message) {
      super(ErrorCode.BAD_GATEWAY, message);
    }
  }

  /** Jev 通道运行时配置（submit 时解析固化，改配置对下一批次生效 §8.2）；包内可见供单测构造 */
  record JevChannel(ExamFaqModelConfigEntity cfg, BigDecimal theta1, BigDecimal theta2,
      int concurrency, int readTimeoutSeconds) {
  }

  /**
   * 关键词剪枝开关（v1.3 附一，§13 步骤 11）：submit 时从 knowledge-fix-jev 配置行读取固化，
   * 改配置对下一批次生效。两个开关独立（KP-19）：stage1 宽剪仅 JEV 通道、stage2 剪枝两通道共享。
   * OFF = 双关 = 既有路径（回退即原路径，KP-12）。
   */
  record PruneConfig(boolean stage1, boolean stage2) {

    static final PruneConfig OFF = new PruneConfig(false, false);
  }

  /** 剪枝归因（KP-22）：随调用审计落库，供 A4 错 L2 归因回查——缺此无法区分"选错"与"被剪掉" */
  record PruneAudit(int candidates, boolean weakFallback, boolean reservedOld) {
  }

  /**
   * Jev 行级上下文（processBatch 内创建，随行传递）。ch 为 null = 通道关闭（§3 通道开关：
   * 配置行缺失/status≠enabled/api_key 未回填），全行走 LLM 全流程。
   * llmFallbackRows：兜底行去重集（跨轮重试的行只计一次，口径=行数而非尝试次数）。
   */
  private record JevRowCtx(JevChannel ch, Semaphore slots, AtomicBoolean openPermanent,
      AtomicBoolean openRound, AtomicInteger consecutiveFailures, AtomicInteger jevClassified,
      AtomicInteger llmFallback, DoubleAdder confSum,
      java.util.Set<Long> llmFallbackRows) {
  }

  /** Jev 单行判定结果：RESOLVED=已出结论 / FALLBACK_LLM=降级 LLM 全流程 / SKIP_ROW=422 记失败跳过（§5） */
  private enum JevOutcome { RESOLVED, FALLBACK_LLM, SKIP_ROW }

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
  /** Jev 决策客户端（独立非 OpenAI 协议，§8.1） */
  private final JevDecisionClient jevClient;
  private final ObjectMapper objectMapper;
  private final ThreadPoolTaskExecutor knowledgeFixExecutor;
  private final ThreadPoolTaskExecutor llmExecutor;
  private final int llmConcurrency;
  private final long retryBackoffBaseMillis;
  private final ExamKnowledgeFixLlmLogMapper llmLogMapper;

  public KnowledgeFixClassifyService(ExamKnowledgeFixBatchMapper batchMapper,
      ExamDetailKnowledgeFixMapper fixMapper, ExamExamDetailFixMapper queryMapper,
      ExamFaqModelConfigMapper modelConfigMapper, FaqLlmClient llmClient,
      JevDecisionClient jevClient, ObjectMapper objectMapper,
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
    this.jevClient = jevClient;
    this.objectMapper = objectMapper;
    this.knowledgeFixExecutor = knowledgeFixExecutor;
    this.llmExecutor = llmExecutor;
    this.llmLogMapper = llmLogMapper;
    this.llmConcurrency = llmConcurrency;
    this.retryBackoffBaseMillis = retryBackoffBaseMillis;
  }

  /** 提交分类批次（秒回 batchNo；装配修正清单 + 异步编排）。全量口径：所有 pending 行入批。 */
  public FixBatchVO submit(Long tenantId, Long userId) {
    return submit(tenantId, userId, null, null);
  }

  /**
   * 提交分类批次（支持范围，方案 §13 步骤 8 小批次实测用）：courseId 限定课程、limit 限制行数；
   * 两者均 null = 全量（既有口径不变）。暂停续跑须携带相同参数，否则剩余范围外行也会入批。
   */
  public FixBatchVO submit(Long tenantId, Long userId, Integer courseId, Integer limit) {
    if (limit != null && (limit < 1 || limit > 1000)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 须在 1~1000 之间");
    }
    if (courseId != null && courseId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "courseId 不合法");
    }
    Long running = batchMapper.selectCount(new LambdaQueryWrapper<ExamKnowledgeFixBatchEntity>()
        .in(ExamKnowledgeFixBatchEntity::getStatus,
            ExamKnowledgeFixBatchEntity.STATUS_PENDING, ExamKnowledgeFixBatchEntity.STATUS_RUNNING,
            ExamKnowledgeFixBatchEntity.STATUS_PAUSING));
    if (running != null && running > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "已有进行中的分类批次");
    }
    ExamFaqModelConfigEntity cfgFlash = requireConfig(MAIN_MODEL_CODE);
    ExamFaqModelConfigEntity cfgPro = requireConfig(PRO_MODEL_CODE);
    requireApiKey(cfgFlash);
    requireApiKey(cfgPro);
    // Jev 通道开关（§3/§5 首行）：配置行缺失/status≠enabled/api_key 未回填 → null（0 次 Jev 安静全走 LLM）；
    // 启用但 θ₂ < 0.3 属硬底线违规（§7.1）→ 配置校验拒绝
    JevChannel jev = resolveJevChannel();
    // 关键词剪枝开关（v1.3 附一）：读同一配置行但不看 status——通道关闭时剪枝对 LLM 路径独立生效（KP-11）
    PruneConfig prune = resolvePruneConfig();

    int populated = queryMapper.populateFixList();
    List<PendingFixRow> pending = scopePending(fixMapper.selectPendingWithContent(), courseId, limit);
    if (pending.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT,
          courseId != null ? "所选课程无待分类的修正行（可能已全部处理）" : "无待分类的修正行（可能已全部处理）");
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
    final Integer fCourseId = courseId;
    final Integer fLimit = limit;
    knowledgeFixExecutor.submit(() -> processBatch(batch.getId(), batchNo, tenantId, userId,
        cfgFlash, cfgPro, jev, prune, fCourseId, fLimit));
    log.info("知识修正批次已提交：batchNo={}, total={}, 新装入={}, scope={}",
        batchNo, pending.size(), populated,
        (courseId != null ? "courseId=" + courseId : "全课程") + (limit != null ? " limit=" + limit : ""));
    return FixBatchVO.from(batch);
  }

  /** 范围过滤（§13 步骤 8 小批次）：courseId 精确过滤 + limit 截断；两者均 null 时原样返回 */
  private static List<PendingFixRow> scopePending(List<PendingFixRow> all, Integer courseId,
      Integer limit) {
    List<PendingFixRow> scoped = all;
    if (courseId != null) {
      scoped = scoped.stream()
          .filter(r -> Objects.equals(r.getCourseId(), courseId)).toList();
    }
    if (limit != null && scoped.size() > limit) {
      scoped = scoped.subList(0, limit);
    }
    return scoped;
  }

  /**
   * 重跑通道（FIX-3/DEF-6）：把上一批 flag 行复位为 pending，使「提交分类批次」可再次完整执行。
   * 已写回（applied）与待写回（apply）行不动；有进行中的批次时拒绝。
   */
  public int resetClassify() {
    Long running = batchMapper.selectCount(new LambdaQueryWrapper<ExamKnowledgeFixBatchEntity>()
        .in(ExamKnowledgeFixBatchEntity::getStatus,
            ExamKnowledgeFixBatchEntity.STATUS_PENDING, ExamKnowledgeFixBatchEntity.STATUS_RUNNING,
            ExamKnowledgeFixBatchEntity.STATUS_PAUSING));
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
    // 双判加固（§10）：内存标志（本进程编排线程读）+ 批次表状态（进程重启后仍生效，
    // 派发边界每行一次主键查）。pausing 为过渡态：在飞行行未收尾，reset/重复 submit 均被拒。
    pauseRequests.add(batch.getBatchNo());
    batch.setStatus(ExamKnowledgeFixBatchEntity.STATUS_PAUSING);
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    log.info("知识修正批次暂停请求已登记：batchNo={}, done={}/{}", batch.getBatchNo(),
        batch.getDone(), batch.getTotal());
    return batch.getBatchNo();
  }

  /** 进行中的批次（pending/running/pausing，取最新一条）；无则返回 null */
  private ExamKnowledgeFixBatchEntity currentInFlight() {
    List<ExamKnowledgeFixBatchEntity> list = batchMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeFixBatchEntity>()
            .in(ExamKnowledgeFixBatchEntity::getStatus,
                ExamKnowledgeFixBatchEntity.STATUS_PENDING,
                ExamKnowledgeFixBatchEntity.STATUS_RUNNING,
                ExamKnowledgeFixBatchEntity.STATUS_PAUSING)
            .orderByDesc(ExamKnowledgeFixBatchEntity::getId)
            .last("LIMIT 1"));
    return list == null || list.isEmpty() ? null : list.get(0);
  }

  /** 编排线程（兼容入口，Jev 通道关闭 + 剪枝关闭）：两阶段分类 + 三采样自一致 + 旗舰升级 + 自动补跑 */
  public void processBatch(Long batchId, Long batchNo, Long tenantId, Long userId,
      ExamFaqModelConfigEntity cfgFlash, ExamFaqModelConfigEntity cfgPro) {
    processBatch(batchId, batchNo, tenantId, userId, cfgFlash, cfgPro, null, PruneConfig.OFF, null, null);
  }

  /** 编排线程（兼容入口）：Jev 分层快速通道 + LLM 全流程兜底，全量口径，剪枝开关关闭 */
  public void processBatch(Long batchId, Long batchNo, Long tenantId, Long userId,
      ExamFaqModelConfigEntity cfgFlash, ExamFaqModelConfigEntity cfgPro, JevChannel jev) {
    processBatch(batchId, batchNo, tenantId, userId, cfgFlash, cfgPro, jev, PruneConfig.OFF, null, null);
  }

  /** 编排线程（兼容入口）：Jev 分层快速通道 + LLM 全流程兜底 + 范围小批次，剪枝开关关闭 */
  public void processBatch(Long batchId, Long batchNo, Long tenantId, Long userId,
      ExamFaqModelConfigEntity cfgFlash, ExamFaqModelConfigEntity cfgPro, JevChannel jev,
      Integer scopeCourseId, Integer scopeLimit) {
    processBatch(batchId, batchNo, tenantId, userId, cfgFlash, cfgPro, jev, PruneConfig.OFF,
        scopeCourseId, scopeLimit);
  }

  /** 编排线程：Jev 分层快速通道 + LLM 全流程兜底（方案 §3）+ 三采样自一致 + 旗舰升级 + 自动补跑；
   *  scopeCourseId/scopeLimit 非空时只处理范围内行（§13 步骤 8 小批次），范围外行保持 pending；
   *  prune 非空时按开关执行关键词剪枝（v1.3 附一：stage1 仅 JEV 通道、stage2 两通道共享） */
  public void processBatch(Long batchId, Long batchNo, Long tenantId, Long userId,
      ExamFaqModelConfigEntity cfgFlash, ExamFaqModelConfigEntity cfgPro, JevChannel jev,
      PruneConfig prune, Integer scopeCourseId, Integer scopeLimit) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      ExamKnowledgeFixBatchEntity batch = batchMapper.selectById(batchId);
      if (batch == null) {
        log.error("知识修正批次不存在：batchId={}", batchId);
        return;
      }
      // §10：pausing 批次不得被启动段覆盖回 running（暂停双判的 DB 侧权威性）
      if (!ExamKnowledgeFixBatchEntity.STATUS_PAUSING.equals(batch.getStatus())) {
        batch.setStatus(ExamKnowledgeFixBatchEntity.STATUS_RUNNING);
      }
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
      // 关键词剪枝索引（v1.3 附一 KP-08）：批次级构建一次、构建后只读，Worker 并发读安全。
      // 任一开关关闭时 prune 标志使 classifyRow 不走剪枝分支，索引构建成本仍是一次性的（可忽略）。
      KnowledgeFixKeywordPruner pruner = new KnowledgeFixKeywordPruner(courseL2, courseL3ByParent);
      PruneConfig effPrune = prune == null ? PruneConfig.OFF : prune;

      Semaphore slots = new Semaphore(Math.max(1, llmConcurrency));
      // Jev 通道运行时状态（§3/§5）：独立并发信号量 + 双级熔断 + 行级统计。
      // 禁止复用 requireConfig 的抛错语义——通道关闭由 submit 解析为 null，批次照常完成。
      Semaphore jevSlots = jev == null ? null : new Semaphore(Math.max(1, jev.concurrency()));
      AtomicBoolean jevOpenPermanent = new AtomicBoolean(false);
      AtomicBoolean jevOpenRound = new AtomicBoolean(false);
      AtomicInteger jevConsecutiveFailures = new AtomicInteger(0);
      AtomicInteger jevClassifiedCnt = new AtomicInteger(0);
      AtomicInteger llmFallbackCnt = new AtomicInteger(0);
      DoubleAdder jevConfSum = new DoubleAdder();
      java.util.Set<Long> llmFallbackRows = java.util.concurrent.ConcurrentHashMap.newKeySet();
      JevRowCtx jevCtx = new JevRowCtx(jev, jevSlots, jevOpenPermanent, jevOpenRound,
          jevConsecutiveFailures, jevClassifiedCnt, llmFallbackCnt, jevConfSum, llmFallbackRows);
      AtomicInteger consecutiveFailures = new AtomicInteger(0);
      AtomicInteger guardCnt = new AtomicInteger(0);
      int roundsUsed = 0;
      AtomicInteger classified = new AtomicInteger(0);
      AtomicInteger unanimous = new AtomicInteger(0);
      AtomicInteger l2Direct = new AtomicInteger(0);
      int totalDispatched = 0;
      boolean batchFailed = false;
      // 范围批次（§13 步骤 8）：批次行集在首轮冻结——limit 圈定的行固定不变（重试仍在批内），
      // 范围外行永不入批、也不会被未分类收尾扫到
      java.util.Set<Long> scopeFrozenRowIds =
          scopeCourseId != null || scopeLimit != null ? new java.util.HashSet<>() : null;
      while (roundsUsed < MAX_DISPATCH_ROUNDS) {
        List<PendingFixRow> pending = fixMapper.selectPendingWithContent();
        if (scopeCourseId != null) {
          pending = pending.stream()
              .filter(r -> Objects.equals(r.getCourseId(), scopeCourseId)).toList();
        }
        if (scopeFrozenRowIds != null) {
          if (scopeFrozenRowIds.isEmpty()) {
            // 首个非空轮：冻结本批行集（limit 截断在此一次性生效）
            if (scopeLimit != null && pending.size() > scopeLimit) {
              pending = pending.subList(0, scopeLimit);
            }
            pending.forEach(r -> scopeFrozenRowIds.add(r.getRowId()));
          } else {
            pending = pending.stream()
                .filter(r -> scopeFrozenRowIds.contains(r.getRowId())).toList();
          }
        }
        if (pending.isEmpty()) {
          break;
        }
        if (roundsUsed > 0) {
          Thread.sleep(Math.max(0, retryBackoffBaseMillis) * roundsUsed);
        }
        roundsUsed++;
        consecutiveFailures.set(0);
        // Jev 熔断按轮复位（401 的 jevOpenPermanent 不复位：配置错误本批次内不会自愈，§5）
        jevOpenRound.set(false);
        jevConsecutiveFailures.set(0);
        // FIX-1/DEF-2：熔断只终止"本轮继续派发"，不放弃整批——本轮结束后进入下一轮补跑
        boolean breakerTrippedThisRound = false;
        // FIX-10/v1.7：人工暂停——只停"继续派发"，在飞行已提交的行走完
        boolean pauseTripped = false;
        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (PendingFixRow row : pending) {
          // 暂停双判（§10 加固）：内存标志 + 批次表状态（每行一次主键查，开销可忽略）
          if (pauseRequests.contains(batchNo) || isPausing(batchId)) {
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
                    nodeById, consecutiveFailures, guardCnt, classified, unanimous, l2Direct,
                    jevCtx, pruner, effPrune);
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
      // 收尾未分类行（FIX-1/DEF-3）：写 basis=unclassified 与"补跑后仍失败"分列，且不计入 done；
      // 有范围时只收尾本批冻结行集内的 pending 行，范围外行保持 pending 供后续批次
      List<PendingFixRow> remain = fixMapper.selectPendingWithContent();
      if (scopeCourseId != null) {
        remain = remain.stream()
            .filter(r -> Objects.equals(r.getCourseId(), scopeCourseId)).toList();
      }
      if (scopeFrozenRowIds != null) {
        remain = remain.stream()
            .filter(r -> scopeFrozenRowIds.contains(r.getRowId())).toList();
      }
      int unclassified = remain.size();
      for (PendingFixRow row : remain) {
        fixMapper.updateClassifyResult(row.getRowId(), row.getOldKnowledgeId(), BigDecimal.ZERO,
            "unclassified", "批次中断未分类", ExamDetailKnowledgeFixEntity.STATUS_FLAG, batchNo);
      }
      if (unclassified > 0) {
        batchMapper.incrementUnclassified(batchNo, unclassified);
      }
      // 收尾（FIX-1.4 质量门禁，§6 调整）：仅未分类占比 >5% 卡 failed；
      // 自一致率改信息项（仅 LLM 兜底行有自一致概念，样本少无统计意义，不再阻断批次）
      ExamKnowledgeFixBatchEntity finalState = batchMapper.selectById(batchId);
      if (finalState != null
          && !ExamKnowledgeFixBatchEntity.STATUS_FAILED.equals(finalState.getStatus())) {
        int total = finalState.getTotal() == null ? 0 : finalState.getTotal();
        // 未分类 = 收尾时仍未获得结论的行数（failed 是"尝试失败次数"，可能因重试大于行数，不能相减）
        int unclassifiedCount = unclassified;
        // 自一致率只衡量 LLM 三采样行（Jev 行无此概念）；仅统计展示，不卡门禁（§6）
        BigDecimal rate = classified.get() > 0
            ? BigDecimal.valueOf(unanimous.get()).divide(BigDecimal.valueOf(classified.get()),
                4, RoundingMode.HALF_UP)
            : null;
        finalState.setSelfConsistencyRate(rate);
        // Jev/LLM 构成统计（§8.5）：jev_classified + llm_fallback ≈ 已获结论行数
        finalState.setJevClassified(jevClassifiedCnt.get());
        finalState.setLlmFallback(llmFallbackCnt.get());
        finalState.setJevAvgConfidence(jevClassifiedCnt.get() > 0
            ? BigDecimal.valueOf(jevConfSum.doubleValue())
                .divide(BigDecimal.valueOf(jevClassifiedCnt.get()), 4, RoundingMode.HALF_UP)
            : null);
        finalState.setUpdatedAt(LocalDateTime.now());
        boolean unclassifiedAboveGate =
            total > 0 && BigDecimal.valueOf(unclassifiedCount)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .compareTo(UNCLASSIFIED_RATIO_GATE) > 0;
        if (batchFailed || unclassifiedAboveGate) {
          finalState.setStatus(ExamKnowledgeFixBatchEntity.STATUS_FAILED);
          finalState.setFailReason(buildGateReason(batchFailed, unclassifiedCount, total));
        } else {
          finalState.setStatus(ExamKnowledgeFixBatchEntity.STATUS_DONE);
        }
        batchMapper.updateById(finalState);
      }
      log.info("知识修正批次结束：batchNo={}, rounds={}, dispatched={}, classified={}, "
              + "unanimous={}, l2Direct={}, guard={}, unclassified={}, jevClassified={}, "
              + "llmFallback={}, status={}",
          batchNo, roundsUsed, totalDispatched, classified.get(), unanimous.get(), l2Direct.get(),
          guardCnt.get(), unclassified, jevClassifiedCnt.get(), llmFallbackCnt.get(),
          finalState == null ? "?" : finalState.getStatus());
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

  /** 单行分类：Jev 分层快速通道（§3）→ LLM 全流程兜底（两阶段三采样自一致 + 旗舰升级 + 卫兵，KF-09~15）；
   *  v1.3 剪枝：stage1 宽剪仅作用于本方法内 JEV 通道的 stage1 criteria（KP-18），
   *  stage2 剪枝作用于 JEV stage2 与 LLM stage2/escalate（两通道同口径，KP-10） */
  private void classifyRow(PendingFixRow row, Long batchNo, Long tenantId,
      ExamFaqModelConfigEntity cfgFlash, ExamFaqModelConfigEntity cfgPro,
      Map<Integer, List<KnowledgeNodeRow>> courseL2,
      Map<Integer, Map<Integer, List<KnowledgeNodeRow>>> courseL3ByParent,
      Map<Integer, KnowledgeNodeRow> nodeById, AtomicInteger consecutiveFailures,
      AtomicInteger guardCnt, AtomicInteger classified, AtomicInteger unanimous,
      AtomicInteger l2Direct, JevRowCtx jevCtx, KnowledgeFixKeywordPruner pruner, PruneConfig prune)
      throws InterruptedException {
    TenantContextHolder.setTenantId(tenantId);
    try {
      // ── Jev 分层快速通道（§3）：通道开着且未熔断才走；需要 LLM 的情况全部降级兜底 ──
      boolean jevAttempted = false;
      if (jevCtx.ch() != null && !jevCtx.openPermanent().get() && !jevCtx.openRound().get()) {
        jevAttempted = true;
        JevOutcome outcome = tryClassifyViaJev(row, batchNo, tenantId, jevCtx, courseL2,
            courseL3ByParent, nodeById, pruner, prune);
        if (outcome == JevOutcome.RESOLVED || outcome == JevOutcome.SKIP_ROW) {
          return; // RESOLVED=已出结论；SKIP_ROW=422 记失败跳过，行保持 pending（§5）
        }
        // FALLBACK_LLM → 落入下方 LLM 全流程（兜底计数已在 tryClassifyViaJev 内按行去重记过）
      }
      if (!jevAttempted) {
        // 通道关闭或熔断跳过：本行直接走 LLM，计入兜底（按行去重）
        countLlmFallback(jevCtx, row.getRowId());
      }
      Integer courseId = row.getCourseId();
      List<KnowledgeNodeRow> l2List = courseL2.getOrDefault(courseId, List.of());
      Map<Integer, List<KnowledgeNodeRow>> l3Map = courseL3ByParent.getOrDefault(courseId, Map.of());
      String questionText = buildQuestionText(row.getContent(), row.getAnswer(), row.getResolve());
      KnowledgeNodeRow oldNode = nodeById.get(row.getOldKnowledgeId());

      // 阶段 1：L2 粗分（1 次）——LLM 全流程 stage1 恒全量候选，不做剪枝（附一 KP-18：
      // stage1 召回仅 ~77%，强加给安全网等于砍其正确率天花板；宽剪只属于 JEV 通道）
      String s1 = callLlm(cfgFlash, tenantId, batchNo, row.getDetailId(), "stage1",
          buildStage1Prompt(questionText, l2List), 1024, null).content();
      int l2Id = extractIntField(s1, "l2_id");
      boolean l2Valid = l2List.stream().anyMatch(n -> n.getId() != null && n.getId() == l2Id);
      if (!l2Valid) {
        throw new GuardRejectException("L2 越界: " + l2Id);
      }
      List<KnowledgeNodeRow> l3List = l3Map.getOrDefault(l2Id, List.of());
      if (l3List.isEmpty()) {
        // 业务裁定"题可挂 L2"：模型选中的章节下无存活 L3 → 直接以该 L2 收尾，
        // 跳过阶段 2（每行省 3~4 次调用）；固定置信 0.5 + basis=l2-direct；
        // 不进自一致率分子分母（无三采样概念，避免稀释门禁口径）
        boolean sameNode = Objects.equals(l2Id, row.getOldKnowledgeId());
        String outcome = sameNode ? ExamDetailKnowledgeFixEntity.STATUS_KEEP
            : ExamDetailKnowledgeFixEntity.STATUS_APPLY;
        fixMapper.updateClassifyResult(row.getRowId(), l2Id, L2_DIRECT_CONFIDENCE, "l2-direct",
            "所选章节下无鉴定点，按业务规则直接挂章节", outcome, batchNo);
        batchMapper.incrementClassified(batchNo, outcome);
        l2Direct.incrementAndGet();
        consecutiveFailures.set(0);
        return;
      }
      // v1.3 stage2 剪枝（两通道同口径，KP-10）：兄弟集收窄 top-8，弱信号/小集合回退全量；
      // 审计记录剪枝后候选数与回退/保位标志（KP-22）
      List<KnowledgeNodeRow> l3Candidates = l3List;
      PruneAudit stage2Audit = null;
      if (prune.stage2()) {
        KnowledgeFixKeywordPruner.PruneResult stage2Prune =
            pruner.pruneSiblings(courseId, l3List, row.getOldKnowledgeId(), questionText);
        l3Candidates = stage2Prune.candidates();
        stage2Audit = new PruneAudit(l3Candidates.size(), stage2Prune.weakFallback(),
            stage2Prune.reservedOld());
      }

      // 阶段 2：三采样自一致
      // FIX-11/v1.7：候选顺序每行随机一次（种子=detailId），3 次采样共用同一顺序——
      // 第 2、3 次采样 prompt 与第 1 次逐字节一致，可整段命中前缀缓存（同课程行亦共享候选清单前缀）。
      // 代价：行内位置偏差检测退化为行间（全批顺序仍随机）；位置偏差兜底 = 确定性卫兵。
      // v1.3：随机化作用于剪枝后的候选集（l3Candidates）。
      List<KnowledgeNodeRow> ordered = new ArrayList<>(l3Candidates);
      Collections.shuffle(ordered, new java.util.Random(row.getDetailId()));
      List<Integer> nodeVotes = new ArrayList<>();
      List<Double> confVotes = new ArrayList<>();
      List<String> reasons = new ArrayList<>();
      for (int i = 0; i < SAMPLES; i++) {
        String s2 = callLlm(cfgFlash, tenantId, batchNo, row.getDetailId(), "stage2",
            buildStage2Prompt(questionText, ordered, oldNode), 1024, stage2Audit).content();
        JsonNode json = parseObject(s2);
        int nodeId = json.path("node_id").asInt(-1);
        double conf = json.path("confidence").asDouble(0);
        String reason = json.path("reason").asText("");
        KnowledgeNodeRow picked = nodeById.get(nodeId);
        if (picked == null || !Objects.equals(picked.getCourseId(), courseId)) {
          throw new GuardRejectException("node_id 越界: " + nodeId);
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
            buildStage2Prompt(questionText, ordered, oldNode), 2048, stage2Audit).content();
        JsonNode json = parseObject(s3);
        finalNodeId = json.path("node_id").asInt(-1);
        finalConf = json.path("confidence").asDouble(0);
        reason = "旗舰复判:" + json.path("reason").asText("");
        basis = "llm-pro";
        if (nodeById.get(finalNodeId) == null) {
          throw new GuardRejectException("旗舰复判 node_id 越界");
        }
      }

      KnowledgeNodeRow chosen = nodeById.get(finalNodeId);
      // keep：模型选中当前节点（防过度修正）
      if (Objects.equals(finalNodeId, row.getOldKnowledgeId())) {
        fixMapper.updateClassifyResult(row.getRowId(), row.getOldKnowledgeId(),
            BigDecimal.valueOf(finalConf).setScale(3, RoundingMode.HALF_UP), basis, reason,
            ExamDetailKnowledgeFixEntity.STATUS_KEEP, batchNo);
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
            "卫兵拦截:" + reason, ExamDetailKnowledgeFixEntity.STATUS_FLAG, batchNo);
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
          BigDecimal.valueOf(finalConf).setScale(3, RoundingMode.HALF_UP), basis, reason, outcome, batchNo);
      batchMapper.incrementClassified(batchNo, outcome);
      consecutiveFailures.set(0);
    } catch (GuardRejectException e) {
      // 守卫拒绝是确定性业务判定（重试同样被拒），不计失败重试与熔断计数；行保持 pending 供下轮重试
      log.warn("知识修正分类被守卫拒绝：detailId={}, err={}", row.getDetailId(), e.getMessage());
    } catch (Exception e) {
      log.warn("知识修正分类失败：detailId={}, err={}", row.getDetailId(), e.getMessage());
      batchMapper.incrementFailed(batchNo);
      consecutiveFailures.incrementAndGet();
    } finally {
      TenantContextHolder.clear();
    }
  }

  /**
   * Jev 两阶段概率判定（方案 §3/§4）：阶段1 L2 粗分 → 阶段2 L3 细分；双门 conf₁≥θ₁ 且 conf₂≥θ₂ → apply
   * （basis=jev，无理由文本）；选中当前节点 → keep（阈值不拦截）；零重合卫兵 → flag（优先于置信分层）；
   * conf₁&lt;θ₁ / conf₂&lt;θ₂ / choice 越界 / criteria&lt;2 → 降级 LLM 全流程。审计见 {@link #logJevCall}。
   */
  private JevOutcome tryClassifyViaJev(PendingFixRow row, Long batchNo, Long tenantId,
      JevRowCtx ctx, Map<Integer, List<KnowledgeNodeRow>> courseL2,
      Map<Integer, Map<Integer, List<KnowledgeNodeRow>>> courseL3ByParent,
      Map<Integer, KnowledgeNodeRow> nodeById, KnowledgeFixKeywordPruner pruner, PruneConfig prune)
      throws InterruptedException {
    Integer courseId = row.getCourseId();
    List<KnowledgeNodeRow> l2List = courseL2.getOrDefault(courseId, List.of());
    String questionText = buildQuestionText(row.getContent(), row.getAnswer(), row.getResolve());

    // v1.3 stage1 宽剪（附一硬约束：仅 JEV 通道；KP-18~21）：L2 候选按 L3 证据聚合收窄 top-5，
    // 老 L2 强制保位（keep 语义不被剪枝消灭）。剪后校验 MIN/MAX——宽剪使超大课程（L2>255）
    // 也满足 Jev criteria 上限，这正是本开关存在的目的之一；开关关闭时保持既有全量校验。
    List<KnowledgeNodeRow> l2Candidates = l2List;
    PruneAudit stage1Audit = null;
    if (prune.stage1()) {
      KnowledgeFixKeywordPruner.PruneResult p1 =
          pruner.pruneL2Shortlist(courseId, l2List, row.getOldKnowledgeId(), questionText);
      l2Candidates = p1.candidates();
      stage1Audit = new PruneAudit(p1.candidates().size(), p1.weakFallback(), p1.reservedOld());
    }
    // 前置拦截（§5）：criteria <2（实测 min_length=2）或 >255（实测上限）均必 422，不发请求直接降级
    if (l2Candidates.size() < MIN_JEV_CRITERIA || l2Candidates.size() > MAX_JEV_CRITERIA) {
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    }
    Map<String, String> l2Criteria = new LinkedHashMap<>();
    for (KnowledgeNodeRow n : l2Candidates) {
      l2Criteria.put(String.valueOf(n.getId()), n.getTitle());
    }
    JevDecisionResult r1;
    try {
      ctx.slots().acquire();
      try {
        r1 = jevClient.decide(ctx.ch().cfg().getBaseUrl(), ctx.ch().cfg().getApiKey(),
            ctx.ch().cfg().getModelId(), questionText, "l2", "该试题真正归属的 L2 章节",
            l2Criteria, ctx.ch().readTimeoutSeconds());
      } finally {
        ctx.slots().release();
      }
    } catch (JevUnauthorizedException e) {
      // 401=配置错误：立即熔断整批转 LLM 并 warn 告警（密钥错不会因重试变好，§5）
      ctx.openPermanent().set(true);
      log.warn("[Jev] 401 鉴权失败，整批熔断转 LLM：batchNo={}, err={}", batchNo, e.getMessage());
      logJevCall(tenantId, batchNo, row.getDetailId(), "jev-stage1", ctx.ch(), null, questionText, e, stage1Audit);
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    } catch (JevValidationException e) {
      // 422 细分（v1.4，2026-10-04 生产实测）：token_budget_exceeded = 候选超展开预算
      // （expanded_input_tokens ≤ 32,768，53 个 L2 全量展开 33,480 即超）——该行 LLM 可正常分类
      // → 降级 LLM 全流程；其余 422 = 请求构造 bug，重试无用 → SKIP_ROW（§5）
      if (e.getMessage() != null && e.getMessage().contains(JEV_BUDGET_EXCEEDED_MARKER)) {
        log.warn("[Jev] 422 候选超展开预算，行降级 LLM：batchNo={}, detailId={}, err={}",
            batchNo, row.getDetailId(), e.getMessage());
        logJevCall(tenantId, batchNo, row.getDetailId(), "jev-stage1", ctx.ch(), null, questionText, e, stage1Audit);
        countLlmFallback(ctx, row.getRowId());
        return JevOutcome.FALLBACK_LLM;
      }
      batchMapper.incrementFailed(batchNo);
      if (ctx.consecutiveFailures().incrementAndGet() >= MAX_CONSECUTIVE_FAILURES) {
        ctx.openRound().set(true);
      }
      log.warn("[Jev] 422 请求构造失败，行记失败跳过：batchNo={}, detailId={}, err={}",
          batchNo, row.getDetailId(), e.getMessage());
      logJevCall(tenantId, batchNo, row.getDetailId(), "jev-stage1", ctx.ch(), null, questionText, e, stage1Audit);
      return JevOutcome.SKIP_ROW;
    } catch (JevDecisionException e) {
      // 超时/5xx/解析失败：该行降级 LLM 全流程；连续失败计入熔断（§5）
      if (ctx.consecutiveFailures().incrementAndGet() >= MAX_CONSECUTIVE_FAILURES) {
        ctx.openRound().set(true);
      }
      log.warn("[Jev] 调用失败，行降级 LLM：batchNo={}, detailId={}, err={}",
          batchNo, row.getDetailId(), e.getMessage());
      logJevCall(tenantId, batchNo, row.getDetailId(), "jev-stage1", ctx.ch(), null, questionText, e, stage1Audit);
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    }
    ctx.consecutiveFailures().set(0);
    logJevCall(tenantId, batchNo, row.getDetailId(), "jev-stage1", ctx.ch(), r1, questionText, null, stage1Audit);

    int l2Id;
    try {
      l2Id = Integer.parseInt(r1.choice());
    } catch (NumberFormatException e) {
      // choice 不在 criteria 语义内 → 卫兵拒绝（不计连续失败），行降级 LLM（§5）
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    }
    // 越界校验对齐实际下发的候选集（宽剪后为短名单，KP-18）
    boolean l2Valid = l2Candidates.stream().anyMatch(n -> n.getId() != null && n.getId() == l2Id);
    if (!l2Valid) {
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    }
    if (BigDecimal.valueOf(r1.confidence()).compareTo(ctx.ch().theta1()) < 0) {
      // conf₁ < θ₁：L2 都拿不准，阶段 2 无意义（§3 双门前置）
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    }
    List<KnowledgeNodeRow> l3List = courseL3ByParent.getOrDefault(courseId, Map.of())
        .getOrDefault(l2Id, List.of());
    if (l3List.isEmpty()) {
      // 所选 L2 下无存活 L3 → jev-l2-direct 直收尾（沿用 l2-direct 业务裁定，basis=jev-l2-direct）
      boolean sameNode = Objects.equals(l2Id, row.getOldKnowledgeId());
      String outcome = sameNode ? ExamDetailKnowledgeFixEntity.STATUS_KEEP
          : ExamDetailKnowledgeFixEntity.STATUS_APPLY;
      fixMapper.updateClassifyResult(row.getRowId(), l2Id, L2_DIRECT_CONFIDENCE, "jev-l2-direct",
          "所选章节下无鉴定点，按业务规则直接挂章节", outcome, batchNo);
      batchMapper.incrementClassified(batchNo, outcome);
      ctx.jevClassified().incrementAndGet();
      ctx.confSum().add(L2_DIRECT_CONFIDENCE.doubleValue());
      return JevOutcome.RESOLVED;
    }
    // v1.3 stage2 剪枝（两通道同口径 KP-10；对 JEV 而言候选收窄直接抬高 maxP 下限，附四）
    List<KnowledgeNodeRow> l3Candidates = l3List;
    PruneAudit stage2Audit = null;
    if (prune.stage2()) {
      KnowledgeFixKeywordPruner.PruneResult p2 =
          pruner.pruneSiblings(courseId, l3List, row.getOldKnowledgeId(), questionText);
      l3Candidates = p2.candidates();
      stage2Audit = new PruneAudit(l3Candidates.size(), p2.weakFallback(), p2.reservedOld());
    }
    if (l3Candidates.size() < MIN_JEV_CRITERIA || l3Candidates.size() > MAX_JEV_CRITERIA) {
      // 单候选（<2）或超上限（>255）无法构成合法 choice，降级 LLM 全流程
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    }
    Map<String, String> l3Criteria = new LinkedHashMap<>();
    for (KnowledgeNodeRow n : l3Candidates) {
      l3Criteria.put(String.valueOf(n.getId()), n.getTitle());
    }
    JevDecisionResult r2;
    try {
      ctx.slots().acquire();
      try {
        r2 = jevClient.decide(ctx.ch().cfg().getBaseUrl(), ctx.ch().cfg().getApiKey(),
            ctx.ch().cfg().getModelId(), questionText, "l3", "该试题真正归属的鉴定点",
            l3Criteria, ctx.ch().readTimeoutSeconds());
      } finally {
        ctx.slots().release();
      }
    } catch (JevUnauthorizedException e) {
      ctx.openPermanent().set(true);
      log.warn("[Jev] 401 鉴权失败，整批熔断转 LLM：batchNo={}, err={}", batchNo, e.getMessage());
      logJevCall(tenantId, batchNo, row.getDetailId(), "jev-stage2", ctx.ch(), null, questionText, e, stage2Audit);
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    } catch (JevValidationException e) {
      // 422 细分同 stage1（v1.4）：预算超限 → 降级 LLM；构造 bug → SKIP_ROW
      if (e.getMessage() != null && e.getMessage().contains(JEV_BUDGET_EXCEEDED_MARKER)) {
        log.warn("[Jev] 422 候选超展开预算，行降级 LLM：batchNo={}, detailId={}, err={}",
            batchNo, row.getDetailId(), e.getMessage());
        logJevCall(tenantId, batchNo, row.getDetailId(), "jev-stage2", ctx.ch(), null, questionText, e, stage2Audit);
        countLlmFallback(ctx, row.getRowId());
        return JevOutcome.FALLBACK_LLM;
      }
      batchMapper.incrementFailed(batchNo);
      if (ctx.consecutiveFailures().incrementAndGet() >= MAX_CONSECUTIVE_FAILURES) {
        ctx.openRound().set(true);
      }
      log.warn("[Jev] 422 请求构造失败，行记失败跳过：batchNo={}, detailId={}, err={}",
          batchNo, row.getDetailId(), e.getMessage());
      logJevCall(tenantId, batchNo, row.getDetailId(), "jev-stage2", ctx.ch(), null, questionText, e, stage2Audit);
      return JevOutcome.SKIP_ROW;
    } catch (JevDecisionException e) {
      if (ctx.consecutiveFailures().incrementAndGet() >= MAX_CONSECUTIVE_FAILURES) {
        ctx.openRound().set(true);
      }
      log.warn("[Jev] 调用失败，行降级 LLM：batchNo={}, detailId={}, err={}",
          batchNo, row.getDetailId(), e.getMessage());
      logJevCall(tenantId, batchNo, row.getDetailId(), "jev-stage2", ctx.ch(), null, questionText, e, stage2Audit);
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    }
    ctx.consecutiveFailures().set(0);
    logJevCall(tenantId, batchNo, row.getDetailId(), "jev-stage2", ctx.ch(), r2, questionText, null, stage2Audit);

    int nodeId;
    try {
      nodeId = Integer.parseInt(r2.choice());
    } catch (NumberFormatException e) {
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    }
    KnowledgeNodeRow picked = nodeById.get(nodeId);
    if (picked == null || !Objects.equals(picked.getCourseId(), courseId)) {
      // 越界=卫兵拒绝语义（§5），降级 LLM，不计连续失败
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    }
    // keep 判定：模型选中当前节点（防过度修正，阈值不拦截 keep，§3）
    if (Objects.equals(nodeId, row.getOldKnowledgeId())) {
      fixMapper.updateClassifyResult(row.getRowId(), row.getOldKnowledgeId(),
          scale(r2.confidence()), "jev", "", ExamDetailKnowledgeFixEntity.STATUS_KEEP, batchNo);
      batchMapper.incrementClassified(batchNo, ExamDetailKnowledgeFixEntity.STATUS_KEEP);
      ctx.jevClassified().incrementAndGet();
      ctx.confSum().add(r2.confidence());
      return JevOutcome.RESOLVED;
    }
    // 确定性卫兵：零关键词重合且非素养节点 → flag（沿用，优先于置信分层）
    if (!KnowledgeNormUtil.hasBigramOverlap(picked.getTitle(), questionText)
        && !KnowledgeNormUtil.isQualityNode(picked.getTitle())) {
      fixMapper.updateClassifyResult(row.getRowId(), nodeId, scale(r2.confidence()), "jev",
          "卫兵拦截:概率判定零关键词重合", ExamDetailKnowledgeFixEntity.STATUS_FLAG, batchNo);
      batchMapper.incrementClassified(batchNo, ExamDetailKnowledgeFixEntity.STATUS_FLAG);
      ctx.jevClassified().incrementAndGet();
      ctx.confSum().add(r2.confidence());
      return JevOutcome.RESOLVED;
    }
    // 双门置信分层：conf₂ < θ₂ → 降级 LLM 全流程（产 reason）；双门全过 → apply（§3）
    if (BigDecimal.valueOf(r2.confidence()).compareTo(ctx.ch().theta2()) < 0) {
      countLlmFallback(ctx, row.getRowId());
      return JevOutcome.FALLBACK_LLM;
    }
    fixMapper.updateClassifyResult(row.getRowId(), nodeId, scale(r2.confidence()), "jev", "",
        ExamDetailKnowledgeFixEntity.STATUS_APPLY, batchNo);
    batchMapper.incrementClassified(batchNo, ExamDetailKnowledgeFixEntity.STATUS_APPLY);
    ctx.jevClassified().incrementAndGet();
    ctx.confSum().add(r2.confidence());
    return JevOutcome.RESOLVED;
  }

  /** 兜底行计数（§8.5）：按 rowId 去重——跨轮重试/多次降级的行只计一次（口径=行数） */
  private void countLlmFallback(JevRowCtx ctx, Long rowId) {
    if (ctx.llmFallbackRows().add(rowId)) {
      ctx.llmFallback().incrementAndGet();
    }
  }

  /** Jev 调用审计落库（§8.8）：stage/响应 model（记现有 model_id 列）/tokens/耗时/confidence_definition/top1/top2；失败调用也留痕 */
  private void logJevCall(Long tenantId, Long batchNo, Integer detailId, String stage,
      JevChannel ch, JevDecisionResult result, String state, Exception error, PruneAudit pruneAudit) {
    try {
      ExamKnowledgeFixLlmLogEntity logRow = new ExamKnowledgeFixLlmLogEntity();
      logRow.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
      logRow.setTenantId(tenantId);
      logRow.setBatchNo(batchNo);
      logRow.setDetailId(detailId);
      logRow.setStage(stage);
      // 响应 model 优先（TokenDance 双服务商路由漂移监控，§9 风险 3）
      logRow.setModelId(result != null && result.responseModel() != null
          && !result.responseModel().isBlank()
          ? result.responseModel() : ch.cfg().getModelId());
      logRow.setPromptDigest(state.length() > 200 ? state.substring(state.length() - 200) : state);
      long in = result == null ? 0 : result.inputTokens();
      long out = result == null ? 0 : result.outputTokens();
      logRow.setPromptTokens((int) in);
      logRow.setCompletionTokens((int) out);
      logRow.setTotalTokens((int) (in + out));
      logRow.setCachedTokens(0);
      logRow.setDurationMs(result == null ? 0 : result.durationMs());
      if (pruneAudit != null) {
        logRow.setPrunedCandidates(pruneAudit.candidates());
        logRow.setPruneWeakFallback(pruneAudit.weakFallback() ? 1 : 0);
        logRow.setPruneReservedOld(pruneAudit.reservedOld() ? 1 : 0);
      }
      if (result != null) {
        logRow.setConfidenceDefinition(result.confidenceDefinition());
        List<Double> tops = result.probabilities().values().stream()
            .sorted(Comparator.reverseOrder()).toList();
        logRow.setTop1Prob(tops.isEmpty() ? null
            : BigDecimal.valueOf(tops.get(0)).setScale(6, RoundingMode.HALF_UP));
        logRow.setTop2Prob(tops.size() > 1
            ? BigDecimal.valueOf(tops.get(1)).setScale(6, RoundingMode.HALF_UP) : null);
      } else if (error != null) {
        logRow.setConfidenceDefinition("error:" + error.getClass().getSimpleName());
      }
      logRow.setCreatedAt(LocalDateTime.now());
      logRow.setCreatedBy(0L);
      logRow.setUpdatedBy(0L);
      logRow.setDeleted(0);
      llmLogMapper.insert(logRow);
    } catch (Exception logEx) {
      log.warn("Jev 审计落库失败（不影响主流程）：{}", logEx.getMessage());
    }
  }

  private BigDecimal scale(double v) {
    return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP);
  }

  /** 暂停双判（§10 加固）：批次表状态检查（派发边界每行一次主键查，开销可忽略） */
  private boolean isPausing(Long batchId) {
    ExamKnowledgeFixBatchEntity b = batchMapper.selectById(batchId);
    return b != null && ExamKnowledgeFixBatchEntity.STATUS_PAUSING.equals(b.getStatus());
  }

  /**
   * 启动恢复（§10 加固）：进程重启后编排线程已消亡，遗留 pending/running/pausing 批次统一收尾为
   * paused，剩余行保持 pending 可再次提交续跑。租户上下文显式设置（启动线程无 SecurityContext）。
   */
  @EventListener(ApplicationReadyEvent.class)
  public void recoverAbandonedBatchesOnStartup() {
    TenantContextHolder.setTenantId(TenantContextHolder.DEFAULT_TENANT_ID);
    try {
      List<ExamKnowledgeFixBatchEntity> abandoned = batchMapper.selectList(
          new LambdaQueryWrapper<ExamKnowledgeFixBatchEntity>()
              .in(ExamKnowledgeFixBatchEntity::getStatus,
                  ExamKnowledgeFixBatchEntity.STATUS_PENDING,
                  ExamKnowledgeFixBatchEntity.STATUS_RUNNING,
                  ExamKnowledgeFixBatchEntity.STATUS_PAUSING));
      if (abandoned == null || abandoned.isEmpty()) {
        return;
      }
      for (ExamKnowledgeFixBatchEntity b : abandoned) {
        b.setStatus(ExamKnowledgeFixBatchEntity.STATUS_PAUSED);
        b.setFailReason("进程重启收尾：遗留批次置为 paused，剩余 pending 行可再次提交续跑");
        b.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(b);
      }
      log.warn("知识修正批次启动恢复：收尾遗留批次 {} 个为 paused", abandoned.size());
    } catch (Exception e) {
      log.error("知识修正批次启动恢复失败（不影响启动）：{}", e.getMessage(), e);
    } finally {
      TenantContextHolder.clear();
    }
  }

  /**
   * Jev 通道开关（§3/§5 首行）：配置行缺失或 status≠enabled 或 api_key 未回填 → null
   * （0 次 Jev，全部行走 LLM，安静降级不计失败）；θ₂ &lt; 0.3 属硬底线违规 → 配置校验拒绝（§7.1）。
   */
  private JevChannel resolveJevChannel() {
    List<ExamFaqModelConfigEntity> list = modelConfigMapper.selectList(
        new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
            .eq(ExamFaqModelConfigEntity::getModelCode, JEV_MODEL_CODE)
            .eq(ExamFaqModelConfigEntity::getStatus, "enabled"));
    if (list == null || list.isEmpty()) {
      log.info("[Jev] 通道未启用（无 enabled 的 {} 配置行），本批次全走 LLM", JEV_MODEL_CODE);
      return null;
    }
    ExamFaqModelConfigEntity cfg = list.get(0);
    if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()
        || "PENDING_USER_INPUT".equals(cfg.getApiKey())) {
      log.info("[Jev] 通道 api_key 未回填（{}），本批次全走 LLM", JEV_MODEL_CODE);
      return null;
    }
    BigDecimal theta1 = cfg.getJevTheta1() == null ? DEFAULT_JEV_THETA : cfg.getJevTheta1();
    BigDecimal theta2 = cfg.getJevTheta2() == null ? DEFAULT_JEV_THETA : cfg.getJevTheta2();
    if (theta2.compareTo(JEV_MIN_THETA2) < 0) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "Jev θ₂ 配置 " + theta2 + " 低于硬底线 0.3（§7.1），已拒绝提交");
    }
    int concurrency = cfg.getJevConcurrency() == null ? DEFAULT_JEV_CONCURRENCY
        : Math.max(1, cfg.getJevConcurrency());
    int readTimeout = cfg.getJevReadTimeoutSeconds() == null ? DEFAULT_JEV_READ_TIMEOUT_SECONDS
        : Math.max(5, cfg.getJevReadTimeoutSeconds());
    log.info("[Jev] 通道已启用：model={}, θ₁={}, θ₂={}, 并发={}, 读超时={}s",
        cfg.getModelId(), theta1, theta2, concurrency, readTimeout);
    return new JevChannel(cfg, theta1, theta2, concurrency, readTimeout);
  }

  /**
   * 关键词剪枝开关解析（v1.3 附一）：读 knowledge-fix-jev 配置行但不看 status——
   * 通道关闭（disabled）时剪枝对 LLM 路径独立生效（KP-11）；配置行缺失 → OFF（=既有路径）。
   * 批次启动时固化，改配置对下一批次生效。
   */
  private PruneConfig resolvePruneConfig() {
    List<ExamFaqModelConfigEntity> list = modelConfigMapper.selectList(
        new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
            .eq(ExamFaqModelConfigEntity::getModelCode, JEV_MODEL_CODE)
            .orderByDesc(ExamFaqModelConfigEntity::getId)
            .last("LIMIT 1"));
    if (list == null || list.isEmpty()) {
      return PruneConfig.OFF;
    }
    ExamFaqModelConfigEntity cfg = list.get(0);
    boolean stage1 = cfg.getJevPruneStage1() != null && cfg.getJevPruneStage1() == 1;
    boolean stage2 = cfg.getJevPruneStage2() != null && cfg.getJevPruneStage2() == 1;
    if (!stage1 && !stage2) {
      return PruneConfig.OFF;
    }
    log.info("[Prune] 剪枝开关：stage1={}, stage2={}（来自 {} 配置行，status={}，通道开关独立判定）",
        stage1, stage2, JEV_MODEL_CODE, cfg.getStatus());
    return new PruneConfig(stage1, stage2);
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
        + Math.max(0, total - done) + " 行保持待分类；续跑请带相同范围点「范围提交（小批次）」"
        + "（点全量提交将纳入所有待分类行）");
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    log.info("知识修正批次已暂停：batchNo={}, done={}/{}", batchNo, done, total);
  }

  /** 质量门禁说明（FIX-1.4，§6 调整）：自一致率已改信息项，仅未分类占比可卡批次 */
  private static String buildGateReason(boolean noDispatch, int unclassified, int total) {
    StringBuilder sb = new StringBuilder("未达质量门禁：");
    if (noDispatch) {
      sb.append("本轮一条都未能派发；");
    }
    if (total > 0 && unclassified > 0) {
      sb.append("未分类 ").append(unclassified).append("/").append(total)
          .append("（上限 5%）；");
    }
    sb.append("未分类行可经 reset 后重跑");
    return sb.toString();
  }

  /** LLM 调用 + 审计落库（LLM 规范 #10，KF-19）：usage/耗时/摘要写入审计表，落库失败不影响主流程 */
  private com.guoyu.platform.ai.dto.LlmChatCompletionResult callLlm(
      ExamFaqModelConfigEntity cfg, Long tenantId, Long batchNo, Integer detailId,
      String stage, String prompt, int maxTokens, PruneAudit pruneAudit) {
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
      if (pruneAudit != null) {
        logRow.setPrunedCandidates(pruneAudit.candidates());
        logRow.setPruneWeakFallback(pruneAudit.weakFallback() ? 1 : 0);
        logRow.setPruneReservedOld(pruneAudit.reservedOld() ? 1 : 0);
      }
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
