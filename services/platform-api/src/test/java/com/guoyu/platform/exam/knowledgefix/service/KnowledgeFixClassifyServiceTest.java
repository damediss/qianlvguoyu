package com.guoyu.platform.exam.knowledgefix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.infrastructure.JevDecisionClient;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.knowledgefix.dto.KnowledgeNodeRow;
import com.guoyu.platform.exam.knowledgefix.dto.PendingFixRow;
import com.guoyu.platform.exam.knowledgefix.entity.ExamDetailKnowledgeFixEntity;
import com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixBatchEntity;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailKnowledgeFixMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamExamDetailFixMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamKnowledgeFixBatchMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamKnowledgeFixLlmLogMapper;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 知识修正分类编排单测（KF-09~19）。LLM 全 mock 桩响应；自一致/升级/卫兵/补跑真实走线程池。
 * 状态语义模拟：updateClassifyResult 修改内存 status 表，selectPendingWithContent 按 status 过滤
 * （与生产"分类后不再 pending"语义同构）。
 */
class KnowledgeFixClassifyServiceTest {

  private static final Long TENANT = 10159L;
  private static final Long USER = 9L;
  private static final Long BATCH_ID = 500L;
  private static final Long BATCH_NO = 501L;

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
  private final Map<Long, String> statusByRowId = new HashMap<>();
  private final List<Integer> stage2CallCounts = new ArrayList<>();

  @BeforeEach
  void setUp() {
    batchMapper = mock(ExamKnowledgeFixBatchMapper.class);
    fixMapper = mock(ExamDetailKnowledgeFixMapper.class);
    queryMapper = mock(ExamExamDetailFixMapper.class);
    modelConfigMapper = mock(ExamFaqModelConfigMapper.class);
    llmClient = mock(FaqLlmClient.class);
    llmLogMapper = mock(ExamKnowledgeFixLlmLogMapper.class);
    statusByRowId.clear();
    stage2CallCounts.clear();

    ThreadPoolTaskExecutor orchestrator = new ThreadPoolTaskExecutor();
    orchestrator.setCorePoolSize(1);
    orchestrator.setMaxPoolSize(1);
    orchestrator.setQueueCapacity(10);
    orchestrator.initialize();

    llmExecutor = new ThreadPoolTaskExecutor();
    llmExecutor.setCorePoolSize(3);
    llmExecutor.setMaxPoolSize(3);
    llmExecutor.setQueueCapacity(10);
    llmExecutor.initialize();

    cfgFlash = cfg("theory-gen-glm", "glm-5.3-flash");
    cfgPro = cfg("knowledge-fix-pro", "glm-5.3");
    batch = batchEntity();
    jevClient = mock(JevDecisionClient.class);

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
    c.setBaseUrl("https://open.bigmodel.cn/api/paas/v4");
    c.setApiKey("test-key");
    c.setStatus("enabled");
    c.setThinking("enabled");
    c.setReasoningEffort("low");
    c.setMaxTokens(1024);
    return c;
  }

  private ExamKnowledgeFixBatchEntity batchEntity() {
    ExamKnowledgeFixBatchEntity b = new ExamKnowledgeFixBatchEntity();
    b.setId(BATCH_ID);
    b.setTenantId(TENANT);
    b.setBatchNo(BATCH_NO);
    b.setTotal(1); // 与用例行数一致；多行用例自行 setTotal（门禁按 total 计算未分类率）
    b.setStatus(ExamKnowledgeFixBatchEntity.STATUS_PENDING);
    b.setCreatedBy(USER);
    return b;
  }

  private KnowledgeNodeRow node(int id, int courseId, int parentId, String title, int level, String important) {
    KnowledgeNodeRow n = new KnowledgeNodeRow();
    n.setId(id);
    n.setCourseId(courseId);
    n.setParentId(parentId);
    n.setTitle(title);
    n.setLevel(level);
    n.setImportant(important);
    return n;
  }

  private PendingFixRow row(long rowId, int detailId, int oldKnowledgeId, int courseId, String stem) {
    PendingFixRow r = new PendingFixRow();
    r.setRowId(rowId);
    r.setDetailId(detailId);
    r.setOldKnowledgeId(oldKnowledgeId);
    r.setCourseId(courseId);
    r.setContent("<p>" + stem + "</p>");
    r.setAnswer("[{\"content\":\"A\",\"isCorrect\":true,\"score\":1}]");
    r.setResolve(null);
    statusByRowId.put(rowId, "pending");
    return r;
  }

  /** 课程 190 树：L2=201 蒸发系统（L3: 301 蒸发器跑碱处理 / 302 蒸发站真空调整）；L2=202 焙烧系统（L3: 303 焙烧温度控制）；
   *  L2=203 安全规程（秃章节，无 L3——业务裁定"题可挂 L2"直收尾用）；老节点 4192 工匠精神培养 */
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

  /** L2/L3 分层桩：L1 阶段选 201，L2 阶段按脚本返回 */
  private void stubLlm(MovieStyle script) {
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> LlmChatCompletionResult.from(null, "{\"l2_id\":201}"));
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> {
          int idx = stage2CallCounts.size();
          stage2CallCounts.add(idx);
          return script.answer(idx, inv.getArgument(4));
        });
  }

  /** 脚本接口：第 n 次 L3 调用返回的内容 */
  private interface MovieStyle {
    LlmChatCompletionResult answer(int callIndex, String userPrompt);
  }

  private LlmChatCompletionResult result(String json) {
    return new LlmChatCompletionResult(json, 10, 5, 15, 0);
  }

  private void stubCommonHappyPath() {
    when(batchMapper.selectById(BATCH_ID)).thenAnswer(inv -> batch);
    when(batchMapper.updateById(ArgumentMatchers.<com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixBatchEntity>any())).thenReturn(1);
    when(queryMapper.selectAllNodes()).thenReturn(courseTree());
    when(queryMapper.selectMayRowCount()).thenReturn(3L);
    when(fixMapper.updateClassifyResult(anyLong(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
      statusByRowId.put(inv.getArgument(0), inv.getArgument(5));
      return 1;
    });
    // 注：Worker 多线程并发调用，计数推进需互斥（生产侧由 SQL 的 x = x + 1 原子完成）
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
    // FIX-1.3：未分类行只累加 flag_cnt、不计 done
    when(batchMapper.incrementUnclassified(anyLong(), anyInt())).thenAnswer(inv -> {
      synchronized (batch) {
        batch.setFlagCnt(nz(batch.getFlagCnt()) + inv.<Integer>getArgument(1));
      }
      return 1;
    });
  }

  // ───────── KF-09~19 ─────────

  /** KF-09：node_id 越界（非本课程树）→ 行保持 pending，3 轮后入 flag（自动补跑后仍失败） */
  @Test
  void kf09_outOfTreeNodeIdEndsFlagged() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> idx == 0
        ? result("{\"l2_id\":201}")
        : result("{\"node_id\":99999,\"confidence\":0.9,\"reason\":\"x\"}"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals("flag", statusByRowId.get(1L));
    verify(llmLogMapper, atLeast(1)).insert(ArgumentMatchers.<com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity>any());
  }

  /** KF-10：模型选中当前节点（4192 工匠精神培养）→ keep，knowledge 不变 */
  @Test
  void kf10_modelPicksCurrentNodeMeansKeep() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> result("{\"node_id\":4192,\"confidence\":0.9,\"reason\":\"确属素养范畴\"}"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "工匠精神的内涵"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_KEEP, statusByRowId.get(1L));
  }

  /** KF-11：三采样 3/3 一致 → apply，自一致率 1.0，批次 DONE */
  @Test
  void kf11_unanimousVoteApplies() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"蒸发器跑碱对应\"}"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    verify(fixMapper).updateClassifyResult(anyLong(), ArgumentMatchers.eq(301), any(), any(), any(),
        ArgumentMatchers.eq(ExamDetailKnowledgeFixEntity.STATUS_APPLY));
    assertEquals(0, BigDecimalCompare(batch.getSelfConsistencyRate(), 1.0));
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** KF-12：2/3 多数 → apply，置信取多数票均值 */
  @Test
  void kf12_majorityVoteApplies() {
    stubCommonHappyPath();
    AtomicInteger callIdx = new AtomicInteger();
    stubLlm((idx, prompt) -> {
      int i = callIdx.getAndIncrement() % 3;
      return switch (i) {
        case 0, 1 -> result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}");
        default -> result("{\"node_id\":302,\"confidence\":0.8,\"reason\":\"x\"}");
      };
    });
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    verify(fixMapper).updateClassifyResult(anyLong(), ArgumentMatchers.eq(301), any(), any(), any(),
        ArgumentMatchers.eq(ExamDetailKnowledgeFixEntity.STATUS_APPLY));
    // §6 调整：自一致率 0（2/3 多数行非 3/3 一致）降为信息项展示，不再卡门禁 → 批次 DONE
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
    assertEquals(0, batch.getSelfConsistencyRate().compareTo(java.math.BigDecimal.ZERO),
        "唯一分类行非 3/3 一致 → 自一致率 0（仅展示）");
  }

  /** KF-13：三方各不同 → 升级旗舰（glm-5.3）复判，结果生效且 basis=llm-pro */
  @Test
  void kf13_escalatesToProModel() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> {
      if (idx == 0) {
        return result("{\"node_id\":301,\"confidence\":0.6,\"reason\":\"a\"}");
      }
      if (idx == 1) {
        return result("{\"node_id\":302,\"confidence\":0.6,\"reason\":\"b\"}");
      }
      return result("{\"node_id\":303,\"confidence\":0.6,\"reason\":\"c\"}");
    });

    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // 升级调用必须走 pro 模型（glm-5.3）
    verify(llmClient, atLeast(1)).chatCompletionDetailed(anyString(), anyString(),
        ArgumentMatchers.eq("glm-5.3"), anyString(), ArgumentMatchers.contains("候选鉴定点（L3）"),
        anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt());
    // §6 调整：升级复判行不计入自一致（率 0 仅展示）→ 批次 DONE，不再被率门禁拦下
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** KF-14：确定性卫兵——选中节点与题干零重合且非素养节点 → 强制 flag */
  @Test
  void kf14_guardForcesFlagOnUnrelatedPick() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> result("{\"node_id\":303,\"confidence\":0.95,\"reason\":\"焙烧温度控制\"}"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因排查"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_FLAG, statusByRowId.get(1L));
    verify(fixMapper).updateClassifyResult(anyLong(), ArgumentMatchers.eq(303), any(), any(),
        ArgumentMatchers.contains("卫兵拦截"),
        ArgumentMatchers.eq(ExamDetailKnowledgeFixEntity.STATUS_FLAG));
  }

  /** KF-15：模型输出零信任——非法 JSON → 行失败计数、编排不中断、行不写结论；
   *  FIX-1.4：整批自一致率 0 → 批次判 failed（未达标禁止写回） */
  @Test
  void kf15_invalidJsonFailsRowAndGateBlocksBatch() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> result("这不是 JSON"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    verify(batchMapper, atLeast(1)).incrementFailed(BATCH_NO);
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_FAILED, batch.getStatus());
    assertTrue(batch.getFailReason().startsWith("未达质量门禁"), batch.getFailReason());
  }

  /** KF-16：失败自动补跑——首轮失败的行第二轮重试成功 */
  @Test
  void kf16_failedRowRetriedNextRound() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> {
      if (idx == 0) {
        throw new RuntimeException("transient timeout");
      }
      return result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}");
    });
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // 轮 1 失败，轮 2 成功 → apply
    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** KF-17：熔断——连续失败达阈值只停"本轮继续派发"（FIX-1/DEF-2），
   *  剩余行进入下一轮补跑；最终仍未分类的行写 basis=unclassified；批次被门禁拦下 */
  @Test
  void kf17_breakerStopsDispatchOnConsecutiveFailures() {
    stubCommonHappyPath();
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 7; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(7);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    AtomicInteger llmCalls = new AtomicInteger();
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), contains("候选鉴定点"),
        anyString(), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          llmCalls.incrementAndGet();
          throw new RuntimeException("boom");
        });
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), contains("候选分类"),
        anyString(), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          llmCalls.incrementAndGet();
          throw new RuntimeException("boom");
        });

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // 熔断后每轮派发受限（不浪费调用），但整批不被放弃：全部行最终都有结论（unclassified）
    assertTrue(llmCalls.get() <= 10, "熔断后调用数应受限，实际 " + llmCalls.get());
    verify(fixMapper, atLeast(1)).updateClassifyResult(anyLong(), any(), any(), any(),
        ArgumentMatchers.contains("批次中断未分类"), ArgumentMatchers.eq(ExamDetailKnowledgeFixEntity.STATUS_FLAG));
    for (int i = 1; i <= 7; i++) {
      assertEquals(ExamDetailKnowledgeFixEntity.STATUS_FLAG, statusByRowId.get((long) i),
          "第 " + i + " 行应有结论（不再被整批放弃）");
    }
    verify(batchMapper).incrementUnclassified(anyLong(), ArgumentMatchers.anyInt());
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_FAILED, batch.getStatus());
  }

  /** KF-18：多租户——分类线程内租户上下文正确 */
  @Test
  void kf18_tenantContextInWorkerThreads() {
    stubCommonHappyPath();
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    List<Long> seen = new CopyOnWriteArrayList<>();
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          seen.add(TenantContextHolder.getTenantId());
          return result("{\"l2_id\":201}");
        });
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          seen.add(TenantContextHolder.getTenantId());
          return result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}");
        });

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(12, seen.size()); // 3 行 ×（1 stage1 + 3 stage2）= 12 次 LLM 调用，每次均记录租户
    for (Long t : seen) {
      assertEquals(TENANT, t);
    }
  }

  /** KF-19：审计落库——每次 LLM 调用写一行审计（含 token 与耗时） */
  @Test
  void kf19_llmCallsAudited() {
    stubCommonHappyPath();
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> result("{\"l2_id\":201}"));
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          System.out.println("[dbg] LLM STAGE2 tail=" + inv.<String>getArgument(4, String.class).substring(Math.max(0, inv.<String>getArgument(4, String.class).length() - 12)));
          return result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}");
        });

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // stage1 + 3×stage2 = 4 次调用 → 4 条审计
    ArgumentCaptor<com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity> cap = ArgumentCaptor.forClass(com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity.class);
    verify(llmLogMapper, times(4)).insert(cap.capture());
    for (com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity e2 : cap.getAllValues()) {
      System.out.println("[dbg] AUDIT stage=" + e2.getStage() + " model=" + e2.getModelId() + " tokens=" + e2.getTotalTokens());
    }
  }


  // ───────── FIX-1/6 回归（KF-25/26/28） ─────────

  /** KF-25：熔断后仍要补跑——首轮连续失败触发熔断，后续轮次把剩余行全部跑完（DEF-2 回归） */
  @Test
  void kf25_rowsStillRunAfterBreakerTrips() {
    stubCommonHappyPath();
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 7; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(7);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    AtomicInteger stage1Calls = new AtomicInteger();
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> {
          // 前 5 次失败触发熔断；其后恢复（模拟瞬时故障）
          if (stage1Calls.incrementAndGet() <= 5) {
            throw new RuntimeException("transient boom");
          }
          return result("{\"l2_id\":201}");
        });
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}"));

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // 熔断只停本轮派发：7 行最终全部拿到结论（apply），批次不再被整批放弃
    for (int i = 1; i <= 7; i++) {
      assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get((long) i),
          "第 " + i + " 行应在补跑轮完成分类");
    }
    assertEquals(7, nz(batch.getDone()));
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** KF-26：批次质量门禁——未分类占比 >5% 或自一致率 <0.8 → failed（禁止 done/写回） */
  @Test
  void kf26_gateBlocksBatchWhenUnclassifiedRatioTooHigh() {
    stubCommonHappyPath();
    // 3 行中仅 1 行能进模型（其余两行 stage1 直接失败且重试仍是失败）：未分类 2/3 = 67% > 5%
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(3);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    AtomicInteger stage1Calls = new AtomicInteger();
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> {
          // 仅第一次 stage1 成功（第 1 行）；其余行三轮全部失败 → 未分类 2/3
          if (stage1Calls.incrementAndGet() == 1) {
            return result("{\"l2_id\":201}");
          }
          throw new RuntimeException("boom");
        });
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}"));

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_FAILED, batch.getStatus());
    assertTrue(batch.getFailReason().contains("未分类"), batch.getFailReason());
    assertEquals(1, nz(batch.getDone()));
  }

  /** KF-28：卫兵拦截不计入连续失败——大量卫兵拦截不会误触发熔断（DEF-1 回归） */
  @Test
  void kf28_guardInterceptionsDoNotTripBreaker() {
    stubCommonHappyPath();
    int n = 60; // 阈值 5 的 12 倍：若卫兵计入失败，第 6 行起就会被熔断
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= n; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(n);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    // 选中与题干零重合且非素养节点（303 焙烧温度控制）→ 全部被卫兵拦截
    stubLlm((idx, prompt) -> result("{\"node_id\":303,\"confidence\":0.95,\"reason\":\"x\"}"));

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    for (int i = 1; i <= n; i++) {
      assertEquals(ExamDetailKnowledgeFixEntity.STATUS_FLAG, statusByRowId.get((long) i),
          "第 " + i + " 行应经卫兵判定为 flag，而不是被熔断跳过");
    }
    verify(batchMapper, org.mockito.Mockito.never()).incrementUnclassified(anyLong(),
        ArgumentMatchers.anyInt());
    assertTrue(nz(batch.getDone()) >= n, "全部行都应得到结论，done=" + nz(batch.getDone()));
  }

  // ───────── v1.7 回归（KF-32/33） ─────────

  /**
   * KF-32：人工暂停（FIX-10/v1.7）——暂停登记后编排不得派发任何行；批次收尾为 paused；
   * 剩余行保持 pending（可再次提交接着跑）、不写"批次中断未分类"兜底、未分类计数不增、零 LLM 调用。
   * 采用"启动前登记暂停"的确定性时序（与页面按钮同一入口 pause()）；运行中暂停走的同一检查点。
   */
  @Test
  void kf32_pauseStopsDispatchingAndFinalizesPaused() {
    stubCommonHappyPath();
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(5);
    batch.setStatus(ExamKnowledgeFixBatchEntity.STATUS_RUNNING);
    when(batchMapper.selectList(ArgumentMatchers
            .<com.baomidou.mybatisplus.core.conditions.Wrapper<ExamKnowledgeFixBatchEntity>>any()))
        .thenReturn(List.of(batch));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.pause();

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(0, statusByRowId.values().stream().filter(s -> !"pending".equals(s)).count(),
        "暂停后不得派发任何行");
    assertEquals(5, statusByRowId.values().stream().filter(s -> "pending".equals(s)).count(),
        "全部行应保持 pending，可再次提交接着跑");
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_PAUSED, batch.getStatus());
    assertTrue(batch.getFailReason() != null && batch.getFailReason().contains("暂停"),
        "failReason 应说明暂停：" + batch.getFailReason());
    verify(llmClient, org.mockito.Mockito.never()).chatCompletionDetailed(anyString(), anyString(),
        anyString(), anyString(), anyString(), anyInt(), any(), any(), anyBoolean(), anyLong(),
        anyInt());
    // 暂停收尾不写"批次中断未分类"兜底（否则继续跑要先 reset 把 flag 退回）
    verify(fixMapper, org.mockito.Mockito.never()).updateClassifyResult(anyLong(), any(), any(),
        any(), any(), anyString());
    verify(batchMapper, org.mockito.Mockito.never()).incrementUnclassified(anyLong(), anyInt());
  }

  /** KF-33：缓存友好 prompt（FIX-11/v1.7）——同一行 3 次采样 prompt 逐字节一致；
   *  结构上候选清单+输出要求在前、【试题】在后（同课程行共享前缀，可命中输入缓存） */
  @Test
  void kf33_samplesShareIdenticalPromptAndStablePrefixFirst() {
    stubCommonHappyPath();
    List<String> stage2Prompts = new CopyOnWriteArrayList<>();
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> result("{\"l2_id\":201}"));
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> {
          stage2Prompts.add(inv.getArgument(4, String.class));
          return result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}");
        });

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(3, stage2Prompts.size());
    assertEquals(stage2Prompts.get(0), stage2Prompts.get(1), "第 2 次采样应与第 1 次 prompt 一致（缓存命中）");
    assertEquals(stage2Prompts.get(1), stage2Prompts.get(2), "第 3 次采样应与第 1 次 prompt 一致（缓存命中）");
    String p = stage2Prompts.get(0);
    int l3 = p.indexOf("【候选鉴定点（L3）】");
    int rule = p.indexOf("请从候选中选出该试题真正归属的鉴定点");
    int q = p.indexOf("【试题】");
    int marker = p.indexOf("【当前标记】");
    assertTrue(p.startsWith("【候选鉴定点（L3）】"), "候选清单应前置");
    assertTrue(l3 >= 0 && rule > l3 && q > rule && marker > q,
        "结构应为 候选清单 → 输出要求 → 【试题】 → 【当前标记】");
  }

  // ───────── "题可挂 L2"业务裁定（KF-34~36） ─────────

  /**
   * KF-34：模型选中的 L2 下无存活 L3（秃章节）→ 直接以 L2 收尾（apply，固定置信 0.5，basis=l2-direct），
   * 跳过阶段 2（零 L3 采样调用）；整批无三采样行 → 自一致率为 null，门禁率项不适用，批次 DONE
   */
  @Test
  void kf34_childlessL2ConcludesDirectlyAtL2() {
    stubCommonHappyPath();
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> result("{\"l2_id\":203}"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "安全规程相关题"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // 直挂 L2：newKnowledgeId=203、固定置信 0.5、basis=l2-direct（capture 精确断言）
    org.mockito.ArgumentCaptor<java.math.BigDecimal> confCap =
        org.mockito.ArgumentCaptor.forClass(java.math.BigDecimal.class);
    org.mockito.ArgumentCaptor<String> basisCap = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(fixMapper).updateClassifyResult(anyLong(), ArgumentMatchers.eq(203), confCap.capture(),
        basisCap.capture(), any(), ArgumentMatchers.eq(ExamDetailKnowledgeFixEntity.STATUS_APPLY));
    assertEquals(0, confCap.getValue().compareTo(java.math.BigDecimal.valueOf(0.5)));
    assertEquals("l2-direct", basisCap.getValue());
    // 阶段 2 零调用（省 3 次采样）
    verify(llmClient, org.mockito.Mockito.never()).chatCompletionDetailed(anyString(), anyString(),
        anyString(), anyString(), ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(),
        any(), anyBoolean(), anyLong(), anyInt());
    // 无三采样行：自一致率 null（非 0），率项不适用，批次 DONE
    assertEquals(null, batch.getSelfConsistencyRate());
    assertEquals(1, nz(batch.getDone()));
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** KF-35：确定性守卫拒绝（L2 越界）不计失败重试、不计熔断——大量越界行也不会触发熔断 */
  @Test
  void kf35_guardRejectDoesNotCountTowardBreaker() {
    stubCommonHappyPath();
    int n = 12; // 阈值 5 的 2.4 倍：若越界计入失败，第 6 行起就会被熔断
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= n; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    batch.setTotal(n);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    // stage1 恒返回候选外 id → 全部行守卫拒绝
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> result("{\"l2_id\":99999}"));

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // 越界不是服务故障：failed 计数一次都没动，且没有熔断把行跳过
    verify(batchMapper, org.mockito.Mockito.never()).incrementFailed(BATCH_NO);
    // 行保持 pending 重试至轮次耗尽，最终被收尾扫为 unclassified-flag（可 reset 重跑）
    for (int i = 1; i <= n; i++) {
      assertEquals(ExamDetailKnowledgeFixEntity.STATUS_FLAG, statusByRowId.get((long) i),
          "第 " + i + " 行应被收尾标记（不是被熔断跳过）");
    }
  }

  /** KF-36：混合批——L2 直收尾行不稀释自一致率（率只按三采样行计算） */
  @Test
  void kf36_l2DirectRowsExcludedFromSelfConsistencyRate() {
    stubCommonHappyPath();
    List<PendingFixRow> rows = List.of(
        row(1, 1, 4192, 190, "秃章节题"),
        row(2, 2, 4192, 190, "蒸发器跑碱的原因"));
    batch.setTotal(2);
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> {
          String prompt = inv.getArgument(4, String.class);
          return result(prompt.contains("秃章节题") ? "{\"l2_id\":203}" : "{\"l2_id\":201}");
        });
    // 仅第 2 行走阶段 2，三采样 3/3 一致
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}"));

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(2L));
    // 率 = 1/1（仅三采样行）而非 1/2：L2 直收尾行不进分子分母
    assertEquals(0, BigDecimalCompare(batch.getSelfConsistencyRate(), 1.0));
    assertEquals(2, nz(batch.getDone()));
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  private static int nz(Integer v) {
    return v == null ? 0 : v;
  }

  private static int BigDecimalCompare(java.math.BigDecimal actual, double expected) {
    return actual == null ? -1 : actual.compareTo(java.math.BigDecimal.valueOf(expected));
  }
}
