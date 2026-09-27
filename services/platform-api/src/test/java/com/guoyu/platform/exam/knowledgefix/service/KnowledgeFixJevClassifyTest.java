package com.guoyu.platform.exam.knowledgefix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.JevDecisionResult;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.infrastructure.JevDecisionClient;
import com.guoyu.platform.ai.infrastructure.JevUnauthorizedException;
import com.guoyu.platform.ai.infrastructure.JevValidationException;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Jev 分层改造单测（方案 §11.2/§11.3 v0.8 口径）：JevDecisionClient 全 mock 桩；
 * 双门/keep/卫兵/直收尾/降级/通道开关/熔断/统计/pausing 三态互斥/启动恢复。
 */
class KnowledgeFixJevClassifyTest {

  private static final Long TENANT = 10159L;
  private static final Long USER = 9L;
  private static final Long BATCH_ID = 900L;
  private static final Long BATCH_NO = 901L;

  private ExamKnowledgeFixBatchMapper batchMapper;
  private ExamDetailKnowledgeFixMapper fixMapper;
  private ExamExamDetailFixMapper queryMapper;
  private ExamFaqModelConfigMapper modelConfigMapper;
  private FaqLlmClient llmClient;
  private JevDecisionClient jevClient;
  private ExamKnowledgeFixLlmLogMapper llmLogMapper;
  private ThreadPoolTaskExecutor llmExecutor;
  private KnowledgeFixClassifyService service;

  private ExamFaqModelConfigEntity cfgFlash;
  private ExamFaqModelConfigEntity cfgPro;
  private ExamKnowledgeFixBatchEntity batch;
  private final Map<Long, String> statusByRowId = new LinkedHashMap<>();
  private final Map<Long, String> basisByRowId = new LinkedHashMap<>();

  @BeforeEach
  void setUp() {
    batchMapper = mock(ExamKnowledgeFixBatchMapper.class);
    fixMapper = mock(ExamDetailKnowledgeFixMapper.class);
    queryMapper = mock(ExamExamDetailFixMapper.class);
    modelConfigMapper = mock(ExamFaqModelConfigMapper.class);
    llmClient = mock(FaqLlmClient.class);
    jevClient = mock(JevDecisionClient.class);
    llmLogMapper = mock(ExamKnowledgeFixLlmLogMapper.class);
    statusByRowId.clear();
    basisByRowId.clear();

    ThreadPoolTaskExecutor orchestrator = new ThreadPoolTaskExecutor();
    orchestrator.setCorePoolSize(1);
    orchestrator.setMaxPoolSize(1);
    orchestrator.setQueueCapacity(10);
    orchestrator.initialize();

    llmExecutor = new ThreadPoolTaskExecutor();
    llmExecutor.setCorePoolSize(3);
    llmExecutor.setMaxPoolSize(3);
    llmExecutor.setQueueCapacity(20);
    llmExecutor.initialize();

    cfgFlash = cfg("theory-gen-glm", "glm-5.3-flash");
    cfgPro = cfg("knowledge-fix-pro", "glm-5.3");
    batch = batchEntity();

    service = new KnowledgeFixClassifyService(batchMapper, fixMapper, queryMapper,
        modelConfigMapper, llmClient, jevClient, new ObjectMapper(), orchestrator, llmExecutor,
        llmLogMapper, 3, 0L);
  }

  @AfterEach
  void tearDown() {
    llmExecutor.shutdown();
  }

  // ───────── 造数 ─────────

  private ExamFaqModelConfigEntity cfg(String code, String modelId) {
    ExamFaqModelConfigEntity c = new ExamFaqModelConfigEntity();
    c.setId(1L);
    c.setTenantId(TENANT);
    c.setModelCode(code);
    c.setModelId(modelId);
    c.setBaseUrl("https://jev.test/gateway");
    c.setApiKey("test-key");
    c.setStatus("enabled");
    return c;
  }

  private ExamKnowledgeFixBatchEntity batchEntity() {
    ExamKnowledgeFixBatchEntity b = new ExamKnowledgeFixBatchEntity();
    b.setId(BATCH_ID);
    b.setTenantId(TENANT);
    b.setBatchNo(BATCH_NO);
    b.setTotal(1);
    b.setStatus(ExamKnowledgeFixBatchEntity.STATUS_PENDING);
    b.setCreatedBy(USER);
    return b;
  }

  private KnowledgeFixClassifyService.JevChannel jevChannel(BigDecimal theta1, BigDecimal theta2) {
    return new KnowledgeFixClassifyService.JevChannel(cfg("knowledge-fix-jev", "bocha-jev-v1"),
        theta1, theta2, 5, 30);
  }

  private KnowledgeNodeRow node(int id, int courseId, int parentId, String title, int level,
      String important) {
    KnowledgeNodeRow n = new KnowledgeNodeRow();
    n.setId(id);
    n.setCourseId(courseId);
    n.setParentId(parentId);
    n.setTitle(title);
    n.setLevel(level);
    n.setImportant(important);
    return n;
  }

  /** 课程 190 树：L2=201 蒸发系统（L3: 301/302）；L2=202 焙烧系统（L3: 仅 303——单候选场景）；
   *  L2=203 安全规程（秃章节）；老节点 4192 工匠精神培养（挂在 201 下） */
  private List<KnowledgeNodeRow> courseTree() {
    List<KnowledgeNodeRow> nodes = new ArrayList<>();
    nodes.add(node(200, 190, 0, "蒸发与焙烧单元", 1, "X"));
    nodes.add(node(201, 190, 200, "蒸发系统", 2, "X"));
    nodes.add(node(202, 190, 200, "焙烧系统", 2, "Y"));
    nodes.add(node(203, 190, 200, "安全规程", 2, "X"));
    nodes.add(node(301, 190, 201, "蒸发器跑碱处理", 3, "X"));
    nodes.add(node(302, 190, 201, "蒸发站真空调整", 3, "Y"));
    nodes.add(node(303, 190, 202, "焙烧温度控制", 3, "Z"));
    nodes.add(node(4192, 190, 201, "工匠精神培养", 3, "X"));
    return nodes;
  }

  private PendingFixRow row(long rowId, int detailId, int oldKnowledgeId, int courseId,
      String stem) {
    PendingFixRow r = new PendingFixRow();
    r.setRowId(rowId);
    r.setDetailId(detailId);
    r.setOldKnowledgeId(oldKnowledgeId);
    r.setCourseId(courseId);
    r.setContent("<p>" + stem + "</p>");
    r.setAnswer(null);
    r.setResolve(null);
    statusByRowId.put(rowId, "pending");
    return r;
  }

  private JevDecisionResult jevResult(String choice, double conf) {
    Map<String, Double> probs = new LinkedHashMap<>();
    probs.put(choice, conf);
    double rest = Math.max(0, (1.0 - conf) / 2);
    probs.put(choice + "-alt1", rest);
    probs.put(choice + "-alt2", rest);
    return new JevDecisionResult(choice, conf, probs, 120, 0,
        "maximum candidate probability; not guaranteed correctness", "bocha-jev-v1", 50, false);
  }

  private void stubJevTwoStage(String l2Choice, double l2Conf, String l3Choice, double l3Conf) {
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
        anyString(), anyMap(), anyInt())).thenReturn(jevResult(l2Choice, l2Conf));
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l3"),
        anyString(), anyMap(), anyInt())).thenReturn(jevResult(l3Choice, l3Conf));
  }

  private void stubCommonHappyPath() {
    when(batchMapper.selectById(BATCH_ID)).thenAnswer(inv -> batch);
    when(batchMapper.updateById(ArgumentMatchers
        .<ExamKnowledgeFixBatchEntity>any())).thenReturn(1);
    when(queryMapper.selectAllNodes()).thenReturn(courseTree());
    when(fixMapper.updateClassifyResult(anyLong(), any(), any(), any(), any(), any()))
        .thenAnswer(inv -> {
          statusByRowId.put(inv.getArgument(0), inv.getArgument(5));
          basisByRowId.put(inv.getArgument(0), inv.getArgument(3));
          return 1;
        });
    when(batchMapper.incrementClassified(anyLong(), anyString())).thenAnswer(inv -> {
      synchronized (batch) {
        batch.setDone(nz(batch.getDone()) + 1);
        String outcome = inv.getArgument(1);
        if ("apply".equals(outcome)) {
          batch.setApplyCnt(nz(batch.getApplyCnt()) + 1);
        } else if ("keep".equals(outcome)) {
          batch.setKeepCnt(nz(batch.getKeepCnt()) + 1);
        } else {
          batch.setFlagCnt(nz(batch.getFlagCnt()) + 1);
        }
      }
      return 1;
    });
    when(batchMapper.incrementFailed(anyLong())).thenAnswer(inv -> {
      synchronized (batch) {
        batch.setFailed(nz(batch.getFailed()) + 1);
      }
      return 1;
    });
    when(batchMapper.incrementUnclassified(anyLong(), anyInt())).thenAnswer(inv -> 1);
  }

  private void stubLlmHappyPath() {
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> new LlmChatCompletionResult("{\"l2_id\":201}", 10, 5, 15, 0));
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> new LlmChatCompletionResult(
            "{\"node_id\":301,\"confidence\":0.9,\"reason\":\"llm 兜底理由\"}", 10, 5, 15, 0));
  }

  private void runBatch(KnowledgeFixClassifyService.JevChannel jev, PendingFixRow... rows) {
    List<PendingFixRow> rowList = List.of(rows);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rowList.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro, jev);
  }

  private int nz(Integer v) {
    return v == null ? 0 : v;
  }

  /** 单 worker 串行服务（llmConcurrency=1）：熔断/401 用例的计数时序确定性化 */
  private KnowledgeFixClassifyService sequentialService() {
    ThreadPoolTaskExecutor single = new ThreadPoolTaskExecutor();
    single.setCorePoolSize(1);
    single.setMaxPoolSize(1);
    single.setQueueCapacity(20);
    single.initialize();
    return new KnowledgeFixClassifyService(batchMapper, fixMapper, queryMapper, modelConfigMapper,
        llmClient, jevClient, new ObjectMapper(),
        new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor(), single,
        llmLogMapper, 1, 0L);
  }

  // ───────── §11.2 分层与门禁 ─────────

  /** JEV-01：双门通过（conf₁=0.9≥0.5、conf₂=0.55≥0.5）且非当前节点 → apply，basis=jev，reason 为空 */
  @Test
  void jev01_dualGateAppliesWithEmptyReason() {
    stubCommonHappyPath();
    stubJevTwoStage("201", 0.9, "301", 0.55);
    PendingFixRow r = row(1, 1, 4192, 190, "蒸发器跑碱的原因");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    ArgumentCaptor<String> basisCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> reasonCap = ArgumentCaptor.forClass(String.class);
    verify(fixMapper).updateClassifyResult(eq(1L), eq(301), any(), basisCap.capture(),
        reasonCap.capture(), eq(ExamDetailKnowledgeFixEntity.STATUS_APPLY));
    assertEquals("jev", basisCap.getValue());
    assertEquals("", reasonCap.getValue(), "Jev 行无理由文本");
  }

  /** JEV-02：conf₂=0.45 < θ₂=0.5 → 走 LLM 三采样分支，最终 basis=llm，兜底计数 +1 */
  @Test
  void jev02_theta2GateFallsBackToLlm() {
    stubCommonHappyPath();
    stubJevTwoStage("201", 0.9, "301", 0.45);
    stubLlmHappyPath();
    PendingFixRow r = row(1, 1, 4192, 190, "蒸发器跑碱的原因");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    assertEquals("llm", basisByRowId.get(1L));
    assertEquals(0, nz(batch.getJevClassified()));
    assertEquals(1, nz(batch.getLlmFallback()));
  }

  /** JEV-03：conf₁=0.4 < θ₁ → 直接降级，阶段 2 不发（Jev 仅 1 次调用） */
  @Test
  void jev03_theta1GateSkipsStage2() {
    stubCommonHappyPath();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
        anyString(), anyMap(), anyInt())).thenReturn(jevResult("201", 0.4));
    stubLlmHappyPath();
    PendingFixRow r = row(1, 1, 4192, 190, "蒸发器跑碱的原因");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    verify(jevClient, times(1)).decide(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyMap(), anyInt());
    verify(jevClient, never()).decide(anyString(), anyString(), anyString(), anyString(),
        eq("l3"), anyString(), anyMap(), anyInt());
    assertEquals("llm", basisByRowId.get(1L));
  }

  /** JEV-04：阈值配置化——θ₂=0.4 后 conf₂=0.45 的行转 apply（通道配置传入即生效） */
  @Test
  void jev04_theta2Configurable() {
    stubCommonHappyPath();
    stubJevTwoStage("201", 0.9, "301", 0.45);
    PendingFixRow r = row(1, 1, 4192, 190, "蒸发器跑碱的原因");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.4")), r);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    assertEquals("jev", basisByRowId.get(1L));
  }

  /** JEV-05：θ₂ < 0.3 硬底线 → 配置校验拒绝（CONFLICT，§7.1） */
  @Test
  void jev05_theta2BelowHardFloorRejected() {
    ExamFaqModelConfigEntity jevCfg = cfg("knowledge-fix-jev", "bocha-jev-v1");
    jevCfg.setJevTheta1(new BigDecimal("0.5"));
    jevCfg.setJevTheta2(new BigDecimal("0.2"));
    when(modelConfigMapper.selectList(ArgumentMatchers.any()))
        .thenReturn(List.of(cfgFlash))
        .thenReturn(List.of(cfgPro))
        .thenReturn(List.of(jevCfg));
    when(queryMapper.populateFixList()).thenReturn(0);

    BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(TENANT, USER));
    assertTrue(ex.getMessage().contains("0.3"), "应提示硬底线 0.3：" + ex.getMessage());
    verify(queryMapper, never()).populateFixList();
  }

  /** JEV-06：Jev 选中当前节点 → keep（阈值不拦截 keep，即使 conf₂=0.2） */
  @Test
  void jev06_keepIgnoresThreshold() {
    stubCommonHappyPath();
    stubJevTwoStage("201", 0.9, "4192", 0.2);
    PendingFixRow r = row(1, 1, 4192, 190, "工匠精神的内涵");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_KEEP, statusByRowId.get(1L));
    assertEquals("jev", basisByRowId.get(1L));
    assertEquals(1, nz(batch.getJevClassified()));
  }

  /** JEV-07：零重合卫兵——Jev 高置信但选中节点与题干零重合且非素养节点 → flag（卫兵优先于置信分层） */
  @Test
  void jev07_guardFlagsZeroOverlap() {
    stubCommonHappyPath();
    stubJevTwoStage("201", 0.95, "303", 0.95);
    PendingFixRow r = row(1, 1, 4192, 190, "蒸发器跑碱的原因排查");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_FLAG, statusByRowId.get(1L));
    ArgumentCaptor<String> reasonCap = ArgumentCaptor.forClass(String.class);
    verify(fixMapper).updateClassifyResult(eq(1L), eq(303), any(), any(), reasonCap.capture(),
        eq(ExamDetailKnowledgeFixEntity.STATUS_FLAG));
    assertTrue(reasonCap.getValue().contains("卫兵拦截"), reasonCap.getValue());
  }

  /** JEV-08：所选 L2 下无存活 L3（秃章节 203）→ jev-l2-direct 直收尾，只调 1 次 Jev */
  @Test
  void jev08_l2DirectWhenNoSurvivingL3() {
    stubCommonHappyPath();
    stubJevTwoStage("203", 0.9, "301", 0.9);
    PendingFixRow r = row(1, 1, 4192, 190, "安全规程相关题");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    ArgumentCaptor<String> basisCap = ArgumentCaptor.forClass(String.class);
    verify(fixMapper).updateClassifyResult(eq(1L), eq(203), any(), basisCap.capture(), any(),
        eq(ExamDetailKnowledgeFixEntity.STATUS_APPLY));
    assertEquals("jev-l2-direct", basisCap.getValue());
    verify(jevClient, times(1)).decide(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyMap(), anyInt());
    assertEquals(1, nz(batch.getJevClassified()));
  }

  /** JEV-09：课程无存活 L2（criteria 空 <2）→ 0 次 Jev 直接降级 LLM（§5 首行/前置拦截）；
   *  LLM 兜底阶段 1 对无树课程越界 → 卫兵拒绝语义，行最终收尾 unclassified */
  @Test
  void jev09_courseWithoutL2NeverCallsJev() {
    stubCommonHappyPath();
    stubLlmHappyPath();
    PendingFixRow r = row(1, 1, 4192, 999, "无树课程题");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    verify(jevClient, never()).decide(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyMap(), anyInt());
    // LLM 兜底路径 stage1 返回 201 但该课程无 L2 → 守卫拒绝 → 轮次耗尽收尾 unclassified
    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_FLAG, statusByRowId.get(1L));
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_FAILED, batch.getStatus());
  }

  /** JEV-10：所选 L2 下仅 1 个 L3（单候选 <2）→ 降级 LLM，阶段 2 不发（实测 min_length=2） */
  @Test
  void jev10_singleL3CandidateFallsBack() {
    stubCommonHappyPath();
    stubJevTwoStage("202", 0.9, "303", 0.9);
    stubLlmHappyPath();
    PendingFixRow r = row(1, 1, 4192, 190, "焙烧温度相关题");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    verify(jevClient, times(1)).decide(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyMap(), anyInt());
    verify(jevClient, never()).decide(anyString(), anyString(), anyString(), anyString(),
        eq("l3"), anyString(), anyMap(), anyInt());
    assertEquals("llm", basisByRowId.get(1L));
  }

  /** JEV-11：Jev 连续失败 5 次 → 本轮停派发 Jev，剩余行全降级 LLM，批次不判 failed（§5 熔断）。
   *  单 worker 串行派发，保证熔断计数时序确定（并发下计数与派发交错会使 times 断言抖动） */
  @Test
  void jev11_fiveConsecutiveJevFailuresTripRoundBreaker() {
    stubCommonHappyPath();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyString(), anyMap(), anyInt())).thenThrow(
        new com.guoyu.platform.ai.infrastructure.JevServerException("boom"));
    stubLlmHappyPath();
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 6; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(6);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    sequentialService().processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro,
        jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")));

    // 行 1~5 各消耗 1 次 Jev 失败（第 5 次触发本轮熔断）；第 6 行起不再调 Jev
    verify(jevClient, times(5)).decide(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyMap(), anyInt());
    for (int i = 1; i <= 6; i++) {
      assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get((long) i),
          "第 " + i + " 行应经 LLM 兜底拿到结论");
    }
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** JEV-12：401 → 立即熔断整批转 LLM（permanent，跨行生效），批次正常完成（单 worker 串行保证时序） */
  @Test
  void jev12_unauthorizedOpensPermanentBreaker() {
    stubCommonHappyPath();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyString(), anyMap(), anyInt())).thenThrow(new JevUnauthorizedException("API 密钥不存在"));
    stubLlmHappyPath();
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(3);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    sequentialService().processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro,
        jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")));

    verify(jevClient, times(1)).decide(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyMap(), anyInt());
    for (int i = 1; i <= 3; i++) {
      assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get((long) i));
    }
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** JEV-13：422 → 该行记失败并跳过（不走 LLM），行保持 pending；轮次耗尽后未分类收尾 → 批次 failed */
  @Test
  void jev13_validationErrorSkipsRowAndCountsFailure() {
    stubCommonHappyPath();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyString(), anyMap(), anyInt())).thenThrow(new JevValidationException("criteria 至少 2 个"));
    PendingFixRow r = row(1, 1, 4192, 190, "蒸发器跑碱的原因");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    verify(batchMapper, atLeastOnce()).incrementFailed(BATCH_NO);
    verify(llmClient, never()).chatCompletionDetailed(anyString(), anyString(), anyString(),
        anyString(), anyString(), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt());
    // 3 轮后收尾扫为 unclassified-flag；未分类 1/1 > 5% → failed
    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_FLAG, statusByRowId.get(1L));
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_FAILED, batch.getStatus());
    assertEquals(0, nz(batch.getLlmFallback()), "422 跳过行不走 LLM，不计入兜底");
  }

  /** JEV-14：通道关闭（jev=null）→ 0 次 Jev，全走 LLM，llm_fallback = 行数 */
  @Test
  void jev14_channelOffRunsAllLlm() {
    stubCommonHappyPath();
    stubLlmHappyPath();
    PendingFixRow r = row(1, 1, 4192, 190, "蒸发器跑碱的原因");

    runBatch(null, r);

    verify(jevClient, never()).decide(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyMap(), anyInt());
    assertEquals(0, nz(batch.getJevClassified()));
    assertEquals(1, nz(batch.getLlmFallback()));
    assertNull(batch.getJevAvgConfidence());
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** JEV-15：越界 choice → 卫兵拒绝语义降级 LLM，不计失败（incrementFailed 零调用） */
  @Test
  void jev15_outOfRangeChoiceFallsBackWithoutFailureCount() {
    stubCommonHappyPath();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
        anyString(), anyMap(), anyInt())).thenReturn(jevResult("99999", 0.9));
    stubLlmHappyPath();
    PendingFixRow r = row(1, 1, 4192, 190, "蒸发器跑碱的原因");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    verify(batchMapper, never()).incrementFailed(anyLong());
    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    assertEquals("llm", basisByRowId.get(1L));
  }

  /** JEV-16：批次统计落库——jev_classified / llm_fallback / jev_avg_confidence 随收尾写入批次行 */
  @Test
  void jev16_batchStatsWrittenAtFinalize() {
    stubCommonHappyPath();
    stubJevTwoStage("201", 0.9, "301", 0.7);
    // 行 1 走 Jev apply（conf 0.7）；行 2 无树课程 → LLM 兜底
    PendingFixRow r1 = row(1, 1, 4192, 190, "蒸发器跑碱的原因");
    PendingFixRow r2 = row(2, 2, 4192, 999, "无树课程题");
    batch.setTotal(2);

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r1, r2);

    assertEquals(1, nz(batch.getJevClassified()));
    assertEquals(1, nz(batch.getLlmFallback()));
    assertNotNull(batch.getJevAvgConfidence());
    assertEquals(0, batch.getJevAvgConfidence().compareTo(new BigDecimal("0.7000")));
  }

  /** JEV-17：Jev 审计落库——stage/model(响应)/confidence_definition/top1/top2；输出 token 记实际值(0) */
  @Test
  void jev17_jevCallsAuditedWithMetadata() {
    stubCommonHappyPath();
    stubJevTwoStage("201", 0.9, "301", 0.7);
    PendingFixRow r = row(1, 1, 4192, 190, "蒸发器跑碱的原因");

    runBatch(jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), r);

    ArgumentCaptor<ExamKnowledgeFixLlmLogEntity> cap =
        ArgumentCaptor.forClass(ExamKnowledgeFixLlmLogEntity.class);
    verify(llmLogMapper, times(2)).insert(cap.capture());
    List<ExamKnowledgeFixLlmLogEntity> logs = cap.getAllValues();
    assertEquals("jev-stage1", logs.get(0).getStage());
    assertEquals("jev-stage2", logs.get(1).getStage());
    for (ExamKnowledgeFixLlmLogEntity e : logs) {
      assertEquals("bocha-jev-v1", e.getModelId(), "响应 model 优先于配置 model");
      assertEquals("maximum candidate probability; not guaranteed correctness",
          e.getConfidenceDefinition());
      assertEquals(0, e.getCompletionTokens(), "输出 token 实测为 0");
      assertNotNull(e.getTop1Prob());
      assertNotNull(e.getTop2Prob());
    }
  }

  /** JEV-18：Jev 独立并发上限——jev_concurrency=1 时即使 Worker 池 3 线程，同时在飞的 Jev 调用 ≤1（§8.1） */
  @Test
  void jev18_jevConcurrencyCapNotExceeded() {
    stubCommonHappyPath();
    AtomicInteger concurrent = new AtomicInteger();
    AtomicInteger maxConcurrent = new AtomicInteger();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyString(), anyMap(), anyInt())).thenAnswer(inv -> {
          int now = concurrent.incrementAndGet();
          maxConcurrent.accumulateAndGet(now, Math::max);
          try {
            Thread.sleep(120);
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
          concurrent.decrementAndGet();
          return "l2".equals(inv.getArgument(4, String.class))
              ? jevResult("201", 0.9) : jevResult("301", 0.7);
        });
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(3);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    // 默认 service：LLM Worker 池 3 线程并发，Jev 信号量应压到 1
    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro,
        new KnowledgeFixClassifyService.JevChannel(cfg("knowledge-fix-jev", "bocha-jev-v1"),
            new BigDecimal("0.5"), new BigDecimal("0.5"), 1, 30));

    assertEquals(1, maxConcurrent.get(), "Jev 并发上限=1 不得超发，实际峰值 " + maxConcurrent.get());
    assertEquals(3, nz(batch.getJevClassified()));
  }

  // ───────── §13 步骤 8 范围提交（courseId/limit） ─────────

  /** JEV-19：courseId 范围提交——只处理该课程行，范围外行保持 pending（供后续全量批次） */
  @Test
  void jev19_courseScopedSubmitOnlyProcessesThatCourse() {
    stubCommonHappyPath();
    stubJevTwoStage("201", 0.9, "301", 0.7);
    PendingFixRow inScope = row(1, 1, 4192, 190, "蒸发器跑碱的原因");
    PendingFixRow outScope = row(2, 2, 4192, 999, "无树课程题");
    batch.setTotal(1);

    // 直接以 scope 调 processBatch（submit 的 scope 解析已由服务实现，此处验证编排侧过滤）
    List<PendingFixRow> rows = List.of(inScope, outScope);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro,
        jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), 190, null);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    assertEquals("pending", statusByRowId.get(2L), "范围外行不得被处理或收尾");
    assertEquals(1, nz(batch.getDone()));
    verify(batchMapper, never()).incrementUnclassified(anyLong(), anyInt());
  }

  /** JEV-20：limit 截断——3 行只取前 2 行入批，第 3 行保持 pending */
  @Test
  void jev20_limitTruncatesBatchSize() {
    stubCommonHappyPath();
    stubJevTwoStage("201", 0.9, "301", 0.7);
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(2);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro,
        jevChannel(new BigDecimal("0.5"), new BigDecimal("0.5")), null, 2);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(2L));
    assertEquals("pending", statusByRowId.get(3L), "limit 外行保持 pending");
    assertEquals(2, nz(batch.getDone()));
  }

  /** JEV-21：submit 参数校验——limit 越界拒绝（1~1000） */
  @Test
  void jev21_submitRejectsOutOfRangeLimit() {
    assertThrows(BusinessException.class, () -> service.submit(TENANT, USER, null, 0));
    assertThrows(BusinessException.class, () -> service.submit(TENANT, USER, null, 1001));
  }

  // ───────── §11.3 暂停/恢复（v0.8 加固） ─────────

  /** PAUSE-01：pausing 过渡态下 reset 与重复 submit 均被拒（§10 互斥口径） */
  @Test
  void pause01_pausingStateRejectsResetAndSubmit() {
    when(batchMapper.selectCount(any())).thenReturn(1L);

    BusinessException resetEx = assertThrows(BusinessException.class, () -> service.resetClassify());
    assertTrue(resetEx.getMessage().contains("进行中"));
    BusinessException submitEx = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER));
    assertTrue(submitEx.getMessage().contains("进行中"));
  }

  /** PAUSE-02：批次表 pausing 状态（进程重启后内存标志丢失的场景）→ 派发边界双判命中，收尾 paused */
  @Test
  void pause02_pausingBatchStatusStopsDispatchViaDbCheck() {
    stubCommonHappyPath();
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(3);
    batch.setStatus(ExamKnowledgeFixBatchEntity.STATUS_PAUSING);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(0, statusByRowId.values().stream().filter(s -> !"pending".equals(s)).count(),
        "pausing 状态下不得派发任何行");
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_PAUSED, batch.getStatus());
    verify(jevClient, never()).decide(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyMap(), anyInt());
  }

  /** PAUSE-03：pause() 同时写批次表 pausing 状态（内存 + 批次表双判，§10 加固点） */
  @Test
  void pause03_pauseWritesPausingStatusToBatchRow() {
    batch.setStatus(ExamKnowledgeFixBatchEntity.STATUS_RUNNING);
    when(batchMapper.selectList(ArgumentMatchers
        .<com.baomidou.mybatisplus.core.conditions.Wrapper<ExamKnowledgeFixBatchEntity>>any()))
        .thenReturn(List.of(batch));

    service.pause();

    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_PAUSING, batch.getStatus());
    verify(batchMapper).updateById(batch);
  }

  /** PAUSE-04：启动恢复——遗留 pending/running/pausing 批次统一收尾为 paused，可续跑 */
  @Test
  void pause04_startupRecoveryFinalizesAbandonedBatches() {
    ExamKnowledgeFixBatchEntity running = batchEntity();
    running.setStatus(ExamKnowledgeFixBatchEntity.STATUS_RUNNING);
    ExamKnowledgeFixBatchEntity pausing = batchEntity();
    pausing.setId(902L);
    pausing.setStatus(ExamKnowledgeFixBatchEntity.STATUS_PAUSING);
    when(batchMapper.selectList(ArgumentMatchers
        .<com.baomidou.mybatisplus.core.conditions.Wrapper<ExamKnowledgeFixBatchEntity>>any()))
        .thenReturn(List.of(running, pausing));
    when(batchMapper.updateById(ArgumentMatchers.<ExamKnowledgeFixBatchEntity>any()))
        .thenReturn(1);

    service.recoverAbandonedBatchesOnStartup();

    ArgumentCaptor<ExamKnowledgeFixBatchEntity> cap =
        ArgumentCaptor.forClass(ExamKnowledgeFixBatchEntity.class);
    verify(batchMapper, times(2)).updateById(cap.capture());
    for (ExamKnowledgeFixBatchEntity b : cap.getAllValues()) {
      assertEquals(ExamKnowledgeFixBatchEntity.STATUS_PAUSED, b.getStatus());
      assertTrue(b.getFailReason().contains("进程重启收尾"));
    }
  }
}
