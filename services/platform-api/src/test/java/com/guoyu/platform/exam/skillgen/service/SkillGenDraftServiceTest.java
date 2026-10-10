package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoMapper;
import com.guoyu.platform.exam.skillgen.dto.SkillPublishReq;
import com.guoyu.platform.exam.skillgen.dto.SkillPublishResultVO;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintNodeMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenBatchMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenDraftMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 草稿审核/编辑/入库单测（方案 §T4 硬闸门：编辑回退、仅 approved 可入库、总量累计门槛、渲染模板） */
class SkillGenDraftServiceTest {

  private static final Long TENANT = 1L;
  private static final Long USER = 9L;
  private static final Long BATCH_ID = 300L;
  private static final Long BP_ID = 100L;

  private SkillGenDraftMapper draftMapper;
  private SkillGenBatchMapper batchMapper;
  private SkillBlueprintMapper blueprintMapper;
  private SkillBlueprintNodeMapper nodeMapper;
  private SkillBlueprintApplicationService blueprintService;
  private ExamTaskExamshicaoMapper taskMapper;
  private ExamTaskExamshicaoDetailMapper detailMapper;
  private SkillGenDraftService service;

  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, SkillGenDraftEntity.class);
    TableInfoHelper.initTableInfo(assistant, SkillBlueprintNodeEntity.class);
  }

  @BeforeEach
  void setUp() {
    draftMapper = mock(SkillGenDraftMapper.class);
    batchMapper = mock(SkillGenBatchMapper.class);
    blueprintMapper = mock(SkillBlueprintMapper.class);
    nodeMapper = mock(SkillBlueprintNodeMapper.class);
    blueprintService = mock(SkillBlueprintApplicationService.class);
    taskMapper = mock(ExamTaskExamshicaoMapper.class);
    detailMapper = mock(ExamTaskExamshicaoDetailMapper.class);
    service = new SkillGenDraftService(draftMapper, batchMapper, blueprintMapper, nodeMapper,
        blueprintService, taskMapper, detailMapper, new ObjectMapper());
  }

  private SkillGenDraftEntity draft(long id, String review, int publish, long nodeId) {
    SkillGenDraftEntity d = new SkillGenDraftEntity();
    d.setId(id);
    d.setTenantId(TENANT);
    d.setBatchId(BATCH_ID);
    d.setBlueprintId(BP_ID);
    d.setNodeId(nodeId);
    d.setQuestionName("联机启动集控室监控设备");
    d.setScore(new BigDecimal(100));
    d.setWeight(new BigDecimal("40.00"));
    d.setDurationMinutes(20);
    d.setExamForm("实操");
    d.setSitePreparation("{\"note\":\"说明\",\"equipment\":[{\"name\":\"主机\",\"spec\":\"x\","
        + "\"unit\":\"套\",\"qty\":\"1\",\"remark\":\"\"}],\"faultSetup\":\"一处异常\"}");
    d.setExamineePrepare("黑色签字笔");
    d.setRequirements("完成联机启动并确认通信正常");
    d.setNegatives("若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：违规");
    d.setScoringItems("[{\"item\":\"开机检查\",\"score\":\"50\",\"scoreDesc\":\"x\","
        + "\"maxDeduct\":\"50\",\"step\":\"10\"},{\"item\":\"通信确认\",\"score\":\"50\","
        + "\"scoreDesc\":\"y\",\"maxDeduct\":\"50\",\"step\":\"10\"}]");
    d.setDifficulty(3);
    d.setReviewStatus(review);
    d.setPublishStatus(publish);
    return d;
  }

  @Test
  void edit_resetsReviewStatusToDraft() {
    SkillGenDraftEntity existing = draft(1L, SkillGenDraftEntity.REVIEW_APPROVED, 0, 11L);
    existing.setReviewBy(USER);
    when(draftMapper.selectById(1L)).thenReturn(existing);
    SkillGenDraftEntity patch = new SkillGenDraftEntity();
    patch.setRequirements("修改后的考核要求");

    var vo = service.edit(TENANT, USER, 1L, patch);

    assertEquals(SkillGenDraftEntity.REVIEW_DRAFT, vo.reviewStatus(), "编辑后必须回退待审（S2）");
    ArgumentCaptor<SkillGenDraftEntity> cap = ArgumentCaptor.forClass(SkillGenDraftEntity.class);
    verify(draftMapper).updateById(cap.capture());
    assertEquals(SkillGenDraftEntity.REVIEW_DRAFT, cap.getValue().getReviewStatus());
    assertNull(cap.getValue().getReviewBy());
    assertNull(cap.getValue().getReviewAt());
  }

  @Test
  void edit_rejectsPublishedDraft() {
    when(draftMapper.selectById(1L)).thenReturn(
        draft(1L, SkillGenDraftEntity.REVIEW_APPROVED, 1, 11L));
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.edit(TENANT, USER, 1L, new SkillGenDraftEntity()));
    assertTrue(ex.getMessage().contains("已入库"));
  }

  @Test
  void edit_rejectsScoringTotalNot100() {
    SkillGenDraftEntity owned = draft(1L, SkillGenDraftEntity.REVIEW_DRAFT, 0, 11L);
    when(draftMapper.selectById(1L)).thenReturn(owned);
    SkillGenDraftEntity patch = new SkillGenDraftEntity();
    patch.setScoringItems("[{\"item\":\"a\",\"score\":\"40\",\"scoreDesc\":\"x\","
        + "\"maxDeduct\":\"40\",\"step\":\"10\"}]");
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.edit(TENANT, USER, 1L, patch));
    assertTrue(ex.getMessage().contains("必须等于 100"));
  }

  @Test
  void publish_requiresApprovedDrafts() {
    when(batchMapper.selectById(BATCH_ID)).thenReturn(batch());
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(draftMapper.selectList(any())).thenReturn(new ArrayList<>());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.publish(TENANT, USER, new SkillPublishReq(BATCH_ID, "create", null)));
    assertTrue(ex.getMessage().contains("无审核通过的草稿可入库"));
  }

  @Test
  void publish_blocksWhenCumulativeBelowFloor() {
    when(batchMapper.selectById(BATCH_ID)).thenReturn(batch());
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    List<SkillGenDraftEntity> drafts = new ArrayList<>();
    for (long i = 1; i <= 5; i++) {
      drafts.add(draft(i, SkillGenDraftEntity.REVIEW_APPROVED, 0, 10 + i));
    }
    when(draftMapper.selectList(any())).thenReturn(drafts);
    // 二级下限 30；累计仅 10 条 + 本次 5 = 15 < 30 → 拦截
    when(blueprintService.cumulativeStat(TENANT, 22, 2))
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(10, 10));
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.publish(TENANT, USER, new SkillPublishReq(BATCH_ID, "create", null)));
    assertTrue(ex.getMessage().contains("低于本等级题库总量下限"));
    assertTrue(ex.getMessage().contains("还差"));
  }

  @Test
  void publish_success_mapsFieldsAndPingfenContract() {
    when(batchMapper.selectById(BATCH_ID)).thenReturn(batch());
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    List<SkillGenDraftEntity> drafts = new ArrayList<>();
    for (long i = 1; i <= 5; i++) {
      drafts.add(draft(i, SkillGenDraftEntity.REVIEW_APPROVED, 0, 10 + i));
    }
    when(draftMapper.selectList(any())).thenReturn(drafts);
    when(blueprintService.cumulativeStat(TENANT, 22, 2))
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(100, 90))   // before
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(105, 95));  // after
    // 节点：考评点 + 二级（权重/选考方式）+ 一级
    SkillBlueprintNodeEntity point = node(11L, 12L, 3, "A-A-001", "联机启动集控室监控设备");
    point.setImportance("X");
    SkillBlueprintNodeEntity l2 = node(12L, 13L, 2, "A-A", "设备准备");
    l2.setWeight(new BigDecimal("40.00"));
    l2.setSelectMode("任选一项");
    SkillBlueprintNodeEntity l1 = node(13L, 0L, 1, "A", "作业环境确认");
    when(blueprintService.findBlueprintNodes(TENANT, BP_ID)).thenReturn(List.of(l1, l2, point));
    SkillGenBatchEntity batch = batch();
    when(batchMapper.selectById(BATCH_ID)).thenReturn(batch);
    // 主表/明细 insert 时回填自增 id（IdType.AUTO 由数据库生成，mock 需模拟）
    org.mockito.Mockito.doAnswer(inv -> {
      ((ExamTaskExamshicaoEntity) inv.getArgument(0)).setId(30);
      return 1;
    }).when(taskMapper).insert(any(ExamTaskExamshicaoEntity.class));
    java.util.concurrent.atomic.AtomicInteger detailId = new java.util.concurrent.atomic.AtomicInteger(200);
    org.mockito.Mockito.doAnswer(inv -> {
      ((ExamTaskExamshicaoDetailEntity) inv.getArgument(0)).setId(detailId.getAndIncrement());
      return 1;
    }).when(detailMapper).insert(any(ExamTaskExamshicaoDetailEntity.class));

    SkillPublishResultVO result = service.publish(TENANT, USER, new SkillPublishReq(BATCH_ID, "create", null));

    assertEquals(5, result.published());
    assertEquals(30L, result.taskId());
    assertEquals(105L, result.cumulativeTotal());
    assertEquals(95L, result.cumulativeAiTotal());
    ArgumentCaptor<ExamTaskExamshicaoDetailEntity> cap =
        ArgumentCaptor.forClass(ExamTaskExamshicaoDetailEntity.class);
    verify(detailMapper, times(5)).insert(cap.capture());
    ExamTaskExamshicaoDetailEntity detail = cap.getAllValues().get(0);
    assertEquals("联机启动集控室监控设备", detail.getJianding());
    assertTrue(detail.getContent().contains("【试题名称】"));
    assertTrue(detail.getContent().contains("【否定项】"));
    // 考场/考生准备均为纯清单（2026-09-11 需求方确认，入库与导出同版式）
    assertEquals("主机×1套", detail.getKaochang());
    assertEquals("黑色签字笔", detail.getKaosheng());
    assertEquals("3", detail.getNanyicd());
    assertEquals(Integer.valueOf(2), detail.getSource());
    assertEquals(BATCH_ID, detail.getAiBatchId());
    assertNull(detail.getXingwei());
    assertNull(detail.getKaohe1());
    // pingfen 五键契约（键序 item/score/scoreDesc/maxDeduct/step，字符串值）
    assertTrue(detail.getPingfen().startsWith("[{\"item\":\"开机检查\",\"score\":\"50\",\"scoreDesc\":\"x\""));
    assertTrue(detail.getPingfen().contains("\"maxDeduct\":\"50\",\"step\":\"10\""));
    // 任务 nums 原子自增（S13）
    verify(taskMapper).incrementNums(any(), org.mockito.ArgumentMatchers.eq(5));
  }

  @Test
  void publishPreview_reportsCumulativeAndGap() {
    when(batchMapper.selectById(BATCH_ID)).thenReturn(batch());
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    // 本次已审通过 5 条
    when(draftMapper.selectCount(any())).thenReturn(5L);
    // 累计仅 10 条 → 10+5=15 < 30，缺口 15 → 不可入库
    when(blueprintService.cumulativeStat(TENANT, 22, 2))
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(10, 10));
    var vo = service.publishPreview(TENANT, BATCH_ID);
    assertEquals(5L, vo.get("approvedCount"));
    assertEquals(30, vo.get("floor"));
    assertEquals(15L, vo.get("afterTotal"));
    assertEquals(15L, vo.get("gap"));
    assertEquals(false, vo.get("canPublish"));

    // 累计充足 → 可入库
    when(blueprintService.cumulativeStat(TENANT, 22, 2))
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(100, 90));
    var vo2 = service.publishPreview(TENANT, BATCH_ID);
    assertEquals(true, vo2.get("canPublish"));
    assertEquals(0L, vo2.get("gap"));
  }

  private SkillGenBatchEntity batch() {
    SkillGenBatchEntity b = new SkillGenBatchEntity();
    b.setId(BATCH_ID);
    b.setTenantId(TENANT);
    b.setBlueprintId(BP_ID);
    b.setWorkTypeId(22);
    b.setLevel(2);
    return b;
  }

  private SkillBlueprintEntity blueprint() {
    SkillBlueprintEntity bp = new SkillBlueprintEntity();
    bp.setId(BP_ID);
    bp.setTenantId(TENANT);
    bp.setWorkTypeId(22);
    bp.setLevel(2);
    bp.setTitle("矿山生产集控员（二级/技师）操作技能考评要素细察表");
    return bp;
  }

  private SkillBlueprintNodeEntity node(long id, long parentId, int level, String fullCode, String name) {
    SkillBlueprintNodeEntity n = new SkillBlueprintNodeEntity();
    n.setId(id);
    n.setParentId(parentId);
    n.setNodeLevel(level);
    n.setFullCode(fullCode);
    n.setName(name);
    return n;
  }
}
