package com.guoyu.platform.trainingdepartment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import com.guoyu.platform.exam.reviewsync.mapper.ReviewSyncStatsMapper;
import com.guoyu.platform.trainingdepartment.mapper.ReviewSyncPanelMapper;
import com.guoyu.platform.trainingdepartment.vo.ReviewSyncPanelVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 同步练习统计面板 Service 单测（方案 §9）：除零兜底、放弃率/及格率/活跃率/人均计算、
 * 默认固定 30 天且不受 92 天上限约束、薄弱 Top5 过滤（≥50）与排序截断。
 */
@ExtendWith(MockitoExtension.class)
class ReviewSyncPanelApplicationServiceTest {

  @Mock
  private ReviewSyncStatsMapper statsMapper;
  @Mock
  private ReviewSyncPanelMapper panelMapper;

  private ReviewSyncPanelApplicationService service;

  @BeforeEach
  void setUp() {
    service = new ReviewSyncPanelApplicationService(statsMapper, panelMapper);
  }

  private static Map<String, Object> row(Object... kv) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i + 1 < kv.length; i += 2) {
      map.put((String) kv[i], kv[i + 1]);
    }
    return map;
  }

  private static BigDecimal bd(String v) {
    return new BigDecimal(v);
  }

  @Test
  void allEmptyEveryRatioNull() {
    ReviewSyncPanelVO vo = service.getPanel(null);

    assertEquals(0, vo.scale().totalPlans());
    assertEquals(0, vo.scale().newPlans7d());
    assertEquals(0, vo.scale().active7d());
    assertNull(vo.scale().activeRate7d());
    assertEquals(0, vo.effort().totalAnswers());
    assertNull(vo.effort().avgAnswersPerPlan());
    assertNull(vo.effort().dayCompletionRate());
    assertEquals(0, vo.effort().todayAnswers());
    assertNull(vo.effect().correctRate());
    assertEquals(0, vo.effect().removedWrongs());
    assertEquals(0, vo.effect().mockDone());
    assertNull(vo.effect().mockAvgScore());
    assertNull(vo.effect().mockPassRate());
    assertTrue(vo.trend30d().isEmpty());
    assertTrue(vo.wrongTrend30d().isEmpty());
    assertTrue(vo.weakTop5().isEmpty());
  }

  @Test
  void ratesComputedFromAggregates() {
    doReturn(List.of(
        row("status", "active", "cnt", 6L, "interruptCnt", 2L),
        row("status", "completed", "cnt", 3L, "interruptCnt", 0L),
        row("status", "abandoned", "cnt", 1L, "interruptCnt", 0L)))
        .when(panelMapper).planStatusAgg();
    doReturn(3L).when(panelMapper).activeUsers7d();
    doReturn(7L).when(panelMapper).newPlans7d();
    doReturn(7010L).when(panelMapper).totalAnswers();
    doReturn(List.of(
        row("totalDays", 60L, "doneDays", 50L),
        row("totalDays", 40L, "doneDays", 32L)))
        .when(statsMapper).dailyDayCompletion(any(), any(), isNull());
    doReturn(row("answers", 1000L, "corrects", 685L))
        .when(statsMapper).answerTotals(any(), any(), isNull());
    // 同日 answerTotals(to,to) 也是上面那桩（返回值共用），今日口径无需再区分
    doReturn(List.of(
        row("cardLevel", "red", "removed", 0, "cnt", 34L),
        row("cardLevel", "yellow", "removed", 0, "cnt", 21L),
        row("cardLevel", "green", "removed", 1, "cnt", 500L)))
        .when(statsMapper).wrongSnapshot(isNull(), isNull());
    doReturn(row("doneCnt", 10L, "avgScore", new BigDecimal("72.4"), "passCnt", 6L))
        .when(panelMapper).mockSummary();

    ReviewSyncPanelVO vo = service.getPanel(30);

    assertEquals(10, vo.scale().totalPlans());
    assertEquals(6, vo.scale().active());
    assertEquals(7, vo.scale().newPlans7d());
    assertEquals(2, vo.scale().interruptPlans());
    assertEquals(0, vo.scale().activeRate7d().compareTo(bd("50.00")));
    assertEquals(7010, vo.effort().totalAnswers());
    assertEquals(0, vo.effort().avgAnswersPerPlan().compareTo(bd("701.00")));
    assertEquals(0, vo.effort().dayCompletionRate().compareTo(bd("82.00")));
    assertEquals(0, vo.effect().correctRate().compareTo(bd("68.50")));
    assertEquals(500, vo.effect().removedWrongs());
    assertEquals(34, vo.effect().redWrongs());
    assertEquals(21, vo.effect().yellowWrongs());
    assertEquals(10, vo.effect().mockDone());
    assertEquals(0, vo.effect().mockAvgScore().compareTo(bd("72.40")));
    assertEquals(0, vo.effect().mockPassRate().compareTo(bd("60.00")));
  }

  @Test
  void daysDefaultsToThirtyAndNo92DayCap() {
    ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
    ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);

    service.getPanel(null);
    Mockito.verify(statsMapper).dailyAnswerTrend(fromCaptor.capture(), toCaptor.capture(),
        isNull(), isNull());
    assertEquals(LocalDate.now().minusDays(29), fromCaptor.getValue());
    assertEquals(LocalDate.now(), toCaptor.getValue());

    // 面板不受 92 天上限约束：days=200 原样透传（from = today-199）
    Mockito.clearInvocations(statsMapper);
    service.getPanel(200);
    Mockito.verify(statsMapper).dailyAnswerTrend(fromCaptor.capture(), any(), isNull(), isNull());
    assertEquals(LocalDate.now().minusDays(199), fromCaptor.getValue());
  }

  @Test
  void weakTopFiltersBelow50AndSortsAscendingTop5() {
    // 知识树：L3(11,12,21,31,41,51,61) → L2(101,201,301,401,501,601) → L1(1..6)
    Map<Long, Map<String, Object>> nodes = new HashMap<>();
    nodes.put(1L, row("id", 1L, "parentId", 0L, "level", 1, "title", "基础知识"));
    nodes.put(2L, row("id", 2L, "parentId", 0L, "level", 1, "title", "专业技能"));
    nodes.put(3L, row("id", 3L, "parentId", 0L, "level", 1, "title", "安全知识"));
    nodes.put(4L, row("id", 4L, "parentId", 0L, "level", 1, "title", "电工基础"));
    nodes.put(5L, row("id", 5L, "parentId", 0L, "level", 1, "title", "仪表使用"));
    nodes.put(6L, row("id", 6L, "parentId", 0L, "level", 1, "title", "法规"));
    nodes.put(101L, row("id", 101L, "parentId", 1L, "level", 2, "title", "L2-101"));
    nodes.put(201L, row("id", 201L, "parentId", 2L, "level", 2, "title", "L2-201"));
    nodes.put(301L, row("id", 301L, "parentId", 3L, "level", 2, "title", "L2-301"));
    nodes.put(401L, row("id", 401L, "parentId", 4L, "level", 2, "title", "L2-401"));
    nodes.put(501L, row("id", 501L, "parentId", 5L, "level", 2, "title", "L2-501"));
    nodes.put(601L, row("id", 601L, "parentId", 6L, "level", 2, "title", "L2-601"));
    nodes.put(11L, row("id", 11L, "parentId", 101L, "level", 3, "title", "P11"));
    nodes.put(12L, row("id", 12L, "parentId", 101L, "level", 3, "title", "P12"));
    nodes.put(21L, row("id", 21L, "parentId", 201L, "level", 3, "title", "P21"));
    nodes.put(31L, row("id", 31L, "parentId", 301L, "level", 3, "title", "P31"));
    nodes.put(41L, row("id", 41L, "parentId", 401L, "level", 3, "title", "P41"));
    nodes.put(51L, row("id", 51L, "parentId", 501L, "level", 3, "title", "P51"));
    nodes.put(61L, row("id", 61L, "parentId", 601L, "level", 3, "title", "P61"));

    doAnswer(inv -> {
      List<Long> ids = inv.getArgument(0);
      List<Map<String, Object>> result = new ArrayList<>();
      for (Long id : ids) {
        if (nodes.containsKey(id)) {
          result.add(nodes.get(id));
        }
      }
      return result;
    }).when(panelMapper).selectKnowledgeNodesByIds(anyList());

    doReturn(List.of(
        row("knowledgeId", 11L, "answers", 60L, "corrects", 30L),   // L1-1 50%
        row("knowledgeId", 12L, "answers", 10L, "corrects", 10L),   // 并入 L1-1 → 70/40=57.14
        row("knowledgeId", 21L, "answers", 120L, "corrects", 36L),  // L1-2 30%
        row("knowledgeId", 31L, "answers", 55L, "corrects", 11L),   // L1-3 20%
        row("knowledgeId", 41L, "answers", 100L, "corrects", 90L),  // L1-4 90%
        row("knowledgeId", 51L, "answers", 50L, "corrects", 25L),   // L1-5 50%
        row("knowledgeId", 61L, "answers", 40L, "corrects", 4L),    // 作答 <50 被过滤
        row("knowledgeId", 999L, "answers", 80L, "corrects", 8L)))  // 节点缺失 → 未归类 10%
        .when(panelMapper).knowledgeAggInRange(any(), any());

    ReviewSyncPanelVO vo = service.getPanel(30);
    List<ReviewSyncPanelVO.WeakItem> weak = vo.weakTop5();

    // 正确率升序：未归类10 < 安全知识20 < 专业技能30 < 仪表使用50 < 基础知识57.14；最强 L1-4(90%) 被 Top5 截断
    assertEquals(5, weak.size());
    assertNull(weak.get(0).knowledgeId());
    assertEquals("未归类", weak.get(0).title());
    assertEquals(0, weak.get(0).correctRate().compareTo(bd("10.00")));
    assertEquals("安全知识", weak.get(1).title());
    assertEquals(0, weak.get(1).correctRate().compareTo(bd("20.00")));
    assertEquals("专业技能", weak.get(2).title());
    assertEquals("仪表使用", weak.get(3).title());
    assertEquals(0, weak.get(3).correctRate().compareTo(bd("50.00")));
    assertEquals("基础知识", weak.get(4).title());
    assertEquals(0, weak.get(4).correctRate().compareTo(bd("57.14")));
    assertEquals(70, weak.get(4).answers());
    assertTrue(weak.stream().noneMatch(w -> "电工基础".equals(w.title())));
  }

  @Test
  void trendPointCorrectRateNullWhenNoAnswers() {
    doReturn(List.of(
        row("day", "2026-09-28", "answers", 0L, "corrects", 0L, "activeUsers", 0L),
        row("day", "2026-09-29", "answers", 100L, "corrects", 70L, "activeUsers", 5L)))
        .when(statsMapper).dailyAnswerTrend(any(), any(), isNull(), isNull());
    doReturn(List.of(row("day", "2026-09-29", "cnt", 3L))).when(statsMapper)
        .wrongDailyNew(any(), any(), isNull());
    doReturn(List.of(row("day", "2026-09-29", "cnt", 5L))).when(statsMapper)
        .wrongDailyRemoved(any(), any(), isNull());

    ReviewSyncPanelVO vo = service.getPanel(30);

    assertEquals(2, vo.trend30d().size());
    assertNull(vo.trend30d().get(0).correctRate());
    assertEquals(0, vo.trend30d().get(1).correctRate().compareTo(bd("70.00")));
    assertEquals(5, vo.trend30d().get(1).activeUsers());
    assertEquals(1, vo.wrongTrend30d().size());
    assertEquals(3, vo.wrongTrend30d().get(0).newCnt());
    assertEquals(5, vo.wrongTrend30d().get(0).removedCnt());
  }
}
