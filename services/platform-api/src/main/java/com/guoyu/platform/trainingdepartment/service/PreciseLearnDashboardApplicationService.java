package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.exam.service.AppExamSmartService;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewWrongItemEntity;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewWrongItemMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AI 精准学 Hub 聚合（编排层）—— 只读组合通道 C（同步学）与通道 A（专项刷题）。
 *
 * <p>红线：不合并 A/B/C 答卷、不返回跨通道综合分、不改 Scheduler。
 *
 * <p>Hub 页内唯一开练入口：通道 A similar-wrong；错题情况展示题型错误率、未掌握知识点与近期错题。
 */
@Service
public class PreciseLearnDashboardApplicationService {

  private static final Logger log = LoggerFactory.getLogger(PreciseLearnDashboardApplicationService.class);
  private static final int WEAK_LIMIT = 12;
  private static final double SPECIAL_WEAK_RATE = 30.0;

  private final TrainClassRoleApplicationService roleService;
  private final ClassReviewSyncApplicationService reviewSyncService;
  private final ClassPracticeScopeApplicationService practiceScopeService;
  private final AppExamSmartService appExamSmartService;
  private final AiReviewPlanMapper planMapper;
  private final AiReviewWrongItemMapper wrongItemMapper;
  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;

  public PreciseLearnDashboardApplicationService(
      TrainClassRoleApplicationService roleService,
      ClassReviewSyncApplicationService reviewSyncService,
      ClassPracticeScopeApplicationService practiceScopeService,
      AppExamSmartService appExamSmartService,
      AiReviewPlanMapper planMapper,
      AiReviewWrongItemMapper wrongItemMapper,
      ExamKnowledgeStructureMapper knowledgeStructureMapper) {
    this.roleService = roleService;
    this.reviewSyncService = reviewSyncService;
    this.practiceScopeService = practiceScopeService;
    this.appExamSmartService = appExamSmartService;
    this.planMapper = planMapper;
    this.wrongItemMapper = wrongItemMapper;
    this.knowledgeStructureMapper = knowledgeStructureMapper;
  }

  public Map<String, Object> dashboard(Long platformUserId, Long classId) {
    roleService.assertClassMember(platformUserId, classId);

    Map<String, Object> syncEntry = reviewSyncService.resolveForAppMember(classId, platformUserId);
    boolean entryVisible = Boolean.TRUE.equals(syncEntry.get("entryVisible"));
    Long examId = toLong(syncEntry.get("examId"));

    Map<String, Object> scope = practiceScopeService.getScopeForAppMember(classId);
    boolean scopeEnabled = toInt(scope.get("enabled"), 1) != 0;

    List<Map<String, Object>> weakItems = new ArrayList<>();
    Map<String, Object> diagnosisSync = null;
    Map<String, Object> diagnosisSpecial = null;
    Map<String, Object> entriesSync = null;
    Map<String, Object> entriesSpecial = null;

    // 入口先落盘：不依赖慢路径 overview/progress
    if (entryVisible && examId != null && examId > 0) {
      entriesSync = Map.of("examId", String.valueOf(examId), "entryVisible", true);
      try {
        // 轻量读已有计划 + 错题卡，避免 overview 物化今日任务 / progress 全量扫描
        AiReviewPlanEntity plan = planMapper.selectOne(new LambdaQueryWrapper<AiReviewPlanEntity>()
            .eq(AiReviewPlanEntity::getPlatformUserId, platformUserId)
            .eq(AiReviewPlanEntity::getExamId, examId)
            .ne(AiReviewPlanEntity::getStatus, "abandoned")
            .last("LIMIT 1"));
        if (plan != null && plan.getId() != null) {
          List<AiReviewWrongItemEntity> wrongRows = listActiveWrongItems(plan.getId());
          diagnosisSync = buildSyncDiagnosisFromPlan(examId, plan, wrongRows);
          weakItems.addAll(buildSyncWeakItemsFromWrong(wrongRows, classId));
        }
      } catch (Exception e) {
        log.warn("[精准学] 同步学轻量聚合失败 classId={} examId={}: {}", classId, examId, e.getMessage());
      }
    }

    try {
      Map<String, Object> predict = appExamSmartService.knowledgePredict();
      Map<String, Object> wrongBook = appExamSmartService.wrongBook(Map.of("page", 1, "limit", 8));
      List<Map<String, Object>> typeStats = appExamSmartService.wrongStatsByQuestionType();
      diagnosisSpecial = buildSpecialDiagnosis(predict, wrongBook);
      weakItems.addAll(buildSpecialWeakItems(predict, classId));
      // 下方会拼进 wrongOverview；先暂存
      diagnosisSpecial.put("_predictItems", predict.get("items"));
      diagnosisSpecial.put("_typeStats", typeStats);
      diagnosisSpecial.put("_wrongList", wrongBook.get("list"));
    } catch (Exception e) {
      log.warn("[精准学] 专项聚合失败 classId={}: {}", classId, e.getMessage());
    }

    if (scopeEnabled) {
      entriesSpecial = Map.of("scopeEnabled", true);
    }

    weakItems.sort(weakComparator());
    if (weakItems.size() > WEAK_LIMIT) {
      weakItems = new ArrayList<>(weakItems.subList(0, WEAK_LIMIT));
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("weakItems", weakItems);
    // 兼容保留；Hub 开练已改为综合 similar-wrong，不再依赖逐条推荐
    out.put("recommended", weakItems.isEmpty() ? null : weakItems.get(0));
    Map<String, Object> diagnosis = new LinkedHashMap<>();
    diagnosis.put("sync", diagnosisSync);
    // 对外诊断卡去掉内部暂存字段
    Map<String, Object> specialPublic = stripInternalDiagnosis(diagnosisSpecial);
    diagnosis.put("special", specialPublic);
    out.put("diagnosis", diagnosis);
    out.put("wrongOverview", buildWrongOverview(diagnosisSync, diagnosisSpecial, weakItems));
    try {
      Map<String, Object> effect = appExamSmartService.precisePracticeEffect();
      @SuppressWarnings("unchecked")
      Map<String, Object> overview = out.get("wrongOverview") instanceof Map
          ? (Map<String, Object>) out.get("wrongOverview")
          : null;
      if (overview != null && effect != null) {
        overview.put("effect", effect);
      }
    } catch (Exception e) {
      log.warn("[精准学] 练后效果聚合失败 classId={}: {}", classId, e.getMessage());
    }
    Map<String, Object> entries = new LinkedHashMap<>();
    entries.put("sync", entriesSync);
    entries.put("special", entriesSpecial);
    out.put("entries", entries);
    return out;
  }

  private static Map<String, Object> stripInternalDiagnosis(Map<String, Object> diagnosisSpecial) {
    if (diagnosisSpecial == null) {
      return null;
    }
    Map<String, Object> copy = new LinkedHashMap<>(diagnosisSpecial);
    copy.remove("_predictItems");
    copy.remove("_typeStats");
    copy.remove("_wrongList");
    return copy;
  }

  /** 学员错题分项：题型错误率、未掌握知识点、近期错题题干。 */
  private Map<String, Object> buildWrongOverview(
      Map<String, Object> diagnosisSync,
      Map<String, Object> diagnosisSpecial,
      List<Map<String, Object>> weakItems) {
    Map<String, Object> overview = new LinkedHashMap<>();

    int syncRed = 0;
    int syncYellow = 0;
    int syncStar = 0;
    if (diagnosisSync != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> ws = diagnosisSync.get("wrongStats") instanceof Map
          ? (Map<String, Object>) diagnosisSync.get("wrongStats")
          : Map.of();
      syncRed = toInt(ws.get("red"), 0);
      syncYellow = toInt(ws.get("yellow"), 0);
      syncStar = toInt(ws.get("star"), 0);
      overview.put("syncStageName", diagnosisSync.get("stageName"));
      overview.put("syncDayIndex", diagnosisSync.get("dayIndex"));
      overview.put("syncTotalDays", diagnosisSync.get("totalDays"));
    }
    Map<String, Object> sync = new LinkedHashMap<>();
    sync.put("red", syncRed);
    sync.put("yellow", syncYellow);
    sync.put("star", syncStar);
    sync.put("cardTotal", syncRed + syncYellow);
    overview.put("sync", sync);

    // 同步学：按知识点卡面汇总（看哪些点没掌握）
    List<Map<String, Object>> syncKnowledges = new ArrayList<>();
    for (Map<String, Object> w : weakItems) {
      if (!"sync".equals(String.valueOf(w.get("source")))) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> signal = w.get("signal") instanceof Map
          ? (Map<String, Object>) w.get("signal")
          : Map.of();
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("knowledgeId", w.get("knowledgeId"));
      row.put("title", w.get("title"));
      row.put("red", toInt(signal.get("red"), 0));
      row.put("yellow", toInt(signal.get("yellow"), 0));
      row.put("star", toInt(signal.get("star"), 0));
      row.put("summary", signal.get("summary"));
      syncKnowledges.add(row);
      if (syncKnowledges.size() >= 8) {
        break;
      }
    }
    overview.put("syncKnowledges", syncKnowledges);

    int specialWrongBook = 0;
    int specialTotalWrong = 0;
    int specialTotalAnswered = 0;
    double specialWrongRate = 0;
    String advice = null;
    if (diagnosisSpecial != null) {
      specialWrongBook = toInt(diagnosisSpecial.get("wrongBookCount"), 0);
      specialTotalWrong = toInt(diagnosisSpecial.get("totalWrong"), 0);
      specialTotalAnswered = toInt(diagnosisSpecial.get("totalAnswered"), 0);
      specialWrongRate = toDouble(diagnosisSpecial.get("wrongRate"), 0);
      advice = diagnosisSpecial.get("advice") == null
          ? null
          : String.valueOf(diagnosisSpecial.get("advice"));
    }
    Map<String, Object> special = new LinkedHashMap<>();
    special.put("wrongBookCount", specialWrongBook);
    special.put("totalWrong", specialTotalWrong);
    special.put("totalAnswered", specialTotalAnswered);
    special.put("wrongRate", specialWrongRate);
    if (advice != null && !advice.isBlank()) {
      special.put("advice", advice);
    }
    overview.put("special", special);

    // 题型错误率
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> typeStats = diagnosisSpecial != null
        && diagnosisSpecial.get("_typeStats") instanceof List
        ? (List<Map<String, Object>>) diagnosisSpecial.get("_typeStats")
        : List.of();
    overview.put("byType", typeStats);

    // 未掌握知识点（专项）：薄弱/一般优先，带错误率
    List<Map<String, Object>> byKnowledge = new ArrayList<>();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> predictItems = diagnosisSpecial != null
        && diagnosisSpecial.get("_predictItems") instanceof List
        ? (List<Map<String, Object>>) diagnosisSpecial.get("_predictItems")
        : List.of();
    for (Map<String, Object> it : predictItems) {
      String level = String.valueOf(it.getOrDefault("level", ""));
      double wrongRate = toDouble(it.get("wrongRate"), 0);
      if ("掌握".equals(level) && wrongRate < 30) {
        continue;
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("knowledgeId", it.get("knowledgeId"));
      row.put("title", it.getOrDefault("knowledgeName", "考点"));
      row.put("wrongCount", it.get("wrongCount"));
      row.put("totalCount", it.get("totalCount"));
      row.put("wrongRate", it.get("wrongRate"));
      row.put("level", level);
      byKnowledge.add(row);
      if (byKnowledge.size() >= 8) {
        break;
      }
    }
    overview.put("byKnowledge", byKnowledge);
    special.put("weakKnowledgeCount", byKnowledge.size());

    // 近期错题题干（直观看出错了哪些题）
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> wrongList = diagnosisSpecial != null
        && diagnosisSpecial.get("_wrongList") instanceof List
        ? (List<Map<String, Object>>) diagnosisSpecial.get("_wrongList")
        : List.of();
    Map<Integer, String> knowledgeTitleCache = new HashMap<>();
    for (Map<String, Object> it : predictItems) {
      Object kid = it.get("knowledgeId");
      if (kid instanceof Number) {
        knowledgeTitleCache.put(((Number) kid).intValue(),
            String.valueOf(it.getOrDefault("knowledgeName", "")));
      }
    }
    List<Map<String, Object>> recentWrongs = new ArrayList<>();
    for (Map<String, Object> q : wrongList) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("questionId", q.get("questionId"));
      String type = q.get("type") == null ? "" : String.valueOf(q.get("type"));
      row.put("type", type);
      row.put("typeName", typeLabel(type));
      Object kid = q.get("knowledgeId");
      row.put("knowledgeId", kid);
      String kname = "";
      if (kid instanceof Number) {
        kname = knowledgeTitleCache.getOrDefault(((Number) kid).intValue(), "");
        if (kname.isBlank()) {
          Map<Long, String> titles = loadKnowledgeTitles(List.of(((Number) kid).longValue()));
          kname = titles.getOrDefault(((Number) kid).longValue(), "");
          if (!kname.isBlank()) {
            knowledgeTitleCache.put(((Number) kid).intValue(), kname);
          }
        }
      }
      row.put("knowledgeName", kname);
      row.put("stem", truncateStem(q.get("stem")));
      recentWrongs.add(row);
      if (recentWrongs.size() >= 8) {
        break;
      }
    }
    overview.put("recentWrongs", recentWrongs);

    // 优先强化摘要（Hub 文案，不增入口）
    Map<String, Object> priority = new LinkedHashMap<>();
    List<Map<String, Object>> topKnowledges = byKnowledge.stream().limit(3).map(k -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("title", k.get("title"));
      row.put("wrongRate", k.get("wrongRate"));
      row.put("level", k.get("level"));
      row.put("knowledgeId", k.get("knowledgeId"));
      return row;
    }).collect(java.util.stream.Collectors.toList());
    priority.put("knowledges", topKnowledges);
    Map<String, Object> weakType = null;
    double maxTypeRate = -1;
    for (Map<String, Object> t : typeStats) {
      double r = toDouble(t.get("wrongRate"), 0);
      if (r > maxTypeRate) {
        maxTypeRate = r;
        weakType = t;
      }
    }
    if (weakType != null) {
      priority.put("type", weakType);
    }
    StringBuilder hint = new StringBuilder();
    if (!topKnowledges.isEmpty()) {
      hint.append(String.valueOf(topKnowledges.get(0).get("title")));
    }
    if (weakType != null && weakType.get("typeName") != null) {
      if (hint.length() > 0) {
        hint.append(" · ");
      }
      hint.append(weakType.get("typeName"));
    }
    priority.put("hint", hint.length() == 0 ? "按错题同型强化" : "优先：" + hint);
    overview.put("priority", priority);

    int totalCards = syncRed + syncYellow;
    boolean hasWrong = totalCards > 0 || specialWrongBook > 0 || specialTotalWrong > 0
        || !byKnowledge.isEmpty() || !recentWrongs.isEmpty() || !syncKnowledges.isEmpty();
    overview.put("hasWrong", hasWrong);
    overview.put("totalHint", hasWrong
        ? "练习将按你的错题推送同考点、同题型题目"
        : "暂无错题统计");
    return overview;
  }

  private static String typeLabel(String type) {
    if (type == null) {
      return "题目";
    }
    return switch (type.trim().toLowerCase()) {
      case "single", "单选" -> "单选";
      case "multi", "多选" -> "多选";
      case "judge", "判断" -> "判断";
      default -> "题目";
    };
  }

  private static String truncateStem(Object stemObj) {
    if (stemObj == null) {
      return "（无题干）";
    }
    String stem = String.valueOf(stemObj)
        .replaceAll("(?is)<br\\s*/?>", " ")
        .replaceAll("(?is)</p\\s*>", " ")
        .replaceAll("(?is)<[^>]+>", "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replaceAll("\\s+", " ")
        .trim();
    if (stem.isEmpty()) {
      return "（无题干）";
    }
    return stem.length() > 48 ? stem.substring(0, 48) + "…" : stem;
  }

  /**
   * Hub 不再调 overview（会物化今日任务）/ progress（全量答题扫描）。
   * 仅读已有计划主表 + 错题卡，首屏快；今日进度提示改引导去「同步练习」。
   */
  private List<AiReviewWrongItemEntity> listActiveWrongItems(long planId) {
    return wrongItemMapper.selectList(
        new LambdaQueryWrapper<AiReviewWrongItemEntity>()
            .eq(AiReviewWrongItemEntity::getPlanId, planId)
            .and(w -> w.isNull(AiReviewWrongItemEntity::getRemoved)
                .or()
                .ne(AiReviewWrongItemEntity::getRemoved, 1))
            .in(AiReviewWrongItemEntity::getCardLevel, List.of("red", "yellow")));
  }

  private Map<String, Object> buildSyncDiagnosisFromPlan(
      Long examId, AiReviewPlanEntity plan, List<AiReviewWrongItemEntity> wrongRows) {
    Map<String, Object> card = new LinkedHashMap<>();
    card.put("examId", String.valueOf(examId));
    int stage = plan.getCurrentStage() == null ? 1 : plan.getCurrentStage();
    card.put("stage", stage);
    card.put("stageName", stageName(stage));
    card.put("dayIndex", plan.getCurrentDayIndex());
    card.put("totalDays", plan.getTotalDays());
    card.put("examDate", plan.getExamDate() != null ? plan.getExamDate().toString() : null);
    card.put("estimatedScore", null);
    card.put("wrongStats", wrongStatsOf(wrongRows));
    card.put("todayHint", "详细今日任务请点下方「同步练习」");
    return card;
  }

  private static Map<String, Object> wrongStatsOf(List<AiReviewWrongItemEntity> items) {
    int red = 0;
    int yellow = 0;
    int star = 0;
    for (AiReviewWrongItemEntity item : items) {
      if ("red".equals(item.getCardLevel())) {
        red++;
        if (item.getStar() != null && item.getStar() == 1) {
          star++;
        }
      } else if ("yellow".equals(item.getCardLevel())) {
        yellow++;
      }
    }
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("red", red);
    view.put("yellow", yellow);
    view.put("star", star);
    return view;
  }

  private static String stageName(int stage) {
    return switch (stage) {
      case 1 -> "地毯式扫荡";
      case 2 -> "定向爆破";
      default -> "仿真模考";
    };
  }

  private List<Map<String, Object>> buildSyncWeakItemsFromWrong(
      List<AiReviewWrongItemEntity> items, Long classId) {
    Map<Long, int[]> byKid = new HashMap<>(); // [red, yellow, star]
    for (AiReviewWrongItemEntity item : items) {
      if (item.getKnowledgeId() == null) {
        continue;
      }
      int[] c = byKid.computeIfAbsent(item.getKnowledgeId(), k -> new int[3]);
      if ("red".equals(item.getCardLevel())) {
        c[0]++;
        if (item.getStar() != null && item.getStar() == 1) {
          c[2]++;
        }
      } else {
        c[1]++;
      }
    }
    if (byKid.isEmpty()) {
      return List.of();
    }

    Map<Long, String> titles = loadKnowledgeTitles(byKid.keySet());
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map.Entry<Long, int[]> e : byKid.entrySet()) {
      int red = e.getValue()[0];
      int yellow = e.getValue()[1];
      int star = e.getValue()[2];
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("knowledgeId", e.getKey());
      row.put("title", titles.getOrDefault(e.getKey(), "知识点 " + e.getKey()));
      row.put("source", "sync");
      Map<String, Object> signal = new LinkedHashMap<>();
      signal.put("red", red);
      signal.put("yellow", yellow);
      signal.put("star", star);
      signal.put("summary", signalSummary(red, yellow, star));
      row.put("signal", signal);
      row.put("action", specialAction(classId, e.getKey()));
      row.put("_score", red * 100 + star * 20 + yellow);
      rows.add(row);
    }
    rows.sort(Comparator.comparingInt((Map<String, Object> m) -> toInt(m.get("_score"), 0)).reversed());
    rows.forEach(m -> m.remove("_score"));
    return rows;
  }

  private List<Map<String, Object>> buildSpecialWeakItems(
      Map<String, Object> predict, Long classId) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = predict.get("items") instanceof List
        ? (List<Map<String, Object>>) predict.get("items")
        : List.of();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> it : items) {
      double wrongRate = toDouble(it.get("wrongRate"), 0);
      String level = String.valueOf(it.getOrDefault("level", ""));
      if (!"薄弱".equals(level) && wrongRate < SPECIAL_WEAK_RATE) {
        continue;
      }
      Object kid = it.get("knowledgeId");
      if (kid == null) {
        continue;
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("knowledgeId", kid);
      row.put("title", it.getOrDefault("knowledgeName", "考点"));
      row.put("source", "special");
      Map<String, Object> signal = new LinkedHashMap<>();
      signal.put("level", level);
      signal.put("wrongRate", wrongRate);
      signal.put("wrongCount", it.get("wrongCount"));
      signal.put("totalCount", it.get("totalCount"));
      signal.put("summary", level + " · 错 " + it.getOrDefault("wrongCount", 0)
          + "/" + it.getOrDefault("totalCount", 0) + " · " + wrongRate + "%");
      row.put("signal", signal);
      row.put("action", specialAction(classId, kid));
      row.put("_score", (int) Math.round(wrongRate));
      rows.add(row);
    }
    rows.sort(Comparator.comparingInt((Map<String, Object> m) -> toInt(m.get("_score"), 0)).reversed());
    rows.forEach(m -> m.remove("_score"));
    return rows;
  }

  private Map<String, Object> buildSpecialDiagnosis(
      Map<String, Object> predict, Map<String, Object> wrongBook) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = predict.get("items") instanceof List
        ? (List<Map<String, Object>>) predict.get("items")
        : List.of();
    List<Map<String, Object>> weakTop = new ArrayList<>();
    for (Map<String, Object> it : items) {
      if (weakTop.size() >= 5) {
        break;
      }
      String level = String.valueOf(it.getOrDefault("level", ""));
      double wrongRate = toDouble(it.get("wrongRate"), 0);
      if (!"薄弱".equals(level) && wrongRate < SPECIAL_WEAK_RATE) {
        continue;
      }
      Map<String, Object> w = new LinkedHashMap<>();
      w.put("knowledgeId", it.get("knowledgeId"));
      w.put("title", it.get("knowledgeName"));
      w.put("wrongRate", it.get("wrongRate"));
      w.put("wrongCount", it.get("wrongCount"));
      w.put("level", level);
      weakTop.add(w);
    }
    Map<String, Object> card = new LinkedHashMap<>();
    card.put("wrongBookCount", wrongBook == null ? 0 : wrongBook.getOrDefault("total", 0));
    card.put("weakKnowledges", weakTop);
    @SuppressWarnings("unchecked")
    Map<String, Object> summary = predict.get("summary") instanceof Map
        ? (Map<String, Object>) predict.get("summary")
        : Map.of();
    card.put("totalAnswered", summary.getOrDefault("totalAnswered", 0));
    card.put("totalWrong", summary.getOrDefault("totalWrong", 0));
    card.put("wrongRate", summary.getOrDefault("wrongRate", 0));
    card.put("advice", summary.getOrDefault("advice", null));
    return card;
  }

  /**
   * 弱项去练：按错题推送同考点+同题型题（通道 A）；前端调 similar-wrong，答错即时解析。
   */
  private static Map<String, Object> specialAction(Long classId, Object knowledgeId) {
    Map<String, Object> action = new LinkedHashMap<>();
    action.put("channel", "special");
    action.put("mode", "similar-wrong");
    action.put("classId", classId);
    action.put("knowledgeId", knowledgeId);
    action.put("total", 20);
    return action;
  }

  private static String signalSummary(int red, int yellow, int star) {
    StringBuilder sb = new StringBuilder();
    if (red > 0) {
      sb.append("红卡 ").append(red);
    }
    if (star > 0) {
      if (sb.length() > 0) {
        sb.append(" · ");
      }
      sb.append("星标 ").append(star);
    }
    if (yellow > 0) {
      if (sb.length() > 0) {
        sb.append(" · ");
      }
      sb.append("黄卡 ").append(yellow);
    }
    return sb.length() == 0 ? "待强化" : sb.toString();
  }

  private Map<Long, String> loadKnowledgeTitles(Iterable<Long> ids) {
    List<Long> list = new ArrayList<>();
    for (Long id : ids) {
      if (id != null) {
        list.add(id);
      }
    }
    if (list.isEmpty()) {
      return Map.of();
    }
    List<ExamKnowledgeStructureEntity> rows = knowledgeStructureMapper.selectBatchIds(
        list.stream().map(Long::intValue).toList());
    Map<Long, String> out = new HashMap<>();
    for (ExamKnowledgeStructureEntity row : rows) {
      if (row.getId() != null) {
        out.put(row.getId().longValue(), row.getTitle());
      }
    }
    return out;
  }

  private static Comparator<Map<String, Object>> weakComparator() {
    return (a, b) -> {
      int sa = "sync".equals(a.get("source")) ? 0 : 1;
      int sb = "sync".equals(b.get("source")) ? 0 : 1;
      if (sa != sb) {
        return Integer.compare(sa, sb);
      }
      return 0;
    };
  }

  private static Long toLong(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static int toInt(Object v, int def) {
    if (v == null) {
      return def;
    }
    if (v instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(v));
    } catch (NumberFormatException e) {
      return def;
    }
  }

  private static double toDouble(Object v, double def) {
    if (v == null) {
      return def;
    }
    if (v instanceof Number n) {
      return n.doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(v));
    } catch (NumberFormatException e) {
      return def;
    }
  }
}
