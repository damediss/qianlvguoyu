package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.exam.reviewsync.mapper.ReviewSyncStatsMapper;
import com.guoyu.platform.trainingdepartment.mapper.ReviewSyncPanelMapper;
import com.guoyu.platform.trainingdepartment.vo.ReviewSyncPanelVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 运营数据面板「同步练习」分区（《运营数据面板_同步练习统计面板方案》v0.1 §5）。
 *
 * <p>直查聚合（本期不建预聚合表）；范围口径复用 ReviewSyncStatsMapper，
 * 全局口径走 ReviewSyncPanelMapper；所有比率除零返回 null，由前端显示"--"。
 */
@Slf4j
@Service
public class ReviewSyncPanelApplicationService {

  /** 面板默认统计窗口（天）；不接受 92 天上限口径，仅限制在 1..365 防止滥用 */
  private static final int DEFAULT_DAYS = 30;
  /** 薄弱项目参评门槛：近 N 天作答次数低于该值不参与（防噪声，方案 §3.3） */
  private static final long WEAK_MIN_ANSWERS = 50L;
  private static final int WEAK_TOP_N = 5;

  private final ReviewSyncStatsMapper statsMapper;
  private final ReviewSyncPanelMapper panelMapper;

  public ReviewSyncPanelApplicationService(ReviewSyncStatsMapper statsMapper,
      ReviewSyncPanelMapper panelMapper) {
    this.statsMapper = statsMapper;
    this.panelMapper = panelMapper;
  }

  public ReviewSyncPanelVO getPanel(Integer daysParam) {
    int days = (daysParam == null || daysParam < 1 || daysParam > 365) ? DEFAULT_DAYS : daysParam;
    LocalDate to = LocalDate.now();
    LocalDate from = to.minusDays(days - 1L);

    ReviewSyncPanelVO.Scale scale = buildScale();
    return new ReviewSyncPanelVO(scale, buildEffort(from, to, scale.totalPlans()),
        buildEffect(from, to), buildTrend(from, to), buildWrongTrend(from, to),
        buildWeakTop(from, to));
  }

  // ------------------------------------------------------------------
  // 规模（用没用）
  // ------------------------------------------------------------------
  private ReviewSyncPanelVO.Scale buildScale() {
    long total = 0;
    long active = 0;
    long completed = 0;
    long abandoned = 0;
    long interrupt = 0;
    for (Map<String, Object> row : nullToEmpty(panelMapper.planStatusAgg())) {
      String status = asString(row.get("status"));
      long cnt = asLong(row.get("cnt"));
      total += cnt;
      interrupt += asLong(row.get("interruptCnt"));
      switch (status == null ? "" : status) {
        case "active" -> active += cnt;
        case "completed" -> completed += cnt;
        case "abandoned" -> abandoned += cnt;
        default -> {
          // 其他状态计入总数即可
        }
      }
    }
    long active7d = defaultZero(panelMapper.activeUsers7d());
    return new ReviewSyncPanelVO.Scale(total, active, completed, abandoned,
        ratio(abandoned, total), active7d, ratio(active7d, active), interrupt);
  }

  // ------------------------------------------------------------------
  // 投入（勤不勤）
  // ------------------------------------------------------------------
  private ReviewSyncPanelVO.Effort buildEffort(LocalDate from, LocalDate to, long totalPlans) {
    long totalAnswers = defaultZero(panelMapper.totalAnswers());
    List<Map<String, Object>> dayRows = nullToEmpty(statsMapper.dailyDayCompletion(from, to, null));
    long totalDays = 0;
    long doneDays = 0;
    for (Map<String, Object> row : dayRows) {
      totalDays += asLong(row.get("totalDays"));
      doneDays += asLong(row.get("doneDays"));
    }
    Map<String, Object> today = statsMapper.answerTotals(to, to, null);
    long todayAnswers = today == null ? 0 : asLong(today.get("answers"));
    long todayActive = today == null ? 0 : asLong(today.get("activeUsers"));
    // 人均作答：无计划学员时置 null（前端"--"），有计划但零作答显示 0
    BigDecimal avgPerPlan = totalPlans == 0 ? null
        : BigDecimal.valueOf(totalAnswers)
            .divide(BigDecimal.valueOf(totalPlans), 2, RoundingMode.HALF_UP);
    return new ReviewSyncPanelVO.Effort(totalAnswers, avgPerPlan,
        ratio(doneDays, totalDays), todayActive, todayAnswers);
  }

  // ------------------------------------------------------------------
  // 效果（好不好）
  // ------------------------------------------------------------------
  private ReviewSyncPanelVO.Effect buildEffect(LocalDate from, LocalDate to) {
    Map<String, Object> totals = statsMapper.answerTotals(from, to, null);
    long answers = totals == null ? 0 : asLong(totals.get("answers"));
    long corrects = totals == null ? 0 : asLong(totals.get("corrects"));

    long removed = 0;
    long red = 0;
    long yellow = 0;
    for (Map<String, Object> row : nullToEmpty(statsMapper.wrongSnapshot(null, null))) {
      long cnt = asLong(row.get("cnt"));
      if (asLong(row.get("removed")) == 1L) {
        removed += cnt;
      } else {
        String level = asString(row.get("cardLevel"));
        if ("red".equals(level)) {
          red += cnt;
        } else if ("yellow".equals(level)) {
          yellow += cnt;
        }
      }
    }

    Map<String, Object> mock = panelMapper.mockSummary();
    long mockDone = mock == null ? 0 : asLong(mock.get("doneCnt"));
    BigDecimal mockAvg = mock == null ? null : asBigDecimal(mock.get("avgScore"));
    long passCnt = mock == null ? 0 : asLong(mock.get("passCnt"));

    return new ReviewSyncPanelVO.Effect(ratio(corrects, answers), removed, red, yellow,
        mockDone, mockDone == 0 ? null : mockAvg, ratio(passCnt, mockDone));
  }

  // ------------------------------------------------------------------
  // 趋势
  // ------------------------------------------------------------------
  private List<ReviewSyncPanelVO.TrendPoint> buildTrend(LocalDate from, LocalDate to) {
    List<ReviewSyncPanelVO.TrendPoint> trend = new ArrayList<>();
    for (Map<String, Object> row : nullToEmpty(statsMapper.dailyAnswerTrend(from, to, null, null))) {
      long answers = asLong(row.get("answers"));
      long corrects = asLong(row.get("corrects"));
      trend.add(new ReviewSyncPanelVO.TrendPoint(asDate(row.get("day")), answers,
          asLong(row.get("activeUsers")), ratio(corrects, answers)));
    }
    return trend;
  }

  private List<ReviewSyncPanelVO.WrongTrendPoint> buildWrongTrend(LocalDate from, LocalDate to) {
    Map<String, long[]> byDay = new TreeMap<>();
    for (Map<String, Object> row : nullToEmpty(statsMapper.wrongDailyNew(from, to, null))) {
      byDay.computeIfAbsent(asDate(row.get("day")), k -> new long[2])[0] += asLong(row.get("cnt"));
    }
    for (Map<String, Object> row : nullToEmpty(statsMapper.wrongDailyRemoved(from, to, null))) {
      byDay.computeIfAbsent(asDate(row.get("day")), k -> new long[2])[1] += asLong(row.get("cnt"));
    }
    List<ReviewSyncPanelVO.WrongTrendPoint> result = new ArrayList<>();
    for (Map.Entry<String, long[]> entry : byDay.entrySet()) {
      result.add(new ReviewSyncPanelVO.WrongTrendPoint(entry.getKey(), entry.getValue()[0],
          entry.getValue()[1]));
    }
    return result;
  }

  // ------------------------------------------------------------------
  // 薄弱考核项目 Top5（L3 聚合 → 沿知识树上溯到 L1）
  // ------------------------------------------------------------------
  private List<ReviewSyncPanelVO.WeakItem> buildWeakTop(LocalDate from, LocalDate to) {
    List<Map<String, Object>> agg = nullToEmpty(panelMapper.knowledgeAggInRange(from, to));
    if (agg.isEmpty()) {
      return List.of();
    }
    Map<Long, long[]> l1Agg = new LinkedHashMap<>(); // l1Id -> [answers, corrects]
    Map<Long, PanelNode> nodes = loadNodeChain(agg);
    for (Map<String, Object> row : agg) {
      Long knowledgeId = asLongObj(row.get("knowledgeId"));
      if (knowledgeId == null) {
        continue;
      }
      PanelNode node = nodes.get(knowledgeId);
      Long l1Id = node == null ? null : findAncestorAtLevel(nodes, node, 1);
      long[] bucket = l1Agg.computeIfAbsent(l1Id == null ? -1L : l1Id, k -> new long[2]);
      bucket[0] += asLong(row.get("answers"));
      bucket[1] += asLong(row.get("corrects"));
    }
    return l1Agg.entrySet().stream()
        .filter(e -> e.getValue()[0] >= WEAK_MIN_ANSWERS)
        .map(e -> {
          PanelNode l1 = nodes.get(e.getKey());
          String title = l1 != null && l1.title() != null ? l1.title()
              : (e.getKey() == -1L ? "未归类" : "知识点#" + e.getKey());
          return new ReviewSyncPanelVO.WeakItem(e.getKey() == -1L ? null : e.getKey(), title,
              e.getValue()[0], ratio(e.getValue()[1], e.getValue()[0]));
        })
        .sorted(Comparator.comparing(ReviewSyncPanelVO.WeakItem::correctRate,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .limit(WEAK_TOP_N)
        .toList();
  }

  /** 按作答涉及的知识 id 加载节点，并沿 parent_id 逐层补齐祖先（最多 8 层防环） */
  private Map<Long, PanelNode> loadNodeChain(List<Map<String, Object>> agg) {
    Map<Long, PanelNode> nodes = new HashMap<>();
    Set<Long> pending = new HashSet<>();
    for (Map<String, Object> row : agg) {
      Long id = asLongObj(row.get("knowledgeId"));
      if (id != null) {
        pending.add(id);
      }
    }
    for (int depth = 0; depth < 8 && !pending.isEmpty(); depth++) {
      Set<Long> next = new HashSet<>();
      List<Long> ids = List.copyOf(pending);
      for (Map<String, Object> row : nullToEmpty(panelMapper.selectKnowledgeNodesByIds(ids))) {
        PanelNode node = new PanelNode(asLongObj(row.get("id")), asLongObj(row.get("parentId")),
            asInt(row.get("level")), asString(row.get("title")));
        if (node.id() != null) {
          nodes.put(node.id(), node);
          if (node.parentId() != null && !nodes.containsKey(node.parentId())) {
            next.add(node.parentId());
          }
        }
      }
      pending = next;
    }
    return nodes;
  }

  private Long findAncestorAtLevel(Map<Long, PanelNode> nodes, PanelNode node, int targetLevel) {
    PanelNode current = node;
    for (int i = 0; i < 8 && current != null; i++) {
      if (current.level() != null && current.level() == targetLevel) {
        return current.id();
      }
      current = current.parentId() != null ? nodes.get(current.parentId()) : null;
    }
    return null;
  }

  // ------------------------------------------------------------------
  // 工具
  // ------------------------------------------------------------------
  private record PanelNode(Long id, Long parentId, Integer level, String title) {
  }

  /** 百分数比率（0-100 两位小数）；分母为 0 返回 null，前端显示"--" */
  private static BigDecimal ratio(long numerator, long denominator) {
    if (denominator <= 0) {
      return null;
    }
    return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
  }

  private static List<Map<String, Object>> nullToEmpty(List<Map<String, Object>> rows) {
    return rows == null ? List.of() : rows;
  }

  private static long asLong(Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static Long asLongObj(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(value.toString().trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Integer asInt(Object value) {
    Long l = asLongObj(value);
    return l == null ? null : l.intValue();
  }

  private static String asString(Object value) {
    return value == null ? null : value.toString();
  }

  /** MySQL DATE() 返回的列统一为 yyyy-MM-dd 字符串 */
  private static String asDate(Object value) {
    String s = asString(value);
    if (s == null || s.isBlank()) {
      return "";
    }
    return s.length() > 10 ? s.substring(0, 10) : s;
  }

  private static BigDecimal asBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal b) {
      return b.setScale(2, RoundingMode.HALF_UP);
    }
    if (value instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }
    try {
      return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static long defaultZero(Long value) {
    return value == null ? 0 : value;
  }
}
