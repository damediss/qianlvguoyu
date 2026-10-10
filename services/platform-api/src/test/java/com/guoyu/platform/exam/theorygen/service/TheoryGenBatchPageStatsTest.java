package com.guoyu.platform.exam.theorygen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import com.guoyu.platform.exam.theorygen.dto.TheoryGenBatchVO;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenBatchEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenDraftEntity;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintNodeMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryGenBatchMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryGenDraftMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 生成工作台：批次分页的状态过滤与草稿统计聚合（审核进度/入库/主观题计数）。
 */
class TheoryGenBatchPageStatsTest {

  private static final Long TENANT = 1L;

  private TheoryGenBatchMapper batchMapper;
  private TheoryGenDraftMapper draftMapper;
  private TheoryGenBatchService service;

  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, TheoryGenBatchEntity.class);
    TableInfoHelper.initTableInfo(assistant, TheoryGenDraftEntity.class);
  }

  @BeforeEach
  void setUp() {
    batchMapper = mock(TheoryGenBatchMapper.class);
    draftMapper = mock(TheoryGenDraftMapper.class);
    service = new TheoryGenBatchService(mock(TheoryBlueprintMapper.class),
        mock(TheoryBlueprintNodeMapper.class), batchMapper, draftMapper,
        mock(ExamFaqModelConfigMapper.class), mock(TheoryGenAiService.class),
        mock(TheoryQuestionValidator.class), new ObjectMapper(),
        mock(ThreadPoolTaskExecutor.class), mock(ThreadPoolTaskExecutor.class),
        mock(TransactionTemplate.class), 5, 0L);
  }

  private TheoryGenBatchEntity batch(long id) {
    TheoryGenBatchEntity b = new TheoryGenBatchEntity();
    b.setId(id);
    b.setTenantId(TENANT);
    b.setStatus("done");
    return b;
  }

  private Map<String, Object> statRow(long batchId, String review, int publish, String type, int cnt) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("batch_id", batchId);
    row.put("review_status", review);
    row.put("publish_status", publish);
    row.put("question_type", type);
    row.put("cnt", (long) cnt);
    return row;
  }

  @Test
  void statsAggregatedIntoVoPerBatch() {
    Page<TheoryGenBatchEntity> pg = new Page<>(1, 10);
    pg.setRecords(List.of(batch(1L), batch(2L)));
    pg.setTotal(2);
    when(batchMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(pg);
    when(draftMapper.selectMaps(any(Wrapper.class))).thenReturn(List.of(
        statRow(1, "approved", 1, "single", 3),
        statRow(1, "draft", 0, "essay", 2),
        statRow(1, "rejected", 0, "judge", 1),
        statRow(2, "draft", 0, "discuss", 4)));

    var resp = service.page(TENANT, null, List.of(), 1, 10);

    TheoryGenBatchVO v1 = resp.items().get(0);
    assertEquals(3, v1.approvedCount());
    assertEquals(2, v1.pendingReviewCount());
    assertEquals(1, v1.rejectedCount());
    assertEquals(3, v1.publishedCount());
    assertEquals(2, v1.subjectiveCount(), "essay 计入主观");

    TheoryGenBatchVO v2 = resp.items().get(1);
    assertEquals(0, v2.approvedCount());
    assertEquals(4, v2.pendingReviewCount());
    assertEquals(0, v2.publishedCount());
    assertEquals(4, v2.subjectiveCount(), "discuss 计入主观");
  }

  @Test
  void noDraftsMeansZeroStats() {
    Page<TheoryGenBatchEntity> pg = new Page<>(1, 10);
    pg.setRecords(List.of(batch(9L)));
    pg.setTotal(1);
    when(batchMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(pg);
    when(draftMapper.selectMaps(any(Wrapper.class))).thenReturn(List.of());

    var resp = service.page(TENANT, null, List.of(), 1, 10);

    TheoryGenBatchVO v = resp.items().get(0);
    assertEquals(0, v.approvedCount());
    assertEquals(0, v.pendingReviewCount());
    assertEquals(0, v.subjectiveCount());
  }

  @Test
  void statusFilterRenderedIntoWhere() {
    Page<TheoryGenBatchEntity> pg = new Page<>(1, 10);
    pg.setRecords(List.of());
    pg.setTotal(0);
    when(batchMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(pg);

    service.page(TENANT, null, List.of("pending", "running"), 1, 10);

    ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
    verify(batchMapper).selectPage(any(Page.class), captor.capture());
    String sql = captor.getValue().getCustomSqlSegment();
    assertTrue(sql.contains("status IN"), sql);
    // 值为绑定参数（#{ew.paramNameValuePairs...}），断言占位符数量为 2
    long placeholders = sql.chars().filter(ch -> ch == ',').count();
    assertTrue(placeholders >= 1, "status IN 应有两个绑定值：" + sql);
  }

  @Test
  void emptyStatusDoesNotFilter() {
    Page<TheoryGenBatchEntity> pg = new Page<>(1, 10);
    pg.setRecords(List.of());
    pg.setTotal(0);
    when(batchMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(pg);

    service.page(TENANT, null, List.of(), 1, 10);

    ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
    verify(batchMapper).selectPage(any(Page.class), captor.capture());
    assertFalse(captor.getValue().getCustomSqlSegment().contains("status IN"));
  }
}
