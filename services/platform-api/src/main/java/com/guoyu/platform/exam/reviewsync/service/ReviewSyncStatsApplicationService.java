package com.guoyu.platform.exam.reviewsync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.reviewsync.dto.ReviewKnowledgeNode;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewMockPaperEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewWrongItemEntity;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewAnswerMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewMockPaperMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewWrongItemMapper;
import com.guoyu.platform.exam.reviewsync.mapper.ReviewSyncStatsMapper;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 同步学统计用例编排（《AI同步学统计功能设计方案》v1.1 §5；单库化改造后全部读主库 gjjddbpro）。
 *
 * <p>全部只读；tenant_id 由多租户插件自动追加。考试计划标题读主库 train_exam_list；
 * 知识树标题读主库 exam_knowledge_structure（失败降级为显示 id，统计主链路不挂）。
 */
@Slf4j
@Service
public class ReviewSyncStatsApplicationService {

  /** 日期范围跨度上限（防全表扫，方案 §5.5） */
  private static final int MAX_RANGE_DAYS = 92;
  /** 学员明细导出行数上限 */
  public static final int EXPORT_MAX_ROWS = 5000;
  /** 学员详情错题清单上限 */
  private static final int DETAIL_WRONG_LIMIT = 200;

  private final ReviewSyncStatsMapper statsMapper;
  private final AiReviewPlanMapper planMapper;
  private final AiReviewWrongItemMapper wrongMapper;
  private final AiReviewMockPaperMapper mockPaperMapper;
  private final AiReviewAnswerMapper answerMapper;
  private final ExamExamDetailMapper examDetailMapper;
  private final PlatformUserMapper platformUserMapper;
  private final TrainExamListMapper trainExamListMapper;
  private final ReviewQuestionQueryService reviewQuery;
  private final ObjectMapper objectMapper;

  public ReviewSyncStatsApplicationService(ReviewSyncStatsMapper statsMapper,
      AiReviewPlanMapper planMapper,
      AiReviewWrongItemMapper wrongMapper,
      AiReviewMockPaperMapper mockPaperMapper,
      AiReviewAnswerMapper answerMapper,
      ExamExamDetailMapper examDetailMapper,
      PlatformUserMapper platformUserMapper,
      TrainExamListMapper trainExamListMapper,
      ReviewQuestionQueryService reviewQuery,
      ObjectMapper objectMapper) {
    this.statsMapper = statsMapper;
    this.planMapper = planMapper;
    this.wrongMapper = wrongMapper;
    this.mockPaperMapper = mockPaperMapper;
    this.answerMapper = answerMapper;
    this.examDetailMapper = examDetailMapper;
    this.platformUserMapper = platformUserMapper;
    this.trainExamListMapper = trainExamListMapper;
    this.reviewQuery = reviewQuery;
    this.objectMapper = objectMapper;
  }

  // ==================================================================
  // #1 概览卡片
  // ==================================================================

  public Map<String, Object> overview(LocalDate from, LocalDate to, Long examId) {
    LocalDate[] range = normalizeRange(from, to);
    Map<String, Object> totals = nullToEmpty(statsMapper.answerTotals(range[0], range[1], examId));
    LocalDate today = LocalDate.now();
    Map<String, Object> todayTotals = nullToEmpty(statsMapper.answerTotals(today, today, examId));

    LambdaQueryWrapper<AiReviewPlanEntity> planWrapper = new LambdaQueryWrapper<AiReviewPlanEntity>();
    if (examId != null) {
      planWrapper.eq(AiReviewPlanEntity::getExamId, examId);
    }
    long planCount = planMapper.selectCount(planWrapper);
    // 去重学员数：selectObjs + groupBy 后取 size（selectCount 配 groupBy 语义不可靠）
    LambdaQueryWrapper<AiReviewPlanEntity> distinctWrapper = new LambdaQueryWrapper<AiReviewPlanEntity>()
        .select(AiReviewPlanEntity::getPlatformUserId)
        .groupBy(AiReviewPlanEntity::getPlatformUserId);
    if (examId != null) {
      distinctWrapper.eq(AiReviewPlanEntity::getExamId, examId);
    }
    long studentCount = planMapper.selectObjs(distinctWrapper).size();

    Map<String, Object> wrong = wrongDistribution(examId, null);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("planCount", planCount);
    result.put("studentCount", studentCount);
    result.put("todayActiveUsers", asLong(todayTotals.get("activeUsers")));
    result.put("todayAnswers", asLong(todayTotals.get("answers")));
    result.put("totalAnswers", asLong(totals.get("answers")));
    result.put("overallAccuracy", accuracy(asLong(totals.get("corrects")), asLong(totals.get("answers"))));
    result.put("wrong", wrong);
    return result;
  }

  // ==================================================================
  // #2 每日使用趋势
  // ==================================================================

  public Map<String, Object> dailyTrend(LocalDate from, LocalDate to, Long examId) {
    LocalDate[] range = normalizeRange(from, to);
    List<Map<String, Object>> answers = nullToEmptyList(
        statsMapper.dailyAnswerTrend(range[0], range[1], examId, null));
    Map<String, Map<String, Object>> completionByDay = new HashMap<>();
    for (Map<String, Object> row : nullToEmptyList(statsMapper.dailyDayCompletion(range[0], range[1], examId))) {
      completionByDay.put(String.valueOf(row.get("day")), row);
    }
    List<Map<String, Object>> series = new ArrayList<>();
    for (Map<String, Object> row : answers) {
      String day = String.valueOf(row.get("day"));
      long answerCnt = asLong(row.get("answers"));
      Map<String, Object> point = new LinkedHashMap<>();
      point.put("day", day);
      point.put("activeUsers", asLong(row.get("activeUsers")));
      point.put("answers", answerCnt);
      point.put("accuracy", accuracy(asLong(row.get("corrects")), answerCnt));
      Map<String, Object> completion = completionByDay.get(day);
      long totalDays = completion != null ? asLong(completion.get("totalDays")) : 0;
      long doneDays = completion != null ? asLong(completion.get("doneDays")) : 0;
      point.put("completionRate", accuracy(doneDays, totalDays));
      series.add(point);
    }
    return Map.of("series", series);
  }

  // ==================================================================
  // #3 错题：牌级分布 + 新增/消灭趋势（近似口径）
  // ==================================================================

  public Map<String, Object> wrongTrend(LocalDate from, LocalDate to, Long examId) {
    LocalDate[] range = normalizeRange(from, to);
    Map<String, Object> distribution = wrongDistribution(examId, null);
    List<Map<String, Object>> dailyNew = nullToEmptyList(statsMapper.wrongDailyNew(range[0], range[1], examId));
    List<Map<String, Object>> dailyRemoved = nullToEmptyList(
        statsMapper.wrongDailyRemoved(range[0], range[1], examId));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("distribution", distribution);
    result.put("dailyNew", dailyNew);
    result.put("dailyRemoved", dailyRemoved);
    return result;
  }

  /** 牌级分布快照：red/yellow=在册，green=已移出（removed=1），star=在册加星 */
  private Map<String, Object> wrongDistribution(Long examId, Long platformUserId) {
    long red = 0, yellow = 0, green = 0, star = 0;
    for (Map<String, Object> row : nullToEmptyList(statsMapper.wrongSnapshot(examId, platformUserId))) {
      long cnt = asLong(row.get("cnt"));
      long removed = asLong(row.get("removed"));
      String level = String.valueOf(row.get("cardLevel"));
      if (removed == 1) {
        green += cnt;
      } else if ("red".equals(level)) {
        red += cnt;
      } else if ("yellow".equals(level)) {
        yellow += cnt;
      }
      if (removed == 0) {
        star += asLong(row.get("starCnt"));
      }
    }
    Map<String, Object> distribution = new LinkedHashMap<>();
    distribution.put("red", red);
    distribution.put("yellow", yellow);
    distribution.put("green", green);
    distribution.put("star", star);
    distribution.put("active", red + yellow);
    return distribution;
  }

  // ==================================================================
  // #4 知识点掌握度（L1 考核项目）
  // ==================================================================

  public Map<String, Object> knowledgeMastery(Long examId, Long platformUserId) {
    if (examId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "examId 不能为空");
    }
    List<Map<String, Object>> agg = nullToEmptyList(statsMapper.knowledgeAgg(examId, platformUserId));
    // 知识树：计划快照 course_id → PHP 全树（失败降级：L3 平铺、标题显示 id）
    Map<Long, ReviewKnowledgeNode> treeById = loadKnowledgeTreeSafe(examId);
    Map<Long, long[]> l1Agg = new LinkedHashMap<>(); // l1Id -> [answers, corrects, answeredL3, totalL3]
    Map<Long, Integer> l1TotalL3 = new HashMap<>();
    for (ReviewKnowledgeNode node : treeById.values()) {
      if (node.level() != null && node.level() == 3) {
        Long l1 = findAncestorAtLevel(treeById, node, 1);
        if (l1 != null) {
          l1TotalL3.merge(l1, 1, Integer::sum);
        }
      }
    }
    List<Map<String, Object>> l3Detail = new ArrayList<>();
    for (Map<String, Object> row : agg) {
      Long knowledgeId = asLongObj(row.get("knowledgeId"));
      if (knowledgeId == null) {
        continue;
      }
      long answers = asLong(row.get("answers"));
      long corrects = asLong(row.get("corrects"));
      ReviewKnowledgeNode node = treeById.get(knowledgeId);
      Long l1 = node != null ? findAncestorAtLevel(treeById, node, 1) : null;
      long[] bucket = l1Agg.computeIfAbsent(l1 != null ? l1 : -1L, k -> new long[4]);
      bucket[0] += answers;
      bucket[1] += corrects;
      bucket[2] += 1; // 已作答 L3 数
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("knowledgeId", knowledgeId);
      item.put("title", node != null && node.title() != null ? node.title() : "知识点#" + knowledgeId);
      item.put("answers", answers);
      item.put("accuracy", accuracy(corrects, answers));
      l3Detail.add(item);
    }
    l3Detail.sort((a, b) -> Long.compare(asLong(b.get("answers")), asLong(a.get("answers"))));

    List<Map<String, Object>> l1List = new ArrayList<>();
    for (Map.Entry<Long, long[]> entry : l1Agg.entrySet()) {
      long[] v = entry.getValue();
      ReviewKnowledgeNode l1Node = treeById.get(entry.getKey());
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("knowledgeId", entry.getKey() == -1L ? null : entry.getKey());
      item.put("title", l1Node != null && l1Node.title() != null ? l1Node.title()
          : (entry.getKey() == -1L ? "未归类" : "知识点#" + entry.getKey()));
      item.put("answers", v[0]);
      item.put("accuracy", accuracy(v[1], v[0]));
      int totalL3 = l1TotalL3.getOrDefault(entry.getKey(), 0);
      item.put("coverage", accuracy(v[2], totalL3));
      item.put("answeredL3", v[2]);
      item.put("totalL3", totalL3);
      l1List.add(item);
    }
    l1List.sort((a, b) -> Long.compare(asLong(b.get("answers")), asLong(a.get("answers"))));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("l1", l1List);
    result.put("l3", l3Detail);
    return result;
  }

  /** 沿 parent 链向上找 level=targetLevel 的祖先 id；找不到返回 null（防循环，最多 8 层） */
  private Long findAncestorAtLevel(Map<Long, ReviewKnowledgeNode> treeById, ReviewKnowledgeNode node, int targetLevel) {
    ReviewKnowledgeNode current = node;
    for (int i = 0; i < 8 && current != null; i++) {
      if (current.level() != null && current.level() == targetLevel) {
        return current.id();
      }
      Long parentId = current.parentId();
      current = parentId != null ? treeById.get(parentId) : null;
    }
    return null;
  }

  /** 主库知识树加载（失败降级空 Map，统计主链路不挂 —— 方案 §5.5） */
  private Map<Long, ReviewKnowledgeNode> loadKnowledgeTreeSafe(Long examId) {
    Map<Long, ReviewKnowledgeNode> treeById = new HashMap<>();
    try {
      AiReviewPlanEntity plan = planMapper.selectOne(new LambdaQueryWrapper<AiReviewPlanEntity>()
          .eq(AiReviewPlanEntity::getExamId, examId)
          .isNotNull(AiReviewPlanEntity::getCourseId)
          .last("LIMIT 1"));
      if (plan == null || plan.getCourseId() == null) {
        return treeById;
      }
      for (ReviewKnowledgeNode node : reviewQuery.loadKnowledgeTree(plan.getCourseId())) {
        treeById.put(node.id(), node);
      }
    } catch (Exception e) {
      log.warn("[AI复习统计] 知识树加载失败，降级为显示知识点 id：examId={} error={}", examId, e.getMessage());
    }
    return treeById;
  }

  // ==================================================================
  // #5 学员明细分页 / #7 导出行
  // ==================================================================

  public PageResponse<Map<String, Object>> students(long page, long pageSize, Long examId, String keyword) {
    List<Long> keywordUserIds = findUserIdsByKeyword(keyword);
    if (keyword != null && !keyword.isBlank() && keywordUserIds.isEmpty()) {
      return new PageResponse<>(List.of(), page, pageSize, 0);
    }
    LambdaQueryWrapper<AiReviewPlanEntity> wrapper = new LambdaQueryWrapper<>();
    if (examId != null) {
      wrapper.eq(AiReviewPlanEntity::getExamId, examId);
    }
    if (keywordUserIds != null) {
      wrapper.in(AiReviewPlanEntity::getPlatformUserId, keywordUserIds);
    }
    wrapper.orderByDesc(AiReviewPlanEntity::getLastActiveDate);
    Page<AiReviewPlanEntity> result = planMapper.selectPage(new Page<>(page, pageSize), wrapper);
    List<Map<String, Object>> rows = assembleStudentRows(result.getRecords());
    return new PageResponse<>(rows, result.getCurrent(), result.getSize(), result.getTotal());
  }

  /** 导出行（复用明细细则，上限 EXPORT_MAX_ROWS） */
  public List<Map<String, Object>> studentExportRows(Long examId, String keyword) {
    List<Long> keywordUserIds = findUserIdsByKeyword(keyword);
    if (keyword != null && !keyword.isBlank() && keywordUserIds.isEmpty()) {
      return List.of();
    }
    LambdaQueryWrapper<AiReviewPlanEntity> wrapper = new LambdaQueryWrapper<>();
    if (examId != null) {
      wrapper.eq(AiReviewPlanEntity::getExamId, examId);
    }
    if (keywordUserIds != null) {
      wrapper.in(AiReviewPlanEntity::getPlatformUserId, keywordUserIds);
    }
    wrapper.orderByDesc(AiReviewPlanEntity::getLastActiveDate)
        .last("LIMIT " + (EXPORT_MAX_ROWS + 1));
    List<AiReviewPlanEntity> plans = planMapper.selectList(wrapper);
    if (plans.size() > EXPORT_MAX_ROWS) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "导出行数超过 " + EXPORT_MAX_ROWS + "，请先按考试计划或学员筛选缩小范围");
    }
    return assembleStudentRows(plans);
  }

  /** 组装学员明细行：plan ⨝ answer 聚合 ⨝ wrong 聚合 ⨝ platform_user ⨝ train_exam_list */
  private List<Map<String, Object>> assembleStudentRows(List<AiReviewPlanEntity> plans) {
    if (plans == null || plans.isEmpty()) {
      return new ArrayList<>();
    }
    List<Long> planIds = plans.stream().map(AiReviewPlanEntity::getId).toList();
    Map<Long, Map<String, Object>> answerByPlan = indexByPlanId(statsMapper.planAnswerAgg(planIds));
    Map<Long, Map<String, Object>> wrongByPlan = indexByPlanId(statsMapper.planWrongAgg(planIds));

    List<Long> userIds = plans.stream().map(AiReviewPlanEntity::getPlatformUserId).distinct().toList();
    Map<Long, PlatformUserEntity> userById = new HashMap<>();
    for (PlatformUserEntity user : platformUserMapper.selectList(
        new LambdaQueryWrapper<PlatformUserEntity>().in(PlatformUserEntity::getId, userIds))) {
      if (user != null && user.getId() != null) {
        userById.put(user.getId(), user);
      }
    }
    List<Long> examIds = plans.stream().map(AiReviewPlanEntity::getExamId).distinct().toList();
    Map<Long, String> examTitleById = new HashMap<>();
    for (TrainExamListEntity exam : trainExamListMapper.selectList(
        new LambdaQueryWrapper<TrainExamListEntity>().in(TrainExamListEntity::getId, examIds))) {
      if (exam != null && exam.getId() != null) {
        examTitleById.put(exam.getId(), exam.getTitle());
      }
    }

    List<Map<String, Object>> rows = new ArrayList<>(plans.size());
    for (AiReviewPlanEntity plan : plans) {
      Map<String, Object> answers = answerByPlan.get(plan.getId());
      Map<String, Object> wrongs = wrongByPlan.get(plan.getId());
      PlatformUserEntity user = userById.get(plan.getPlatformUserId());
      long answerCnt = answers != null ? asLong(answers.get("answers")) : 0;
      long correctCnt = answers != null ? asLong(answers.get("corrects")) : 0;

      Map<String, Object> row = new LinkedHashMap<>();
      row.put("planId", String.valueOf(plan.getId())); // 雪花 id 超 JS 安全整数，字符串下发（AGENTS.md 坑 #24）
      row.put("platformUserId", plan.getPlatformUserId());
      row.put("realname", user != null && user.getRealname() != null ? user.getRealname() : "学员#" + plan.getPlatformUserId());
      row.put("phone", user != null ? user.getPhone() : null);
      row.put("idcard", user != null ? user.getIdcard() : null);
      row.put("examId", plan.getExamId());
      row.put("examTitle", examTitleById.getOrDefault(plan.getExamId(), "计划#" + plan.getExamId()));
      row.put("stage", plan.getCurrentStage());
      row.put("dayIndex", plan.getCurrentDayIndex());
      row.put("totalDays", plan.getTotalDays());
      row.put("status", plan.getStatus());
      row.put("answers", answerCnt);
      row.put("newAnswers", answers != null ? asLong(answers.get("newAnswers")) : 0);
      row.put("reviewAnswers", answers != null ? asLong(answers.get("reviewAnswers")) : 0);
      row.put("mockAnswers", answers != null ? asLong(answers.get("mockAnswers")) : 0);
      row.put("accuracy", accuracy(correctCnt, answerCnt));
      row.put("redCnt", wrongs != null ? asLong(wrongs.get("redCnt")) : 0);
      row.put("yellowCnt", wrongs != null ? asLong(wrongs.get("yellowCnt")) : 0);
      row.put("greenCnt", wrongs != null ? asLong(wrongs.get("greenCnt")) : 0);
      row.put("starCnt", wrongs != null ? asLong(wrongs.get("starCnt")) : 0);
      row.put("lastActiveDate", plan.getLastActiveDate() != null ? plan.getLastActiveDate().toString() : null);
      rows.add(row);
    }
    return rows;
  }

  private Map<Long, Map<String, Object>> indexByPlanId(List<Map<String, Object>> agg) {
    Map<Long, Map<String, Object>> map = new HashMap<>();
    for (Map<String, Object> row : nullToEmptyList(agg)) {
      Long planId = asLongObj(row.get("planId"));
      if (planId != null) {
        map.put(planId, row);
      }
    }
    return map;
  }

  /** 关键词（姓名/手机号）→ platform_user id 列表；null 关键词返回 null（不过滤） */
  private List<Long> findUserIdsByKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    List<PlatformUserEntity> users = platformUserMapper.selectList(new LambdaQueryWrapper<PlatformUserEntity>()
        .and(w -> w.like(PlatformUserEntity::getRealname, keyword)
            .or().like(PlatformUserEntity::getPhone, keyword))
        .last("LIMIT 5000"));
    return users.stream().map(PlatformUserEntity::getId).filter(java.util.Objects::nonNull).toList();
  }

  // ==================================================================
  // #6 学员详情
  // ==================================================================

  public Map<String, Object> studentDetail(long planId) {
    AiReviewPlanEntity plan = planMapper.selectById(planId);
    if (plan == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "复习计划不存在");
    }
    PlatformUserEntity user = platformUserMapper.selectById(plan.getPlatformUserId());
    TrainExamListEntity exam = trainExamListMapper.selectById(plan.getExamId());

    // 个人每日趋势（计划起止范围内）
    LocalDate from = plan.getStartDate() != null ? plan.getStartDate() : LocalDate.now().minusDays(13);
    LocalDate to = LocalDate.now();
    List<Map<String, Object>> daily = nullToEmptyList(
        statsMapper.dailyAnswerTrend(from, to, plan.getExamId(), plan.getPlatformUserId()));
    List<Map<String, Object>> dailySeries = new ArrayList<>();
    for (Map<String, Object> row : daily) {
      Map<String, Object> point = new LinkedHashMap<>();
      point.put("day", String.valueOf(row.get("day")));
      point.put("answers", asLong(row.get("answers")));
      point.put("accuracy", accuracy(asLong(row.get("corrects")), asLong(row.get("answers"))));
      dailySeries.add(point);
    }

    // 错题清单（加星/红牌优先，按最近答错倒序）
    List<AiReviewWrongItemEntity> wrongItems = wrongMapper.selectList(
        new LambdaQueryWrapper<AiReviewWrongItemEntity>()
            .eq(AiReviewWrongItemEntity::getPlanId, plan.getId())
            .orderByDesc(AiReviewWrongItemEntity::getStar)
            .orderByAsc(AiReviewWrongItemEntity::getRemoved)
            .orderByDesc(AiReviewWrongItemEntity::getLastWrongTime)
            .last("LIMIT " + DETAIL_WRONG_LIMIT));

    // 题目完整信息（题型/题目/选项/正确答案）：单库化后 exam_exam_detail 即合并表，一次主库直查；
    // 缺失 id 降级为只显示题号（统计主链路不挂，方案 §5.5）
    List<Long> wrongQids = wrongItems.stream().map(AiReviewWrongItemEntity::getQuestionId).toList();
    Map<Long, QuestionView> detailByQid = loadQuestionViews(wrongQids);
    Map<Long, AiReviewAnswerEntity> latestAnswerByQid = latestAnswersByQuestion(plan.getId(), wrongQids);

    List<Map<String, Object>> wrongList = new ArrayList<>();
    for (AiReviewWrongItemEntity item : wrongItems) {
      QuestionView detail = detailByQid.get(item.getQuestionId());
      AiReviewAnswerEntity latest = latestAnswerByQid.get(item.getQuestionId());
      List<Map<String, Object>> options = detail != null ? parseOptionsForView(detail.answerJson()) : List.of();
      String correctAnswer = options.stream()
          .filter(o -> Boolean.TRUE.equals(o.get("correct")))
          .map(o -> String.valueOf(o.get("content")))
          .reduce((a, b) -> a + "、" + b)
          .orElse(null);

      Map<String, Object> row = new LinkedHashMap<>();
      row.put("questionId", item.getQuestionId());
      row.put("questionType", detail != null && detail.type() != null ? detail.type()
          : (latest != null ? latest.getQuestionType() : null));
      row.put("content", detail != null ? detail.content() : null);
      row.put("options", options);
      row.put("correctAnswer", correctAnswer);
      row.put("userAnswer", latest != null ? latest.getUserAnswer() : null);
      row.put("lastCorrect", latest != null && latest.getIsCorrect() != null && latest.getIsCorrect() == 1);
      row.put("cardLevel", item.getCardLevel());
      row.put("star", item.getStar());
      row.put("removed", item.getRemoved());
      row.put("wrongCount", item.getWrongCount());
      row.put("correctCount", item.getCorrectCount());
      row.put("lastWrongTime", item.getLastWrongTime() != null ? item.getLastWrongTime().toString() : null);
      wrongList.add(row);
    }

    // 模考成绩
    List<AiReviewMockPaperEntity> papers = mockPaperMapper.selectList(
        new LambdaQueryWrapper<AiReviewMockPaperEntity>()
            .eq(AiReviewMockPaperEntity::getPlanId, plan.getId())
            .eq(AiReviewMockPaperEntity::getStatus, "done")
            .orderByDesc(AiReviewMockPaperEntity::getId));
    List<Map<String, Object>> mockList = new ArrayList<>();
    for (AiReviewMockPaperEntity paper : papers) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("paperNo", paper.getPaperNo());
      row.put("score", paper.getScore());
      row.put("totalScore", paper.getTotalScore());
      row.put("passScore", paper.getPassScore());
      row.put("passed", paper.getScore() != null && paper.getPassScore() != null
          && paper.getScore().compareTo(paper.getPassScore()) >= 0);
      row.put("correctCount", paper.getCorrectCount());
      row.put("questionCount", paper.getQuestionCount());
      row.put("createdAt", paper.getCreatedAt() != null ? paper.getCreatedAt().toString() : null);
      mockList.add(row);
    }

    Map<String, Object> planView = new LinkedHashMap<>();
    planView.put("planId", String.valueOf(plan.getId()));
    planView.put("realname", user != null && user.getRealname() != null ? user.getRealname() : "学员#" + plan.getPlatformUserId());
    planView.put("phone", user != null ? user.getPhone() : null);
    planView.put("examId", plan.getExamId());
    planView.put("examTitle", exam != null && exam.getTitle() != null ? exam.getTitle() : "计划#" + plan.getExamId());
    planView.put("stage", plan.getCurrentStage());
    planView.put("dayIndex", plan.getCurrentDayIndex());
    planView.put("totalDays", plan.getTotalDays());
    planView.put("status", plan.getStatus());
    planView.put("targetQuestions", plan.getTargetQuestions());
    planView.put("startDate", plan.getStartDate() != null ? plan.getStartDate().toString() : null);
    planView.put("examDate", plan.getExamDate() != null ? plan.getExamDate().toString() : null);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("plan", planView);
    result.put("daily", dailySeries);
    result.put("knowledge", knowledgeMastery(plan.getExamId(), plan.getPlatformUserId()));
    result.put("wrongDistribution", wrongDistribution(plan.getExamId(), plan.getPlatformUserId()));
    result.put("wrongList", wrongList);
    result.put("mockPapers", mockList);
    return result;
  }

  // ==================================================================
  // 内部工具
  // ==================================================================

  /** 题目视图（统计展示用）：type/content/answerJson 三字段即可 */
  private record QuestionView(String type, String content, String answerJson) {
  }

  /**
   * 题目详情批量加载：单库化后 exam_exam_detail 即合并库唯一题库，一次主库直查（原"本地优先→回退 PHP"
   * 三重链已折叠）；查询异常整体降级只显示题号，统计主链路不挂。
   */
  private Map<Long, QuestionView> loadQuestionViews(List<Long> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, QuestionView> result = new HashMap<>();
    try {
      for (ExamExamDetailEntity entity : examDetailMapper.selectList(
          new LambdaQueryWrapper<ExamExamDetailEntity>().in(ExamExamDetailEntity::getId, questionIds))) {
        if (entity != null && entity.getId() != null) {
          result.put(entity.getId().longValue(),
              new QuestionView(entity.getType(), entity.getContent(), entity.getAnswer()));
        }
      }
    } catch (Exception e) {
      log.warn("[AI复习统计] 题库加载失败，缺失题号降级显示：error={}", e.getMessage());
    }
    return result;
  }

  /** 该计划内每题最近一次作答（含改错重答，按 id 倒序取首条） */
  private Map<Long, AiReviewAnswerEntity> latestAnswersByQuestion(long planId, List<Long> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return Map.of();
    }
    List<AiReviewAnswerEntity> answers = answerMapper.selectList(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getPlanId, planId)
        .in(AiReviewAnswerEntity::getQuestionId, questionIds)
        .orderByDesc(AiReviewAnswerEntity::getId));
    Map<Long, AiReviewAnswerEntity> latest = new HashMap<>();
    for (AiReviewAnswerEntity answer : answers) {
      if (answer != null && answer.getQuestionId() != null) {
        latest.putIfAbsent(answer.getQuestionId(), answer);
      }
    }
    return latest;
  }

  /** 选项 JSON 解析为 [{content, correct}]（与判分口径一致：isCorrect=true/"true"） */
  private List<Map<String, Object>> parseOptionsForView(String answerJson) {
    if (answerJson == null || answerJson.isBlank()) {
      return List.of();
    }
    try {
      List<Map<String, Object>> raw = objectMapper.readValue(answerJson, new TypeReference<>() {
      });
      List<Map<String, Object>> options = new ArrayList<>(raw.size());
      for (Map<String, Object> item : raw) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("content", item.get("content") != null ? String.valueOf(item.get("content")) : "");
        option.put("correct", Boolean.TRUE.equals(item.get("isCorrect"))
            || "true".equals(String.valueOf(item.get("isCorrect"))));
        options.add(option);
      }
      return options;
    } catch (Exception e) {
      log.warn("[AI复习统计] 选项 JSON 解析失败：{}", e.getMessage());
      return List.of();
    }
  }

  /** 日期范围归一化：默认近 14 天；跨度上限 MAX_RANGE_DAYS（方案 §5.5） */
  private LocalDate[] normalizeRange(LocalDate from, LocalDate to) {
    LocalDate end = to != null ? to : LocalDate.now();
    LocalDate start = from != null ? from : end.minusDays(13);
    if (start.isAfter(end)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "开始日期不能晚于结束日期");
    }
    if (java.time.temporal.ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "日期跨度不能超过 " + MAX_RANGE_DAYS + " 天");
    }
    return new LocalDate[]{start, end};
  }

  private static double accuracy(long corrects, long total) {
    if (total <= 0) {
      return 0;
    }
    return BigDecimal.valueOf(corrects * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }

  private static long asLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    return 0;
  }

  private static Long asLongObj(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    return null;
  }

  private static Map<String, Object> nullToEmpty(Map<String, Object> map) {
    return map != null ? map : new LinkedHashMap<>();
  }

  private static List<Map<String, Object>> nullToEmptyList(List<Map<String, Object>> list) {
    return list != null ? list : List.of();
  }
}
