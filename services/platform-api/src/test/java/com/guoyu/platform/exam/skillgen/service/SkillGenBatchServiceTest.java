package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintNodeMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenBatchMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenDraftMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** 批次提交单测：发起预检（S9/S10）、写库先提交后投递、拒绝路径、GLM 配置校验 */
class SkillGenBatchServiceTest {

  private static final Long TENANT = 1L;
  private static final Long USER = 9L;
  private static final Long BP_ID = 100L;
  private static final Long CFG_ID = 200L;

  private SkillBlueprintMapper blueprintMapper;
  private SkillBlueprintNodeMapper nodeMapper;
  private SkillGenBatchMapper batchMapper;
  private SkillGenDraftMapper draftMapper;
  private ExamFaqModelConfigMapper modelConfigMapper;
  private SkillGenAiService aiService;
  private SkillBlueprintApplicationService blueprintService;
  private TransactionTemplate transactionTemplate;
  private ThreadPoolTaskExecutor orchestrator;
  private SkillGenBatchService service;

  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, SkillGenDraftEntity.class);
    TableInfoHelper.initTableInfo(assistant, SkillBlueprintNodeEntity.class);
    TableInfoHelper.initTableInfo(assistant, SkillGenBatchEntity.class);
  }

  @BeforeEach
  void setUp() {
    blueprintMapper = mock(SkillBlueprintMapper.class);
    nodeMapper = mock(SkillBlueprintNodeMapper.class);
    batchMapper = mock(SkillGenBatchMapper.class);
    draftMapper = mock(SkillGenDraftMapper.class);
    modelConfigMapper = mock(ExamFaqModelConfigMapper.class);
    aiService = mock(SkillGenAiService.class);
    blueprintService = mock(SkillBlueprintApplicationService.class);
    transactionTemplate = mock(TransactionTemplate.class);
    org.mockito.Mockito.doAnswer(inv -> {
      Consumer<TransactionStatus> consumer = inv.getArgument(0);
      consumer.accept(mock(TransactionStatus.class));
      return null;
    }).when(transactionTemplate).executeWithoutResult(any());
    orchestrator = mock(ThreadPoolTaskExecutor.class);
    ThreadPoolTaskExecutor blockExecutor = mock(ThreadPoolTaskExecutor.class);
    service = new SkillGenBatchService(blueprintMapper, nodeMapper, batchMapper, draftMapper,
        modelConfigMapper, aiService, new SkillQuestionValidator(), blueprintService,
        new ObjectMapper(), orchestrator, blockExecutor, transactionTemplate, 5, 10000L);
  }

  private SkillBlueprintEntity blueprint() {
    SkillBlueprintEntity bp = new SkillBlueprintEntity();
    bp.setId(BP_ID);
    bp.setTenantId(TENANT);
    bp.setWorkTypeId(22);
    bp.setLevel(2);
    return bp;
  }

  private ExamFaqModelConfigEntity modelConfig() {
    ExamFaqModelConfigEntity cfg = new ExamFaqModelConfigEntity();
    cfg.setId(CFG_ID);
    cfg.setModelId("glm-5.3-flash");
    cfg.setModelCode("skill-gen-glm");
    cfg.setBaseUrl("https://open.bigmodel.cn/api/paas/v4");
    cfg.setApiKey("sk-test");
    cfg.setThinking("enabled");
    cfg.setReasoningEffort("high");
    cfg.setMaxTokens(16384);
    cfg.setStatus("enabled");
    return cfg;
  }

  private List<SkillBlueprintNodeEntity> points(int count) {
    List<SkillBlueprintNodeEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      SkillBlueprintNodeEntity n = new SkillBlueprintNodeEntity();
      n.setId(10L + i);
      n.setTenantId(TENANT);
      n.setBlueprintId(BP_ID);
      n.setNodeLevel(3);
      n.setFullCode("A-A-" + String.format("%03d", i + 1));
      n.setName("考评点" + (i + 1));
      list.add(n);
    }
    return list;
  }

  /** 回归：pause→快速 resume 竞态——旧编排器在飞块（未落草稿）已登记的点不得再派发 */
  @Test
  void gen3_pendingNodesSkipsInFlightDispatchedNodes() {
    List<SkillBlueprintNodeEntity> points = points(3);
    java.util.Set<Long> done = new java.util.HashSet<>();          // DB 尚无草稿
    java.util.Set<Long> dispatched = new java.util.HashSet<>();
    dispatched.add(points.get(1).getId());                          // 旧编排器在飞
    List<SkillBlueprintNodeEntity> pending = SkillGenBatchService.pendingNodes(points, done, dispatched);
    assertEquals(2, pending.size(), "在飞登记点必须跳过，否则重复生成（实测 27/23 竞态）");
    assertTrue(pending.stream().noneMatch(n -> n.getId().equals(points.get(1).getId())));
    // 已有草稿点同样跳过（断点续跑）
    done.add(points.get(0).getId());
    assertEquals(1, SkillGenBatchService.pendingNodes(points, done, dispatched).size());
  }

  /** 回归：库内防重——该点已有草稿时 saveDrafts 不落库、不计数 */
  @Test
  void gen3_saveDraftsSkipsWhenNodeAlreadyHasDrafts() {
    SkillGenBatchEntity batch = new SkillGenBatchEntity();
    batch.setId(300L);
    batch.setTenantId(TENANT);
    batch.setBlueprintId(BP_ID);
    batch.setCreatedBy(USER);
    ExamFaqModelConfigEntity cfg = modelConfig();
    SkillBlueprintNodeEntity point = points(1).get(0);
    when(draftMapper.selectCount(any())).thenReturn(5L);   // 已有草稿
    List<com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem> questions = new ArrayList<>();
    boolean saved = service.saveDrafts(batch, cfg, point, null, questions, 100);
    assertEquals(false, saved, "已有草稿必须跳过落库");
    verify(draftMapper, never()).insert(any(SkillGenDraftEntity.class));
  }

  @Test
  void submit_precheckBlocksWhenPlannedPlusCumulativeBelowFloor() {
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(CFG_ID)).thenReturn(modelConfig());
    when(nodeMapper.selectList(any())).thenReturn(points(4)); // 4×5=20
    when(blueprintService.cumulativeStat(TENANT, 22, 2))
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(5, 5)); // 20+5=25 < 30

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID));
    assertTrue(ex.getMessage().contains("低于本等级题库总量下限"));
    assertTrue(ex.getMessage().contains("还差"));
    verify(batchMapper, never()).insert(any(SkillGenBatchEntity.class));
  }

  @Test
  void submit_successInsertsBatchThenDispatches() {
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(CFG_ID)).thenReturn(modelConfig());
    when(nodeMapper.selectList(any())).thenReturn(points(10)); // 10×5=50 ≥ 30
    when(blueprintService.cumulativeStat(TENANT, 22, 2))
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(0, 0));

    var vo = service.submit(TENANT, USER, BP_ID, CFG_ID);

    assertEquals(SkillGenBatchEntity.STATUS_PENDING, vo.status());
    assertEquals(10, vo.totalPoints());
    ArgumentCaptor<SkillGenBatchEntity> cap = ArgumentCaptor.forClass(SkillGenBatchEntity.class);
    verify(batchMapper).insert(cap.capture());
    assertEquals("{\"perPoint\":5}", cap.getValue().getQuotaConfig());
    assertTrue(cap.getValue().getPromptDigest() != null && !cap.getValue().getPromptDigest().isBlank());
    // 写库先提交（TransactionTemplate）后投递编排线程
    verify(transactionTemplate).executeWithoutResult(any());
    verify(orchestrator).submit(any(Runnable.class));
  }

  @Test
  void submit_rejectedExecutorMarksBatchFailed() {
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(CFG_ID)).thenReturn(modelConfig());
    when(nodeMapper.selectList(any())).thenReturn(points(10));
    when(blueprintService.cumulativeStat(TENANT, 22, 2))
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(0, 0));
    org.mockito.Mockito.doThrow(new RejectedExecutionException("full"))
        .when(orchestrator).submit(any(Runnable.class));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID));
    assertTrue(ex.getMessage().contains("服务繁忙"));
    verify(batchMapper).insert(any(SkillGenBatchEntity.class));
    ArgumentCaptor<SkillGenBatchEntity> updated = ArgumentCaptor.forClass(SkillGenBatchEntity.class);
    verify(batchMapper).updateById(updated.capture());
    assertEquals(SkillGenBatchEntity.STATUS_FAILED, updated.getValue().getStatus());
    assertTrue(updated.getValue().getFailReason().contains("服务繁忙"));
  }

  @Test
  void submit_glmDisabledThinkingRejected() {
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    ExamFaqModelConfigEntity cfg = modelConfig();
    cfg.setThinking("disabled");
    when(modelConfigMapper.selectById(CFG_ID)).thenReturn(cfg);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID));
    assertTrue(ex.getMessage().contains("不可关闭思考"));
  }

  @Test
  void submit_rejectsApiKeyPlaceholder() {
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    ExamFaqModelConfigEntity cfg = modelConfig();
    cfg.setApiKey("PENDING_USER_INPUT");
    when(modelConfigMapper.selectById(CFG_ID)).thenReturn(cfg);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID));
    assertTrue(ex.getMessage().contains("api_key 未配置"));
  }
}
