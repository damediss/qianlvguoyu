package com.guoyu.platform.exam.theorygen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import com.guoyu.platform.exam.theorygen.dto.TheoryQuestionItem;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintNodeEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenBatchEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenDraftEntity;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintNodeMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryGenBatchMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryGenDraftMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 理论生成批次优化单测（优化方案 v0.3 §7 OPT-01~07/11~16）。
 * 全部桩响应不依赖真实模型；并发用例用真实线程池 + 可控延迟。
 */
class TheoryGenBatchServiceOptTest {

  private static final Long TENANT = 1L;
  private static final Long USER = 9L;
  private static final Long BP_ID = 100L;
  private static final Long CFG_ID = 200L;
  private static final Long BATCH_ID = 300L;
  private static final String GLM_URL = "https://open.bigmodel.cn/api/paas/v4";

  private TheoryBlueprintMapper blueprintMapper;
  private TheoryBlueprintNodeMapper nodeMapper;
  private TheoryGenBatchMapper batchMapper;
  private TheoryGenDraftMapper draftMapper;
  private ExamFaqModelConfigMapper modelConfigMapper;
  private TheoryGenAiService aiService;
  private TheoryQuestionValidator validator;
  private TransactionTemplate transactionTemplate;
  private ThreadPoolTaskExecutor blockExecutor;
  private TheoryGenBatchService service;

  /**
   * 纯单测无 MyBatis 引导：LambdaQueryWrapper.select(...) 会即刻解析实体列缓存，
   * 需手动初始化 TableInfo（进程内一次即可，重复调用幂等）。
   */
  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, TheoryGenDraftEntity.class);
    TableInfoHelper.initTableInfo(assistant, TheoryBlueprintNodeEntity.class);
    TableInfoHelper.initTableInfo(assistant, TheoryGenBatchEntity.class);
  }

  @BeforeEach
  void setUp() {
    blueprintMapper = mock(TheoryBlueprintMapper.class);
    nodeMapper = mock(TheoryBlueprintNodeMapper.class);
    batchMapper = mock(TheoryGenBatchMapper.class);
    draftMapper = mock(TheoryGenDraftMapper.class);
    modelConfigMapper = mock(ExamFaqModelConfigMapper.class);
    aiService = mock(TheoryGenAiService.class);
    validator = mock(TheoryQuestionValidator.class);
    transactionTemplate = mock(TransactionTemplate.class);
    doAnswer(inv -> {
      Consumer<TransactionStatus> action = inv.getArgument(0);
      action.accept(mock(TransactionStatus.class));
      return null;
    }).when(transactionTemplate).executeWithoutResult(any());

    blockExecutor = new ThreadPoolTaskExecutor();
    blockExecutor.setCorePoolSize(5);
    blockExecutor.setMaxPoolSize(5);
    blockExecutor.setQueueCapacity(5);
    blockExecutor.initialize();

    service = newService(5);
  }

  @AfterEach
  void tearDown() {
    blockExecutor.shutdown();
  }

  private TheoryGenBatchService newService(int blockConcurrency) {
    return new TheoryGenBatchService(blueprintMapper, nodeMapper, batchMapper, draftMapper,
        modelConfigMapper, aiService, validator, new ObjectMapper(),
        mock(ThreadPoolTaskExecutor.class), blockExecutor, transactionTemplate, blockConcurrency, 0L);
  }

  /** 模拟草稿持久化：insert 的 nodeId 累积到此集合，selectList 据此返回当前已落库草稿 */
  private Set<Long> persistedDraftNodeIds = java.util.concurrent.ConcurrentHashMap.newKeySet(); // 并发块同时落库，HashSet 丢更新会致重派

  // ─────────────────────── 造数辅助 ───────────────────────

  private TheoryBlueprintEntity blueprint() {
    TheoryBlueprintEntity bp = new TheoryBlueprintEntity();
    bp.setId(BP_ID);
    bp.setTenantId(TENANT);
    bp.setLevel(5);
    return bp;
  }

  private TheoryBlueprintEntity blueprint(int level) {
    TheoryBlueprintEntity bp = blueprint();
    bp.setLevel(level);
    return bp;
  }

  private ExamFaqModelConfigEntity config(String baseUrl, String thinking, String effort) {
    ExamFaqModelConfigEntity cfg = new ExamFaqModelConfigEntity();
    cfg.setId(CFG_ID);
    cfg.setStatus("enabled");
    cfg.setApiKey("test-key");
    cfg.setBaseUrl(baseUrl);
    cfg.setModelId("glm-5.3-flash");
    cfg.setThinking(thinking);
    cfg.setReasoningEffort(effort);
    cfg.setMaxTokens(4096);
    return cfg;
  }

  private TheoryBlueprintNodeEntity point(long id, String code) {
    TheoryBlueprintNodeEntity p = new TheoryBlueprintNodeEntity();
    p.setId(id);
    p.setFullCode(code);
    p.setName("考评点" + code);
    p.setNodeLevel(4);
    p.setSortOrder((int) id);
    return p;
  }

  private TheoryGenDraftEntity draftOf(long nodeId) {
    TheoryGenDraftEntity d = new TheoryGenDraftEntity();
    d.setId(nodeId * 10);
    d.setNodeId(nodeId);
    return d;
  }

  private TheoryGenBatchEntity batch(Long runId) {
    TheoryGenBatchEntity b = new TheoryGenBatchEntity();
    b.setId(BATCH_ID);
    b.setTenantId(TENANT);
    b.setBlueprintId(BP_ID);
    b.setModelConfigId(CFG_ID);
    b.setModelId("glm-5.3-flash");
    b.setThinking("enabled");
    b.setReasoningEffort("low");
    b.setQuotaConfig("{\"single\":4,\"judge\":2}");
    b.setStatus(TheoryGenBatchEntity.STATUS_PENDING);
    b.setRunId(runId);
    b.setCreatedBy(USER);
    return b;
  }

  private TheoryQuestionItem question(String content) {
    TheoryQuestionItem q = new TheoryQuestionItem();
    q.setQuestionType("single");
    q.setContent(content);
    q.setAnswer("A");
    TheoryQuestionItem.Option option = new TheoryQuestionItem.Option();
    option.setLabel("A");
    option.setContent("x");
    q.setOptions(List.of(option));
    return q;
  }

  private LlmChatCompletionResult result(String content) {
    return new LlmChatCompletionResult(content, 100, 50, 150, 80);
  }

  private void stubProcessBatch(TheoryGenBatchEntity b, List<TheoryBlueprintNodeEntity> points,
      List<TheoryGenDraftEntity> existingDrafts) {
    persistedDraftNodeIds = java.util.concurrent.ConcurrentHashMap.newKeySet(); // 并发块同时落库，HashSet 丢更新会致重派
    for (TheoryGenDraftEntity d : existingDrafts) {
      persistedDraftNodeIds.add(d.getNodeId());
    }
    when(batchMapper.selectById(eq(BATCH_ID))).thenAnswer(inv -> b);
    when(modelConfigMapper.selectById(eq(CFG_ID))).thenReturn(
        config(GLM_URL, "enabled", "low"));
    when(nodeMapper.selectList(any())).thenReturn(points);
    // 模拟草稿持久化：insert 累积节点，selectList 返回当前已落库集合（多轮补跑的跳过依据）
    when(draftMapper.insert(any(TheoryGenDraftEntity.class))).thenAnswer(inv -> {
      persistedDraftNodeIds.add(((TheoryGenDraftEntity) inv.getArgument(0)).getNodeId());
      return 1;
    });
    when(draftMapper.selectList(any())).thenAnswer(inv -> {
      List<TheoryGenDraftEntity> rows = new ArrayList<>();
      for (Long nodeId : persistedDraftNodeIds) {
        TheoryGenDraftEntity row = new TheoryGenDraftEntity();
        row.setNodeId(nodeId);
        rows.add(row);
      }
      return rows;
    });
    when(batchMapper.updateById(any(TheoryGenBatchEntity.class))).thenReturn(1);
    when(blueprintMapper.selectById(eq(BP_ID))).thenReturn(blueprint());
    when(validator.validate(any())).thenReturn(List.of());
    when(validator.checkDuplicates(any())).thenReturn(List.of());
    when(aiService.generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenReturn(result("ok"));
    when(aiService.parseQuestions(any())).thenReturn(List.of(question("题干")));
  }

  // ─────────────────────── P0：GLM 配置校验 ───────────────────────

  /** OPT-01：GLM + thinking=disabled → 拒绝 */
  @Test
  void opt01_glmThinkingDisabledRejected() {
    when(blueprintMapper.selectById(eq(BP_ID))).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(eq(CFG_ID))).thenReturn(config(GLM_URL, "disabled", "low"));
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID, null));
    assertTrue(ex.getMessage().contains("不可关闭思考"));
  }

  /** OPT-02：reasoning_effort 非法拒绝；合法值放行（放行以进入后续"无考评点"校验为证） */
  @Test
  void opt02_glmReasoningEffortValidation() {
    when(blueprintMapper.selectById(eq(BP_ID))).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(eq(CFG_ID))).thenReturn(config(GLM_URL, "enabled", "medium"));
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID, null));
    assertTrue(ex.getMessage().contains("reasoning_effort"));

    // 合法值 low → 通过校验，流程走到"无考评点"
    when(modelConfigMapper.selectById(eq(CFG_ID))).thenReturn(config(GLM_URL, "enabled", "low"));
    when(nodeMapper.selectList(any())).thenReturn(List.of());
    BusinessException passed = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID, null));
    assertTrue(passed.getMessage().contains("无考评点"));
  }

  /** OPT-03：非 GLM 配置不做该校验（thinking=disabled 放行） */
  @Test
  void opt03_nonGlmSkipsValidation() {
    when(blueprintMapper.selectById(eq(BP_ID))).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(eq(CFG_ID)))
        .thenReturn(config("https://api.deepseek.com", "disabled", "high"));
    when(nodeMapper.selectList(any())).thenReturn(List.of());
    BusinessException passed = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID, null));
    assertTrue(passed.getMessage().contains("无考评点"));
  }

  // ─────────────────────── 主观题轮转配额（指引 §2.1 二级/一级） ───────────────────────

  /** 主观配额键仅二级/一级允许：level 5 传 sub5 → 拒绝 */
  @Test
  void quota_subjectiveKeyRejectedForLevel5() {
    when(blueprintMapper.selectById(eq(BP_ID))).thenReturn(blueprint(5));
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(eq(CFG_ID)))
        .thenReturn(config("https://api.deepseek.com", "disabled", "high"));
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID, Map.of("single", 4, "judge", 2, "sub5", 3)));
    assertTrue(ex.getMessage().contains("仅二级/一级"));
  }

  /** 轮转间隔大于默认 → 主观题总量低于指引下限 → 拒绝 */
  @Test
  void quota_subjectiveIntervalBeyondDefaultRejected() {
    when(blueprintMapper.selectById(eq(BP_ID))).thenReturn(blueprint(2));
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(eq(CFG_ID)))
        .thenReturn(config("https://api.deepseek.com", "disabled", "high"));
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID,
            Map.of("single", 3, "multi", 2, "judge", 2, "sub5", 4)));
    assertTrue(ex.getMessage().contains("低于指引下限"));
  }

  /** 未知配额键拒绝 */
  @Test
  void quota_unknownKeyRejected() {
    when(blueprintMapper.selectById(eq(BP_ID))).thenReturn(blueprint(5));
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(eq(CFG_ID)))
        .thenReturn(config("https://api.deepseek.com", "disabled", "high"));
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID, Map.of("single", 4, "judge", 2, "essay", 1)));
    assertTrue(ex.getMessage().contains("未知题型配额键"));
  }

  /** 二级/一级只传客观三键 → 主观轮转配额按默认补齐并持久化到批次 */
  @Test
  void quota_level2_defaultFillsSubjectiveRotation() {
    when(blueprintMapper.selectById(eq(BP_ID))).thenReturn(blueprint(2));
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(eq(CFG_ID)))
        .thenReturn(config("https://api.deepseek.com", "disabled", "high"));
    when(nodeMapper.selectList(any())).thenReturn(List.of(point(1, "1.1")));

    service.submit(TENANT, USER, BP_ID, CFG_ID, Map.of("single", 3, "multi", 2, "judge", 2));

    ArgumentCaptor<TheoryGenBatchEntity> captor = ArgumentCaptor.forClass(TheoryGenBatchEntity.class);
    verify(batchMapper).insert(captor.capture());
    String quotaJson = captor.getValue().getQuotaConfig();
    assertTrue(quotaJson.contains("\"sub5\":3"), quotaJson);
    assertTrue(quotaJson.contains("\"sub10\":3"), quotaJson);
  }

  // ─────────────────────── P1：断点续跑 / run_id / 原子落库 ───────────────────────

  /** OPT-04：暂停恢复后已完成点跳过（drafts 含点 1 → 仅调 2、3 两块） */
  @Test
  void opt04_resumeSkipsCompletedPoints() {
    TheoryGenBatchEntity b = batch(11L);
    stubProcessBatch(b, List.of(point(1, "1.1"), point(2, "1.2"), point(3, "1.3")),
        List.of(draftOf(1L)));

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    verify(aiService, times(2)).generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    verify(batchMapper, times(2)).incrementDone(eq(BATCH_ID), eq(1), anyLong(), anyLong(), anyLong(), anyLong());
    verify(draftMapper, times(2)).insert(any(TheoryGenDraftEntity.class));
    assertEquals(TheoryGenBatchEntity.STATUS_DONE, b.getStatus());
  }

  /** OPT-05：失败重试只补缺口（drafts 含点 1、3 → 每轮仅重跑 2、4；轮次耗尽 → FAILED 可人工兜底） */
  @Test
  void opt05_retryFailedOnlyRerunsMissing() {
    TheoryGenBatchEntity b = batch(12L);
    stubProcessBatch(b, List.of(point(1, "1.1"), point(2, "1.2"), point(3, "1.3"), point(4, "1.4")),
        List.of(draftOf(1L), draftOf(3L)));
    when(aiService.generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenThrow(new RuntimeException("boom"));

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    // 每轮只重跑缺口点（2、4），4 轮共 8 次调用；轮次耗尽 → FAILED
    verify(aiService, times(8)).generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    verify(batchMapper, times(8)).incrementFailed(eq(BATCH_ID), eq(0L), eq(0L), eq(0L), eq(0L));
    verify(draftMapper, never()).insert(any(TheoryGenDraftEntity.class));
    assertEquals(TheoryGenBatchEntity.STATUS_FAILED, b.getStatus());
    assertTrue(b.getFailReason().contains("自动补跑"));
  }

  /** OPT-06：块落库经事务模板，落库中途异常 → 整块计失败；自动补跑每轮重试（真实回滚由 Spring 事务保证） */
  @Test
  void opt06_draftInsertFailureCountsBlockFailed() {
    TheoryGenBatchEntity b = batch(13L);
    stubProcessBatch(b, List.of(point(1, "1.1")), List.of());
    when(aiService.parseQuestions(any()))
        .thenReturn(List.of(question("q1"), question("q2")));
    when(draftMapper.insert(any(TheoryGenDraftEntity.class)))
        .thenReturn(1)
        .thenThrow(new RuntimeException("db down"));

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    verify(transactionTemplate, times(4)).executeWithoutResult(any());
    // 轮1：insert 1 成功 + 1 异常；轮2/轮3/轮4：首个 insert 即异常
    verify(draftMapper, times(5)).insert(any(TheoryGenDraftEntity.class));
    // 响应已产生 usage：失败块的 token 花费同样入账（P2），每轮均计
    verify(batchMapper, times(4)).incrementFailed(eq(BATCH_ID), eq(150L), eq(100L), eq(50L), eq(80L));
    verify(batchMapper, never()).incrementDone(any(), anyInt(), anyLong(), anyLong(), anyLong(), anyLong());
  }

  /** OPT-07：run_id 竞态——旧编排器（runId=1）读到新编排（runId=2，RUNNING）→ 立即停止派发 */
  @Test
  void opt07_staleOrchestratorStopsOnRunIdMismatch() {
    TheoryGenBatchEntity stale = batch(1L);
    TheoryGenBatchEntity superseded = batch(2L);
    superseded.setStatus(TheoryGenBatchEntity.STATUS_RUNNING);
    when(batchMapper.selectById(eq(BATCH_ID))).thenReturn(stale).thenAnswer(inv -> superseded);

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    verify(aiService, never()).generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    verify(draftMapper, never()).insert(any(TheoryGenDraftEntity.class));
  }

  // ─────────────────────── P2：usage 逐块入账 ───────────────────────

  /** OPT-11：每块 usage 以块为粒度 SQL 原子入账，块间互不串数 */
  @Test
  void opt11_usageReportedPerBlock() {
    TheoryGenBatchEntity b = batch(14L);
    stubProcessBatch(b, List.of(point(1, "1.1"), point(2, "1.2")), List.of());
    AtomicInteger calls = new AtomicInteger();
    when(aiService.generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenAnswer(inv -> calls.incrementAndGet() == 1
            ? new LlmChatCompletionResult("ok", 100, 50, 150, 80)
            : new LlmChatCompletionResult("ok", 20, 10, 30, 0));

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    verify(batchMapper, times(1)).incrementDone(eq(BATCH_ID), eq(1), eq(150L), eq(100L), eq(50L), eq(80L));
    verify(batchMapper, times(1)).incrementDone(eq(BATCH_ID), eq(1), eq(30L), eq(20L), eq(10L), eq(0L));
    assertEquals(TheoryGenBatchEntity.STATUS_DONE, b.getStatus());
  }

  // ─────────────────────── P3：并发编排 ───────────────────────

  /** OPT-12：20 块 N=5 —— 在飞 LLM 调用峰值 ≤ 5，耗时 ≈ ceil(20/5)×单块时延（串行则需 20×时延） */
  @Test
  void opt12_concurrencyBoundsInFlightAndLatency() {
    TheoryGenBatchEntity b = batch(15L);
    List<TheoryBlueprintNodeEntity> points = new ArrayList<>();
    for (long i = 1; i <= 20; i++) {
      points.add(point(i, "1." + i));
    }
    stubProcessBatch(b, points, List.of());
    AtomicInteger inFlight = new AtomicInteger();
    AtomicInteger maxInFlight = new AtomicInteger();
    when(aiService.generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenAnswer(inv -> {
          int now = inFlight.incrementAndGet();
          maxInFlight.accumulateAndGet(now, Math::max);
          try {
            Thread.sleep(120);
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
          inFlight.decrementAndGet();
          return result("ok");
        });

    long t0 = System.nanoTime();
    service.processBatch(BATCH_ID, TENANT, USER, 5);
    long elapsedMs = (System.nanoTime() - t0) / 1_000_000;

    assertEquals(TheoryGenBatchEntity.STATUS_DONE, b.getStatus());
    assertTrue(maxInFlight.get() <= 5, "在飞峰值应 ≤5，实际 " + maxInFlight.get());
    assertTrue(maxInFlight.get() >= 2, "应存在并行调用，实际峰值 " + maxInFlight.get());
    // 4 波 × 120ms = 480ms；给调度留余量下限 400ms，上限远小于串行 2400ms
    assertTrue(elapsedMs >= 400, "耗时应 ≥400ms，实际 " + elapsedMs);
    assertTrue(elapsedMs < 2000, "耗时应明显小于串行 2400ms，实际 " + elapsedMs);
  }

  /** OPT-13：派发中暂停 → 无新块派发，在飞块结果正常落库，状态不被置 DONE */
  @Test
  void opt13_pauseStopsDispatch() {
    TheoryGenBatchEntity running = batch(16L);
    TheoryGenBatchEntity paused = batch(16L);
    paused.setStatus(TheoryGenBatchEntity.STATUS_PAUSED);
    when(modelConfigMapper.selectById(eq(CFG_ID))).thenReturn(config(GLM_URL, "enabled", "low"));
    when(nodeMapper.selectList(any()))
        .thenReturn(List.of(point(1, "1.1"), point(2, "1.2"), point(3, "1.3")));
    when(draftMapper.selectList(any())).thenReturn(List.of());
    when(blueprintMapper.selectById(eq(BP_ID))).thenReturn(blueprint());
    when(validator.validate(any())).thenReturn(List.of());
    when(validator.checkDuplicates(any())).thenReturn(List.of());
    AtomicInteger calls = new AtomicInteger();
    // 首次读批次（初始化）→ running；其后所有状态检查 → paused
    when(batchMapper.selectById(eq(BATCH_ID))).thenAnswer(inv ->
        calls.incrementAndGet() == 1 ? running : paused);

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    verify(aiService, never()).generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    assertEquals(TheoryGenBatchEntity.STATUS_PAUSED, paused.getStatus());
  }

  /** OPT-14：并发下熔断——按块完成顺序连续失败 5 次 → 停止派发，批次 FAILED */
  @Test
  void opt14_consecutiveFailureCircuitBreaker() {
    TheoryGenBatchService single = newService(1);
    TheoryGenBatchEntity b = batch(17L);
    List<TheoryBlueprintNodeEntity> points = new ArrayList<>();
    for (long i = 1; i <= 6; i++) {
      points.add(point(i, "1." + i));
    }
    stubProcessBatch(b, points, List.of());
    when(aiService.generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenThrow(new RuntimeException("boom"));

    single.processBatch(BATCH_ID, TENANT, USER, 5);

    verify(aiService, times(5)).generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    verify(batchMapper, times(5)).incrementFailed(eq(BATCH_ID), eq(0L), eq(0L), eq(0L), eq(0L));
    assertEquals(TheoryGenBatchEntity.STATUS_FAILED, b.getStatus());
    assertTrue(b.getFailReason().contains("连续"));
  }

  /** OPT-15：并发块各自 set/clear TenantContextHolder，无租户串号 */
  @Test
  void opt15_tenantContextPerWorker() {
    TheoryGenBatchEntity b = batch(18L);
    List<TheoryBlueprintNodeEntity> points = new ArrayList<>();
    for (long i = 1; i <= 4; i++) {
      points.add(point(i, "1." + i));
    }
    stubProcessBatch(b, points, List.of());
    List<Long> seenTenants = new CopyOnWriteArrayList<>();
    when(aiService.generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenAnswer(inv -> {
          seenTenants.add(TenantContextHolder.getTenantId());
          return result("ok");
        });

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    assertEquals(4, seenTenants.size());
    for (Long seen : seenTenants) {
      assertEquals(TENANT, seen);
    }
  }

  /** OPT-16：P1+P3 组合——暂停时在飞块完成（草稿已存在）后恢复，该块被跳过不重复调用 */
  @Test
  void opt16_inFlightBlockCompletedBeforeResumeIsSkipped() {
    TheoryGenBatchEntity b = batch(19L);
    stubProcessBatch(b,
        List.of(point(1, "1.1"), point(2, "1.2"), point(3, "1.3"), point(4, "1.4")),
        List.of(draftOf(1L), draftOf(2L)));

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    verify(aiService, times(2)).generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    verify(batchMapper, times(2)).incrementDone(eq(BATCH_ID), eq(1), anyLong(), anyLong(), anyLong(), anyLong());
    assertEquals(TheoryGenBatchEntity.STATUS_DONE, b.getStatus());
  }

  /**
   * OPT-17（E2E 2096462034129305601 发现的竞态回归）：熔断触发后 join 阶段在飞块成功，
   * 实时计数器被清零，批次曾被误标为 done 且剩余考评点未派发。
   *
   * <p>确定性场景：N=2。派发时序由信号量严格约束——1.1~1.5 慢失败（各 300ms，计数逐个到 5），
   * 1.6 慢成功（10s，占用一个 permit 直到 join）。1.7 的 acquire 必然阻塞到 1.5 释放
   * （释放严格晚于其计数自增），post-acquire 必然读到 5 → 熔断，1.7~1.10 不派发。
   * join 等待 1.6 至 10s 完成并清零计数器——旧实现在此误标 done，修复后必须 FAILED。
   */
  @Test
  void opt17_breakerTrippedBatchStaysFailedDespiteInFlightSuccess() throws InterruptedException {
    TheoryGenBatchService pairExecutorService = newService(2);
    TheoryGenBatchEntity b = batch(20L);
    List<TheoryBlueprintNodeEntity> points = new ArrayList<>();
    for (long i = 1; i <= 10; i++) {
      points.add(point(i, "1." + i));
    }
    stubProcessBatch(b, points, List.of());
    when(aiService.generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenAnswer(inv -> {
          String userPrompt = inv.getArgument(8);
          if (userPrompt.startsWith("考评点代码：1.6")) {
            // 慢成功块：跨越熔断触发时刻仍在飞，join 阶段成功会清零计数器
            Thread.sleep(10_000);
            return result("ok");
          }
          if (userPrompt.startsWith("考评点代码：1.1")
              || userPrompt.startsWith("考评点代码：1.2")
              || userPrompt.startsWith("考评点代码：1.3")
              || userPrompt.startsWith("考评点代码：1.4")
              || userPrompt.startsWith("考评点代码：1.5")) {
            // 慢失败块：N=2 下两两成对推进，计数递增节奏可被检查点稳定观察到
            Thread.sleep(300);
            throw new RuntimeException("boom");
          }
          // 1.7~1.10：不应被派发到（熔断先触发）
          throw new RuntimeException("boom-fast");
        });

    pairExecutorService.processBatch(BATCH_ID, TENANT, USER, 5);

    assertEquals(TheoryGenBatchEntity.STATUS_FAILED, b.getStatus());
    assertTrue(b.getFailReason().contains("连续"));
    verify(aiService, times(6)).generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    verify(batchMapper, times(1)).incrementDone(eq(BATCH_ID), eq(1), anyLong(), anyLong(), anyLong(), anyLong());
    verify(batchMapper, times(5)).incrementFailed(eq(BATCH_ID), eq(0L), eq(0L), eq(0L), eq(0L));
  }

  /** OPT-18：自动补跑——1.1 首轮失败后，第 2 轮自动只补跑该点并成功 → 批次置 DONE（200/200 型场景） */
  @Test
  void opt18_autoRetryCompletesMissingPoints() {
    TheoryGenBatchEntity b = batch(21L);
    stubProcessBatch(b, List.of(point(1, "1.1"), point(2, "1.2"), point(3, "1.3")), List.of());
    Map<String, AtomicInteger> failOnce = new HashMap<>();
    when(aiService.generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenAnswer(inv -> {
          String code = inv.<String>getArgument(8).split("\n")[0].replace("考评点代码：", "");
          boolean firstFailure = "1.1".equals(code)
              && failOnce.computeIfAbsent(code, k -> new AtomicInteger()).incrementAndGet() == 1;
          if (firstFailure) {
            throw new RuntimeException("transient");
          }
          return result("ok");
        });

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    assertEquals(TheoryGenBatchEntity.STATUS_DONE, b.getStatus());
    // 轮1：3 块（1.1 失败）；轮2：仅补跑 1.1（成功）
    verify(aiService, times(4)).generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    verify(batchMapper, times(3)).incrementDone(eq(BATCH_ID), eq(1), anyLong(), anyLong(), anyLong(), anyLong());
    verify(batchMapper, times(1)).incrementFailed(eq(BATCH_ID), eq(0L), eq(0L), eq(0L), eq(0L));
  }

  /** OPT-19：补跑轮次耗尽——持久失败点 3 轮均失败 → 批次 FAILED（人工"重试失败块"兜底） */
  @Test
  void opt19_roundsExhaustedMarksFailed() {
    TheoryGenBatchEntity b = batch(22L);
    stubProcessBatch(b, List.of(point(1, "1.1"), point(2, "1.2")), List.of());
    when(aiService.generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenAnswer(inv -> {
          String code = inv.<String>getArgument(8).split("\n")[0].replace("考评点代码：", "");
          if ("1.1".equals(code)) {
            throw new RuntimeException("persist");
          }
          return result("ok");
        });

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    assertEquals(TheoryGenBatchEntity.STATUS_FAILED, b.getStatus());
    assertTrue(b.getFailReason().contains("自动补跑"));
    // 1.2 轮1成功即跳过；1.1 四轮各失败一次
    verify(aiService, times(5)).generateForPoint(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    verify(batchMapper, times(4)).incrementFailed(eq(BATCH_ID), eq(0L), eq(0L), eq(0L), eq(0L));
  }

  /** OPT-20：人工兜底——"已完成但有缺口"的批次可 retryFailed（置回 pending + 换 runId）；无缺口完成批次拒绝 */
  @Test
  void opt20_manualRetryAllowsDoneWithGapOnly() {
    TheoryGenBatchEntity gapped = batch(23L);
    gapped.setStatus(TheoryGenBatchEntity.STATUS_DONE);
    gapped.setTotalPoints(200);
    gapped.setDonePoints(144);
    when(batchMapper.selectById(eq(BATCH_ID))).thenReturn(gapped);
    when(batchMapper.updateById(any(TheoryGenBatchEntity.class))).thenReturn(1);
    when(blueprintMapper.selectById(eq(BP_ID))).thenReturn(blueprint());

    service.retryFailed(TENANT, USER, BATCH_ID);

    assertEquals(TheoryGenBatchEntity.STATUS_PENDING, gapped.getStatus());
    assertTrue(!gapped.getRunId().equals(23L), "重试必须换新 run_id（编排独占权）");

    TheoryGenBatchEntity complete = batch(24L);
    complete.setStatus(TheoryGenBatchEntity.STATUS_DONE);
    complete.setTotalPoints(200);
    complete.setDonePoints(200);
    when(batchMapper.selectById(eq(BATCH_ID))).thenReturn(complete);
    assertThrows(BusinessException.class, () -> service.retryFailed(TENANT, USER, BATCH_ID));
  }

  /** OPT-21：运行时 thinking/reasoning_effort 跟随配置表当前值（批次启动时 selectById 固化），
   * 不用提交时的旧快照——改配置后旧批次的重试/补跑即生效 */
  @Test
  void opt21_runtimeThinkingEffortFollowsConfig() {
    TheoryGenBatchEntity b = batch(25L); // 旧批次快照：enabled/low
    stubProcessBatch(b, List.of(point(1, "1.1")), List.of());
    // 配置表已改为 enabled/high（运行时 selectById 实时读到）
    when(modelConfigMapper.selectById(eq(CFG_ID))).thenReturn(config(GLM_URL, "enabled", "high"));

    service.processBatch(BATCH_ID, TENANT, USER, 5);

    // LLM 调用实际收到配置表当前值 high，而非批次快照 low
    verify(aiService, times(1)).generateForPoint(any(), any(), any(), any(),
        eq("enabled"), eq("high"), anyInt(), any(), any());
    verify(aiService, never()).generateForPoint(any(), any(), any(), any(),
        any(), eq("low"), anyInt(), any(), any());
    // 草稿留痕记录实际生效值（ai_model/ai_thinking 取配置表）
    org.mockito.ArgumentCaptor<TheoryGenDraftEntity> cap =
        org.mockito.ArgumentCaptor.forClass(TheoryGenDraftEntity.class);
    verify(draftMapper, times(1)).insert(cap.capture());
    assertEquals("glm-5.3-flash", cap.getValue().getAiModel());
    assertEquals("enabled", cap.getValue().getAiThinking());
  }
}
