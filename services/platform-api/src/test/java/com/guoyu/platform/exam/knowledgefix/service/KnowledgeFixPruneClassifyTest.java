package com.guoyu.platform.exam.knowledgefix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
import com.guoyu.platform.ai.infrastructure.JevValidationException;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 关键词剪枝集成单测（方案 v1.3 附一 KP-09~22）：双通道剪枝口径、开关矩阵、keep 保位、
 * 弱信号回退、审计归因。Jev/LLM 客户端全 mock 桩。
 */
class KnowledgeFixPruneClassifyTest {

  private static final Long TENANT = 10159L;
  private static final Long USER = 9L;
  private static final Long BATCH_ID = 950L;
  private static final Long BATCH_NO = 951L;

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

  private KnowledgeNodeRow node(int id, int courseId, int parentId, String title, int level) {
    KnowledgeNodeRow n = new KnowledgeNodeRow();
    n.setId(id);
    n.setCourseId(courseId);
    n.setParentId(parentId);
    n.setTitle(title);
    n.setLevel(level);
    return n;
  }

  /** 课程 290：8 个 L2（501~508，>5 触发 stage1 宽剪）；501 蒸发系统挂 10 个 L3（>8 触发 stage2 剪枝）；
   *  4192 工匠精神培养挂 506 职业道德下（跨章错挂老节点）；老节点 610 防雷接地施工挂 501 下（keep 保位用） */
  private List<KnowledgeNodeRow> tree() {
    List<KnowledgeNodeRow> nodes = new ArrayList<>();
    nodes.add(node(400, 290, 0, "基本要求", 1));
    String[] l2 = {"蒸发系统", "焙烧系统", "沉降系统", "过滤系统", "通信系统",
        "职业道德", "分析系统", "计量系统"};
    for (int i = 0; i < l2.length; i++) {
      nodes.add(node(501 + i, 290, 400, l2[i], 2));
    }
    String[] evap = {"蒸发器跑碱处理", "蒸发站真空调整", "蒸发器结垢清理", "蒸发强度计算",
        "蒸发室液位控制", "蒸发器传热系数", "蒸发站效数选择", "二次蒸汽利用",
        "不凝汽排放", "防雷接地施工"};
    for (int i = 0; i < evap.length; i++) {
      nodes.add(node(601 + i, 290, 501, evap[i], 3));
    }
    nodes.add(node(611, 290, 502, "焙烧温度控制", 3));
    nodes.add(node(612, 290, 506, "爱岗敬业要求", 3));
    nodes.add(node(4192, 290, 506, "工匠精神培养", 3));
    return nodes;
  }

  private PendingFixRow row(long rowId, int oldKnowledgeId, String stem) {
    PendingFixRow r = new PendingFixRow();
    r.setRowId(rowId);
    r.setDetailId((int) rowId);
    r.setOldKnowledgeId(oldKnowledgeId);
    r.setCourseId(290);
    r.setContent("<p>" + stem + "</p>");
    r.setAnswer(null);
    r.setResolve(null);
    statusByRowId.put(rowId, "pending");
    return r;
  }

  private JevDecisionResult jevResult(String choice, double conf) {
    Map<String, Double> probs = new LinkedHashMap<>();
    probs.put(choice, conf);
    probs.put(choice + "-alt", 1.0 - conf);
    return new JevDecisionResult(choice, conf, probs, 120, 0,
        "maximum candidate probability; not guaranteed correctness", "bocha-jev-v1", 50, false);
  }

  private void stubCommon() {
    when(batchMapper.selectById(BATCH_ID)).thenAnswer(inv -> batch);
    when(batchMapper.updateById(ArgumentMatchers.<ExamKnowledgeFixBatchEntity>any())).thenReturn(1);
    when(queryMapper.selectAllNodes()).thenReturn(tree());
    when(fixMapper.updateClassifyResult(anyLong(), any(), any(), any(), any(), any(), anyLong()))
        .thenAnswer(inv -> {
          statusByRowId.put(inv.getArgument(0), inv.getArgument(5));
          basisByRowId.put(inv.getArgument(0), inv.getArgument(3));
          return 1;
        });
    when(batchMapper.incrementClassified(anyLong(), anyString())).thenAnswer(inv -> {
      String outcome = inv.getArgument(1);
      if ("apply".equals(outcome)) {
        batch.setApplyCnt(nz(batch.getApplyCnt()) + 1);
      } else if ("keep".equals(outcome)) {
        batch.setKeepCnt(nz(batch.getKeepCnt()) + 1);
      } else {
        batch.setFlagCnt(nz(batch.getFlagCnt()) + 1);
      }
      batch.setDone(nz(batch.getDone()) + 1);
      return 1;
    });
    when(batchMapper.incrementFailed(anyLong())).thenReturn(1);
    when(batchMapper.incrementUnclassified(anyLong(), anyInt())).thenReturn(1);
  }

  private void stubLlmStage1(int l2Id) {
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> new LlmChatCompletionResult("{\"l2_id\":" + l2Id + "}", 10, 5, 15, 0));
  }

  private void stubLlmStage2(int nodeId, double conf) {
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> new LlmChatCompletionResult(
            "{\"node_id\":" + nodeId + ",\"confidence\":" + conf + ",\"reason\":\"llm 理由\"}",
            10, 5, 15, 0));
  }

  private void stubJev(int l2Id, double l2Conf, int nodeId, double nodeConf) {
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
        anyString(), anyMap(), anyInt())).thenReturn(jevResult(String.valueOf(l2Id), l2Conf));
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l3"),
        anyString(), anyMap(), anyInt())).thenReturn(jevResult(String.valueOf(nodeId), nodeConf));
  }

  private void runBatch(KnowledgeFixClassifyService.JevChannel jev,
      KnowledgeFixClassifyService.PruneConfig prune, PendingFixRow... rows) {
    List<PendingFixRow> rowList = List.of(rows);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rowList.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro, jev, prune,
        null, null);
  }

  private KnowledgeFixClassifyService.JevChannel jevChannel() {
    return new KnowledgeFixClassifyService.JevChannel(cfg("knowledge-fix-jev", "bocha-jev-v1"),
        new java.math.BigDecimal("0.5"), new java.math.BigDecimal("0.5"), 5, 30);
  }

  private int nz(Integer v) {
    return v == null ? 0 : v;
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<Map<String, String>> mapCaptor() {
    return ArgumentCaptor.forClass((Class) Map.class);
  }

  // ───────── KP-09~22 ─────────

  /** KP-09：JEV stage2 criteria 为剪枝后集合（10 兄弟 → 8，含 601、不含 609/610） */
  @Test
  void kp09_jevStage2CriteriaPruned() {
    stubCommon();
    stubJev(501, 0.9, 601, 0.9);
    ArgumentCaptor<Map<String, String>> l3 = mapCaptor();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l3"),
        anyString(), l3.capture(), anyInt())).thenReturn(jevResult("601", 0.9));
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
        anyString(), anyMap(), anyInt())).thenReturn(jevResult("501", 0.9));
    runBatch(jevChannel(), new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 4192, "蒸发器跑碱如何处理"));
    Map<String, String> criteria = l3.getValue();
    assertEquals(8, criteria.size());
    assertTrue(criteria.containsKey("601"));
    assertFalse(criteria.containsKey("609"));
    assertFalse(criteria.containsKey("610"));
    assertEquals("apply", statusByRowId.get(1L));
  }

  /** KP-10：LLM 兜底与 JEV 同一剪枝口径——θ₂ 拦截降级后，LLM stage2 prompt 只含剪后候选 */
  @Test
  void kp10_llmFallbackUsesSamePrunedSet() {
    stubCommon();
    stubJev(501, 0.9, 601, 0.3);
    stubLlmStage1(501);
    stubLlmStage2(601, 0.9);
    ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        prompt.capture(), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          String prm = inv.getArgument(4);
          if (prm.contains("候选分类（L2）")) {
            return new LlmChatCompletionResult("{\"l2_id\":501}", 10, 5, 15, 0);
          }
          return new LlmChatCompletionResult(
              "{\"node_id\":601,\"confidence\":0.9,\"reason\":\"llm 理由\"}", 10, 5, 15, 0);
        });
    runBatch(jevChannel(), new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 4192, "蒸发器跑碱如何处理"));
    List<String> s2Prompts = prompt.getAllValues().stream()
        .filter(v -> v.contains("候选鉴定点（L3）")).toList();
    assertTrue(s2Prompts.size() >= 3);
    for (String p2 : s2Prompts) {
      assertTrue(p2.contains("601|"));
      assertFalse(p2.contains("609|"));
      assertFalse(p2.contains("610|"));
    }
    assertEquals("apply", statusByRowId.get(1L));
    assertEquals("llm", basisByRowId.get(1L));
  }

  /** KP-11：通道关闭（当前生产形态）时剪枝仍作用于 LLM 路径；stage1 恒全量（KP-18 的 LLM 侧） */
  @Test
  void kp11_pruneAppliesToLlmPathWhenChannelDisabled() {
    stubCommon();
    stubLlmStage1(501);
    stubLlmStage2(601, 0.9);
    ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        prompt.capture(), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          String prm = inv.getArgument(4);
          if (prm.contains("候选分类（L2）")) {
            return new LlmChatCompletionResult("{\"l2_id\":501}", 10, 5, 15, 0);
          }
          return new LlmChatCompletionResult(
              "{\"node_id\":601,\"confidence\":0.9,\"reason\":\"llm 理由\"}", 10, 5, 15, 0);
        });
    runBatch(null, new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 4192, "蒸发器跑碱如何处理"));
    verify(jevClient, never()).decide(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyMap(), anyInt());
    // KP-18：LLM stage1 恒全量——8 个 L2 全在 prompt 中
    String s1Prompt = prompt.getAllValues().stream()
        .filter(v -> v.contains("候选分类（L2）")).findFirst().orElse("");
    assertTrue(s1Prompt.contains("501|"));
    assertTrue(s1Prompt.contains("508|"));
    // stage2 剪枝生效
    String s2Prompt = prompt.getAllValues().stream()
        .filter(v -> v.contains("候选鉴定点（L3）")).findFirst().orElse("");
    assertTrue(s2Prompt.contains("601|"));
    assertFalse(s2Prompt.contains("609|"));
    assertEquals("apply", statusByRowId.get(1L));
  }

  /** KP-12：开关全关 = 既有路径（l2 criteria 8 全量、l3 criteria 10 全量，回退即原路径） */
  @Test
  void kp12_switchesOffKeepsOriginalPath() {
    stubCommon();
    stubJev(501, 0.9, 601, 0.9);
    ArgumentCaptor<Map<String, String>> l2 = mapCaptor();
    ArgumentCaptor<Map<String, String>> l3 = mapCaptor();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
        anyString(), l2.capture(), anyInt())).thenReturn(jevResult("501", 0.9));
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l3"),
        anyString(), l3.capture(), anyInt())).thenReturn(jevResult("601", 0.9));
    runBatch(jevChannel(), KnowledgeFixClassifyService.PruneConfig.OFF,
        row(1L, 4192, "蒸发器跑碱如何处理"));
    assertEquals(8, l2.getValue().size());
    assertEquals(10, l3.getValue().size());
    assertEquals("apply", statusByRowId.get(1L));
  }

  /** KP-13：卫兵语义在剪枝下不变——JEV 选中零重合候选（608 二次蒸汽利用，弱信号补位进名单）→ flag */
  @Test
  void kp13_guardUnchangedUnderPruning() {
    stubCommon();
    stubJev(501, 0.9, 608, 0.9);
    runBatch(jevChannel(), new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 4192, "蒸发器跑碱如何处理"));
    assertEquals("flag", statusByRowId.get(1L));
    assertTrue(basisByRowId.get(1L).equals("jev"));
  }

  /** KP-14：keep 保位端到端——老节点 610（零分）被保位进 stage2 名单，JEV 选中它 → keep（basis=jev） */
  @Test
  void kp14_keepReachedThroughReservedSlot() {
    stubCommon();
    stubJev(501, 0.9, 610, 0.9);
    runBatch(jevChannel(), new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 610, "蒸发器跑碱如何处理"));
    assertEquals("keep", statusByRowId.get(1L));
    assertEquals("jev", basisByRowId.get(1L));
  }

  /** KP-15：范围提交 + 剪枝开启——scope 过滤不受剪枝影响（他课行保持 pending） */
  @Test
  void kp15_scopeSubmitCompatibleWithPruning() {
    stubCommon();
    stubJev(501, 0.9, 601, 0.9);
    PendingFixRow inScope = row(1L, 4192, "蒸发器跑碱如何处理");
    PendingFixRow other = row(2L, 4192, "焙烧温度如何控制");
    other.setCourseId(291);
    List<PendingFixRow> rowList = List.of(inScope, other);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rowList.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro, jevChannel(),
        new KnowledgeFixClassifyService.PruneConfig(true, true), 290, null);
    assertEquals("apply", statusByRowId.get(1L));
    assertEquals("pending", statusByRowId.get(2L));
  }

  /** KP-16/KP-22：审计落库剪枝归因三值——stage1（5/回退0/保位1）、stage2（8/回退0/保位0） */
  @Test
  void kp16_kp22_auditAttributionRecorded() {
    stubCommon();
    stubJev(501, 0.9, 601, 0.9);
    runBatch(jevChannel(), new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 4192, "蒸发器跑碱如何处理"));
    ArgumentCaptor<ExamKnowledgeFixLlmLogEntity> logs =
        ArgumentCaptor.forClass(ExamKnowledgeFixLlmLogEntity.class);
    verify(llmLogMapper, org.mockito.Mockito.atLeastOnce()).insert(logs.capture());
    Map<String, ExamKnowledgeFixLlmLogEntity> byStage = new LinkedHashMap<>();
    for (ExamKnowledgeFixLlmLogEntity l : logs.getAllValues()) {
      byStage.put(l.getStage(), l);
    }
    ExamKnowledgeFixLlmLogEntity s1 = byStage.get("jev-stage1");
    ExamKnowledgeFixLlmLogEntity s2 = byStage.get("jev-stage2");
    assertNotNull(s1);
    assertNotNull(s2);
    assertEquals(5, s1.getPrunedCandidates());
    assertEquals(0, s1.getPruneWeakFallback());
    assertEquals(1, s1.getPruneReservedOld());
    assertEquals(8, s2.getPrunedCandidates());
    assertEquals(0, s2.getPruneWeakFallback());
    assertEquals(0, s2.getPruneReservedOld());
  }

  /** KP-18：stage1 宽剪仅 JEV 通道——LLM stage1 恒全量（通道关闭 + stage1 开也不剪） */
  @Test
  void kp18_stage1WidePruneJevOnly() {
    stubCommon();
    stubLlmStage1(501);
    stubLlmStage2(601, 0.9);
    ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        prompt.capture(), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          String prm = inv.getArgument(4);
          if (prm.contains("候选分类（L2）")) {
            return new LlmChatCompletionResult("{\"l2_id\":501}", 10, 5, 15, 0);
          }
          return new LlmChatCompletionResult(
              "{\"node_id\":601,\"confidence\":0.9,\"reason\":\"llm 理由\"}", 10, 5, 15, 0);
        });
    runBatch(null, new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 4192, "蒸发器跑碱如何处理"));
    String s1Prompt = prompt.getAllValues().stream()
        .filter(v -> v.contains("候选分类（L2）")).findFirst().orElse("");
    for (int id = 501; id <= 508; id++) {
      assertTrue(s1Prompt.contains(id + "|"), "L2 " + id + " 应在全量 prompt 中");
    }
  }

  /** KP-19：开关独立——四种组合下 stage1/stage2 候选数各自符合预期 */
  @Test
  void kp19_switchMatrix() {
    stubCommon();
    int[][] expectations = {{5, 8}, {5, 10}, {8, 8}, {8, 10}};
    boolean[][] combos = {{true, true}, {true, false}, {false, true}, {false, false}};
    for (int i = 0; i < combos.length; i++) {
      // 每个组合独立会话态
      statusByRowId.clear();
      basisByRowId.clear();
      statusByRowId.put(1L, "pending");
      batch.setDone(0);
      org.mockito.Mockito.clearInvocations(jevClient, llmClient, llmLogMapper);
      ArgumentCaptor<Map<String, String>> l2 = mapCaptor();
      ArgumentCaptor<Map<String, String>> l3 = mapCaptor();
      when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
          anyString(), l2.capture(), anyInt())).thenReturn(jevResult("501", 0.9));
      when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l3"),
          anyString(), l3.capture(), anyInt())).thenReturn(jevResult("601", 0.9));
      runBatch(jevChannel(),
          new KnowledgeFixClassifyService.PruneConfig(combos[i][0], combos[i][1]),
          row(1L, 4192, "蒸发器跑碱如何处理"));
      assertEquals(expectations[i][0], l2.getValue().size(),
          "combo" + i + " stage1 candidates");
      assertEquals(expectations[i][1], l3.getValue().size(),
          "combo" + i + " stage2 candidates");
    }
  }

  /** KP-20：stage1 keep 保位语义——老 L2 不在证据 top-5 时替换末位（恒 5），已在则不占额外槽位 */
  @Test
  void kp20_stage1KeepReservationSemantics() {
    stubCommon();
    stubJev(501, 0.9, 601, 0.9);
    // 老 L2 = 506（老节点 4192 挂 506）不在证据 top-5 → 保位替换末位
    ArgumentCaptor<Map<String, String>> l2 = mapCaptor();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
        anyString(), l2.capture(), anyInt())).thenReturn(jevResult("501", 0.9));
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l3"),
        anyString(), anyMap(), anyInt())).thenReturn(jevResult("601", 0.9));
    runBatch(jevChannel(), new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 4192, "蒸发器跑碱如何处理"));
    assertEquals(5, l2.getValue().size());
    assertTrue(l2.getValue().containsKey("501"));
    assertTrue(l2.getValue().containsKey("506"));
    // 老 L2 已在证据 top-5（老节点 601 挂 501）→ 无保位标志，仍恒 5
    runBatch(jevChannel(), new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(2L, 601, "蒸发器跑碱如何处理"));
    ArgumentCaptor<ExamKnowledgeFixLlmLogEntity> logs =
        ArgumentCaptor.forClass(ExamKnowledgeFixLlmLogEntity.class);
    verify(llmLogMapper, org.mockito.Mockito.atLeastOnce()).insert(logs.capture());
    boolean sawReserved1 = false;
    boolean sawReserved0With5 = false;
    for (ExamKnowledgeFixLlmLogEntity l : logs.getAllValues()) {
      if (l.getStage() != null && l.getStage().equals("jev-stage1")) {
        if (l.getPruneReservedOld() != null && l.getPruneReservedOld() == 1) {
          sawReserved1 = true;
        }
        if (l.getPrunedCandidates() != null && l.getPrunedCandidates() == 5
            && l.getPruneReservedOld() != null && l.getPruneReservedOld() == 0) {
          sawReserved0With5 = true;
        }
      }
    }
    assertTrue(sawReserved1);
    assertTrue(sawReserved0With5);
  }

  /** KP-21：stage1 下界——单 L2 课程剪后 <2 → 不发 Jev，降级 LLM；弱信号回退全量且可识别 */
  @Test
  void kp21_stage1BelowBoundsFallsBack() {
    stubCommon();
    // 课程 291 仅 1 个 L2：宽剪后 <2 → 不发 Jev（min_length=2 会 422）→ LLM 全流程
    List<KnowledgeNodeRow> nodes = new ArrayList<>();
    nodes.add(node(900, 291, 0, "基本要求", 1));
    nodes.add(node(920, 291, 900, "熔铸工艺", 2));
    nodes.add(node(921, 291, 920, "熔体静置处理", 3));
    when(queryMapper.selectAllNodes()).thenReturn(nodes);
    stubLlmStage1(920);
    stubLlmStage2(921, 0.9);
    PendingFixRow r = new PendingFixRow();
    r.setRowId(3L);
    r.setDetailId(3);
    r.setOldKnowledgeId(921);
    r.setCourseId(291);
    r.setContent("<p>熔体静置时间如何控制</p>");
    statusByRowId.put(3L, "pending");
    List<PendingFixRow> rowList = List.of(r);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rowList.stream()
        .filter(x -> "pending".equals(statusByRowId.get(x.getRowId()))).toList());
    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro, jevChannel(),
        new KnowledgeFixClassifyService.PruneConfig(true, true), null, null);
    verify(jevClient, never()).decide(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyMap(), anyInt());
    assertEquals("keep", statusByRowId.get(3L));
  }

  /** KP-21 补：弱信号回退全量且审计可识别（stage2 口径：题干与全部兄弟零重合 → 全量 + weak=1） */
  @Test
  void kp21_weakSignalFallbackIdentifiable() {
    stubCommon();
    stubJev(501, 0.9, 601, 0.9);
    // 老节点 610（零重合）+ 弱信号题干：候选全零分 → 回退全量（10）且 weak=1
    PendingFixRow weak = new PendingFixRow();
    weak.setRowId(4L);
    weak.setDetailId(4);
    weak.setOldKnowledgeId(501);
    weak.setCourseId(290);
    weak.setContent("<p>zzz qqq xxx vvv</p>");
    statusByRowId.put(4L, "pending");
    // 弱信号下 stage1 也回退全量：l2 criteria = 8
    ArgumentCaptor<Map<String, String>> l2 = mapCaptor();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
        anyString(), l2.capture(), anyInt())).thenReturn(jevResult("501", 0.9));
    ArgumentCaptor<Map<String, String>> l3 = mapCaptor();
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l3"),
        anyString(), l3.capture(), anyInt())).thenReturn(jevResult("601", 0.9));
    List<PendingFixRow> rowList = List.of(weak);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rowList.stream()
        .filter(x -> "pending".equals(statusByRowId.get(x.getRowId()))).toList());
    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro, jevChannel(),
        new KnowledgeFixClassifyService.PruneConfig(true, true), null, null);
    assertEquals(8, l2.getValue().size());
    assertEquals(10, l3.getValue().size());
    ArgumentCaptor<ExamKnowledgeFixLlmLogEntity> logs =
        ArgumentCaptor.forClass(ExamKnowledgeFixLlmLogEntity.class);
    verify(llmLogMapper, org.mockito.Mockito.atLeastOnce()).insert(logs.capture());
    boolean weakSeen = false;
    for (ExamKnowledgeFixLlmLogEntity l : logs.getAllValues()) {
      if ("jev-stage2".equals(l.getStage())) {
        assertEquals(10, l.getPrunedCandidates());
        if (l.getPruneWeakFallback() != null && l.getPruneWeakFallback() == 1) {
          weakSeen = true;
        }
      }
    }
    assertTrue(weakSeen);
  }

  /** KP-17 的 LLM 路径补强：LLM stage1 审计行不携带剪枝归因（恒全量，pruned_candidates 为 NULL） */
  @Test
  void kp22_llmStage1AuditHasNoPruneAttribution() {
    stubCommon();
    stubLlmStage1(501);
    stubLlmStage2(601, 0.9);
    runBatch(null, new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 4192, "蒸发器跑碱如何处理"));
    ArgumentCaptor<ExamKnowledgeFixLlmLogEntity> logs =
        ArgumentCaptor.forClass(ExamKnowledgeFixLlmLogEntity.class);
    verify(llmLogMapper, org.mockito.Mockito.atLeastOnce()).insert(logs.capture());
    ExamKnowledgeFixLlmLogEntity s1 = null;
    ExamKnowledgeFixLlmLogEntity s2 = null;
    for (ExamKnowledgeFixLlmLogEntity l : logs.getAllValues()) {
      if ("stage1".equals(l.getStage())) {
        s1 = l;
      }
      if ("stage2".equals(l.getStage())) {
        s2 = l;
      }
    }
    assertNotNull(s1);
    assertNotNull(s2);
    assertNull(s1.getPrunedCandidates());
    assertNull(s1.getPruneWeakFallback());
    assertEquals(8, s2.getPrunedCandidates());
  }


  /** KP-23（v1.4 补，2026-10-04 生产缺陷修复）：JEV 422=token_budget_exceeded → 降级 LLM 分类成功（不再 SKIP_ROW 卡死） */
  @Test
  void kp23_budgetExceeded422FallsBackToLlm() {
    stubCommon();
    stubLlmStage1(501);
    stubLlmStage2(601, 0.9);
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
        anyString(), anyMap(), anyInt()))
        .thenThrow(new JevValidationException("Jev 422 请求校验失败: "
            + "{\"detail\":{\"code\":\"token_budget_exceeded\",\"counted_tokens\":33480,\"limit_tokens\":32768}}"));
    runBatch(jevChannel(), new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 4192, "蒸发器跑碱如何处理"));
    verify(jevClient, times(1)).decide(anyString(), anyString(), anyString(), anyString(),
        eq("l2"), anyString(), anyMap(), anyInt());
    assertEquals("apply", statusByRowId.get(1L));
    assertEquals("llm", basisByRowId.get(1L));
  }

  /** KP-23 补：非预算类 422（真构造 bug）保持 SKIP_ROW 既有语义——行不进 LLM、计失败 */
  @Test
  void kp23_nonBudget422StillSkipsRow() {
    stubCommon();
    stubLlmStage1(501);
    stubLlmStage2(601, 0.9);
    when(jevClient.decide(anyString(), anyString(), anyString(), anyString(), eq("l2"),
        anyString(), anyMap(), anyInt()))
        .thenThrow(new JevValidationException("Jev 422 请求校验失败: "
            + "{\"detail\":{\"msg\":\"Dictionary should have at least 2 items\"}}"));
    runBatch(jevChannel(), new KnowledgeFixClassifyService.PruneConfig(true, true),
        row(1L, 4192, "蒸发器跑碱如何处理"));
    // SKIP_ROW 行保持 pending，3 轮派发各试一次（MAX_DISPATCH_ROUNDS=3，既有语义）
    verify(jevClient, times(3)).decide(anyString(), anyString(), anyString(), anyString(),
        eq("l2"), anyString(), anyMap(), anyInt());
    verify(llmClient, never()).chatCompletionDetailed(anyString(), anyString(), anyString(),
        anyString(), contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt());
    // 批次未达门禁收尾：未分类行落 flag（与生产「批次中断未分类」一致）
    assertEquals("flag", statusByRowId.get(1L));
  }
}
