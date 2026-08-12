package com.guoyu.platform.app.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.exam.entity.AppExamMockPaperEntity;
import com.guoyu.platform.app.exam.entity.AppExamUserAnswerLogEntity;
import com.guoyu.platform.app.exam.mapper.AppExamMockPaperMapper;
import com.guoyu.platform.app.exam.mapper.AppExamUserAnswerLogMapper;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeCourseEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeKnowledgeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeScopeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskQuestionReq;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeCourseMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeKnowledgeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeScopeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * App 端智能学习服务：AI 组卷 / 模拟考 / 考点预测 / 错题重练。
 *
 * <p>设计要点：
 * <ul>
 *   <li>组卷按"考点×题型×难度"分层抽样，保证覆盖重点考点</li>
 *   <li>错题重练从 exam_user_answer_log 中找历史错题</li>
 *   <li>考点预测按错题率排序给出薄弱考点</li>
 *   <li>交卷后写入 exam_user_answer_log，作为后续错题/学情数据源</li>
 * </ul>
 */
@Service
public class AppExamSmartService {

  /** 默认组卷参数 */
  private static final int DEFAULT_QUESTION_COUNT = 20;
  private static final int DEFAULT_TIME_LIMIT_MIN = 30;
  /** 错题重练默认题量 */
  private static final int WRONG_REPLAY_LIMIT = 20;

  private final ExamExamDetailMapper examDetailMapper;
  private final ExamKnowledgeStructureMapper knowledgeMapper;
  private final AppExamMockPaperMapper mockPaperMapper;
  private final AppExamUserAnswerLogMapper answerLogMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainClassPracticeScopeMapper practiceScopeMapper;
  private final TrainClassPracticeCourseMapper practiceCourseMapper;
  private final TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper;
  private final FollowTaskBankQuestionConverter bankQuestionConverter;

  public AppExamSmartService(ExamExamDetailMapper examDetailMapper,
                             ExamKnowledgeStructureMapper knowledgeMapper,
                             AppExamMockPaperMapper mockPaperMapper,
                             AppExamUserAnswerLogMapper answerLogMapper,
                             TrainPeopleListMapper peopleListMapper,
                             TrainClassPracticeScopeMapper practiceScopeMapper,
                             TrainClassPracticeCourseMapper practiceCourseMapper,
                             TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper,
                             FollowTaskBankQuestionConverter bankQuestionConverter) {
    this.examDetailMapper = examDetailMapper;
    this.knowledgeMapper = knowledgeMapper;
    this.mockPaperMapper = mockPaperMapper;
    this.answerLogMapper = answerLogMapper;
    this.peopleListMapper = peopleListMapper;
    this.practiceScopeMapper = practiceScopeMapper;
    this.practiceCourseMapper = practiceCourseMapper;
    this.practiceKnowledgeMapper = practiceKnowledgeMapper;
    this.bankQuestionConverter = bankQuestionConverter;
  }

  // ============================================================
  // 1. AI 组卷：按考点×难度智能抽题，生成一张模拟卷
  // ============================================================
  public Map<String, Object> generatePaper(Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = resolveExamTenantId(user);

    Integer courseId = intVal(body, "courseId", null);
    Integer total = intVal(body, "total", DEFAULT_QUESTION_COUNT);
    Integer timeLimit = intVal(body, "timeLimit", DEFAULT_TIME_LIMIT_MIN);
    Integer difficultyMax = intVal(body, "difficultyMax", 5);
    String title = strVal(body, "title", "智能组卷");
    PracticeContext ctx = resolvePracticeContext(body, user);
    if (courseId == null) {
      courseId = ctx.courseId();
    } else if (ctx.courseId() == null || !courseId.equals(ctx.courseId())) {
      ctx = new PracticeContext(
          ctx.classId(), ctx.peopleId(), courseId,
          ctx.allowedCourses(), ctx.allowedKnowledges(), ctx.skipScopeFilter());
    }

    List<ExamExamDetailEntity> pool = loadScopedQuestionPool(tenantId, ctx, courseId, difficultyMax);
    if (pool.isEmpty()) {
      throw new AppSmsException(-1, "本班练习范围内暂无试题，请检查后台试题配置或题库挂接");
    }

    Map<Integer, List<ExamExamDetailEntity>> byKnowledge = pool.stream()
        .filter(e -> e.getKnowledgeId() != null)
        .collect(Collectors.groupingBy(ExamExamDetailEntity::getKnowledgeId));
    List<Integer> knowledgeIds = new ArrayList<>(byKnowledge.keySet());

    List<ExamExamDetailEntity> picked = new ArrayList<>();
    if (!knowledgeIds.isEmpty() && total > 0) {
      int perKnowledge = Math.max(1, total / knowledgeIds.size());
      for (Integer kid : knowledgeIds) {
        List<ExamExamDetailEntity> ks = new ArrayList<>(byKnowledge.get(kid));
        Collections.shuffle(ks);
        picked.addAll(ks.subList(0, Math.min(perKnowledge, ks.size())));
      }
    }
    List<ExamExamDetailEntity> orphan = pool.stream()
        .filter(e -> e.getKnowledgeId() == null).collect(Collectors.toList());
    Collections.shuffle(orphan);
    int i = 0;
    while (picked.size() < total && i < orphan.size()) {
      picked.add(orphan.get(i++));
    }
    while (picked.size() < total && picked.size() < pool.size()) {
      picked.add(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
    }
    if (picked.size() > total) {
      picked = new ArrayList<>(picked.subList(0, total));
    }

    return persistPaper(user, title, "mock", picked, timeLimit, ctx);
  }

  public Map<String, Object> randomPractice(Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    Integer courseId = intVal(body, "courseId", null);
    Integer total = intVal(body, "total", DEFAULT_QUESTION_COUNT);
    String orderBy = strVal(body, "orderBy", "random").toLowerCase(Locale.ROOT);
    boolean takeAll = boolVal(body, "all", false)
        || "type".equals(orderBy)
        || "seq_type".equals(orderBy)
        || "knowledge".equals(orderBy)
        || "seq_knowledge".equals(orderBy);
    PracticeContext ctx = resolvePracticeContext(body, user);
    if (courseId == null) {
      courseId = ctx.courseId();
    } else if (ctx.courseId() == null || !courseId.equals(ctx.courseId())) {
      ctx = new PracticeContext(
          ctx.classId(), ctx.peopleId(), courseId,
          ctx.allowedCourses(), ctx.allowedKnowledges(), ctx.skipScopeFilter());
    }
    Long tenantId = resolveExamTenantId(user);

    List<ExamExamDetailEntity> pool = loadScopedQuestionPool(tenantId, ctx, courseId, null);
    pool = filterPoolByRequest(pool, body, courseId);
    if (boolVal(body, "excludeDone", false) && ctx.classId() != null) {
      Set<Integer> doneIds = new HashSet<>(
          answerLogMapper.findDoneQuestionIdsByClass(tenantId, user.getUserId(), ctx.classId()));
      if (!doneIds.isEmpty()) {
        pool = pool.stream()
            .filter(q -> q.getId() == null || !doneIds.contains(q.getId()))
            .collect(Collectors.toList());
      }
    }
    if (pool.isEmpty()) {
      if (boolVal(body, "excludeDone", false)) {
        throw new AppSmsException(-1, "本范围内题目都做过了，可关闭「只练未做」复习，或去错题重练");
      }
      throw new AppSmsException(-1, "所选范围暂无试题，请调整题型或知识点后重试");
    }

    List<ExamExamDetailEntity> working = new ArrayList<>(pool);
    if ("type".equals(orderBy) || "seq_type".equals(orderBy)) {
      working.sort(Comparator
          .comparingInt((ExamExamDetailEntity q) -> typeSortKey(q.getType()))
          .thenComparing(ExamExamDetailEntity::getId, Comparator.nullsLast(Integer::compareTo)));
      orderBy = "type";
    } else if ("knowledge".equals(orderBy) || "seq_knowledge".equals(orderBy)) {
      working.sort(Comparator
          .comparing(ExamExamDetailEntity::getKnowledgeId, Comparator.nullsLast(Integer::compareTo))
          .thenComparing(ExamExamDetailEntity::getId, Comparator.nullsLast(Integer::compareTo)));
      orderBy = "knowledge";
    } else {
      Collections.shuffle(working);
      orderBy = "random";
      takeAll = boolVal(body, "all", false);
    }
    int limit = takeAll ? working.size() : Math.min(total == null ? DEFAULT_QUESTION_COUNT : total, working.size());
    List<ExamExamDetailEntity> picked = new ArrayList<>(working.subList(0, limit));
    String mode = switch (orderBy) {
      case "type" -> "seq_type";
      case "knowledge" -> "seq_knowledge";
      default -> "random";
    };
    String title = switch (orderBy) {
      case "type" -> "按题型顺序练习";
      case "knowledge" -> "按知识点顺序练习";
      default -> "随机练习";
    };
    return persistPaper(user, title, mode, picked, 0, ctx);
  }

  /** 按请求中的题型 / 知识点再过滤抽题池（仍受班级白名单约束）。 */
  private List<ExamExamDetailEntity> filterPoolByRequest(
      List<ExamExamDetailEntity> pool, Map<String, Object> body, Integer courseId) {
    if (pool == null || pool.isEmpty()) {
      return List.of();
    }
    Set<String> types = parseTypeFilter(body);
    Set<Integer> knowledgeFilter = parseKnowledgeFilter(body);
    if (types.isEmpty() && knowledgeFilter.isEmpty()) {
      return pool;
    }
    Set<Integer> expandedKids = knowledgeFilter.isEmpty()
        ? Set.of()
        : expandKnowledgeDescendants(
            knowledgeFilter,
            courseId != null && courseId > 0 ? Set.of(courseId) : Set.of());
    return pool.stream()
        .filter(q -> {
          if (!types.isEmpty()) {
            String t = normalizeQuestionType(q.getType());
            if (!types.contains(t)) {
              return false;
            }
          }
          if (!expandedKids.isEmpty()) {
            return q.getKnowledgeId() != null && expandedKids.contains(q.getKnowledgeId());
          }
          return true;
        })
        .collect(Collectors.toList());
  }

  private static Set<String> parseTypeFilter(Map<String, Object> body) {
    Set<String> out = new LinkedHashSet<>();
    if (body == null) {
      return out;
    }
    Object raw = body.get("types");
    if (raw == null) {
      raw = body.get("questionTypes");
    }
    if (raw instanceof Collection<?> col) {
      for (Object o : col) {
        String n = normalizeQuestionType(o == null ? null : String.valueOf(o));
        if (n != null) {
          out.add(n);
        }
      }
    } else if (raw != null) {
      String s = String.valueOf(raw).trim();
      if (!s.isEmpty()) {
        for (String part : s.split("[,，\\s]+")) {
          String n = normalizeQuestionType(part);
          if (n != null) {
            out.add(n);
          }
        }
      }
    }
    String single = strVal(body, "type", null);
    if (single != null) {
      String n = normalizeQuestionType(single);
      if (n != null) {
        out.add(n);
      }
    }
    return out;
  }

  private static Set<Integer> parseKnowledgeFilter(Map<String, Object> body) {
    Set<Integer> out = new LinkedHashSet<>();
    if (body == null) {
      return out;
    }
    Object raw = body.get("knowledgeIds");
    if (raw instanceof Collection<?> col) {
      for (Object o : col) {
        Integer id = toInt(o);
        if (id != null && id > 0) {
          out.add(id);
        }
      }
    } else if (raw != null) {
      String s = String.valueOf(raw).trim();
      if (!s.isEmpty()) {
        for (String part : s.split("[,，\\s]+")) {
          Integer id = toInt(part);
          if (id != null && id > 0) {
            out.add(id);
          }
        }
      }
    }
    Integer one = intVal(body, "knowledgeId", null);
    if (one != null && one > 0) {
      out.add(one);
    }
    return out;
  }

  private static String normalizeQuestionType(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String t = raw.trim().toLowerCase(Locale.ROOT);
    return switch (t) {
      case "single", "danxuan", "单选", "1" -> "single";
      case "multi", "duoxuan", "多选", "2" -> "multi";
      case "judge", "pandan", "判断", "3" -> "judge";
      default -> t;
    };
  }

  private static int typeSortKey(String raw) {
    String t = normalizeQuestionType(raw);
    if ("single".equals(t)) {
      return 1;
    }
    if ("multi".equals(t)) {
      return 2;
    }
    if ("judge".equals(t)) {
      return 3;
    }
    return 9;
  }

  public Map<String, Object> wrongReplay(Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = resolveExamTenantId(user);
    Long userId = user.getUserId();
    PracticeContext ctx = resolvePracticeContext(body, user);

    Integer knowledgeId = intVal(body, "knowledgeId", null);
    // 本班错题 / all=true / limit≤0：取全部错题按顺序练，不截断题量
    boolean takeAll = ctx.classId() != null
        || boolVal(body, "all", false)
        || (body != null && body.containsKey("limit") && intVal(body, "limit", 0) <= 0);
    Integer limit = takeAll ? null : intVal(body, "limit", WRONG_REPLAY_LIMIT);

    List<Integer> wrongIds = answerLogMapper.findWrongQuestionIds(tenantId, userId, knowledgeId, limit);
    if (wrongIds.isEmpty()) {
      throw new AppSmsException(-1, "暂无错题，快去刷题吧");
    }
    Map<Integer, ExamExamDetailEntity> byId = examDetailMapper.selectBatchIds(wrongIds).stream()
        .filter(e -> e.getDeleted() == 0)
        .collect(Collectors.toMap(ExamExamDetailEntity::getId, e -> e, (a, b) -> a, LinkedHashMap::new));
    // 保持错题出现顺序（首次答错时间升序），不做乱序
    List<ExamExamDetailEntity> questions = new ArrayList<>();
    for (Integer id : wrongIds) {
      ExamExamDetailEntity q = byId.get(id);
      if (q != null) {
        questions.add(q);
      }
    }
    questions = applyPracticeScopeFilter(questions, ctx);
    if (questions.isEmpty()) {
      throw new AppSmsException(-1, "暂无错题，快去刷题吧");
    }
    return persistPaper(user, "错题重练", "wrong", questions, 0, ctx);
  }

  /**
   * 练习过程中单题进度：揭晓后立即写入 answer_log，便于覆盖进度实时更新。
   * 错题重练且答对时同步出本。
   */
  public Map<String, Object> recordPaperProgress(Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = resolveExamTenantId(user);
    Integer paperId = intVal(body, "paperId", null);
    Integer questionId = intVal(body, "questionId", null);
    String userAnswer = strVal(body, "answer", "");
    long spendMs = longVal(body, "spendMs", 0L);
    if (paperId == null || paperId <= 0 || questionId == null || questionId <= 0) {
      throw new AppSmsException(-1, "参数不完整");
    }
    AppExamMockPaperEntity paper = mockPaperMapper.selectById(paperId);
    if (paper == null) {
      throw new AppSmsException(-1, "试卷不存在");
    }
    if (!user.getUserId().equals(paper.getUserId())) {
      throw new AppSmsException(-1, "无权操作此试卷");
    }
    if ("done".equalsIgnoreCase(String.valueOf(paper.getStatus()))) {
      throw new AppSmsException(-1, "试卷已交卷");
    }
    List<Integer> qIds = parseIds(paper.getQuestionIds());
    if (!qIds.contains(questionId)) {
      throw new AppSmsException(-1, "题目不在本卷中");
    }
    ExamExamDetailEntity q = examDetailMapper.selectById(questionId);
    if (q == null || (q.getDeleted() != null && q.getDeleted() != 0)) {
      throw new AppSmsException(-1, "题目不存在");
    }
    ParsedQuestion pq = parseQuestionForApp(q);
    boolean right = isUserAnswerCorrect(pq.questionType(), pq.correctAnswer(), userAnswer);
    String mode = paper.getMode() == null ? "" : paper.getMode().trim().toLowerCase(Locale.ROOT);
    int deleted = 0;
    if ("wrong".equals(mode) && right) {
      deleted = answerLogMapper.deleteWrongLogsByQuestion(tenantId, user.getUserId(), questionId);
    }

    Long nextLogId = answerLogMapper.selectMaxId();
    AppExamUserAnswerLogEntity log = new AppExamUserAnswerLogEntity();
    log.setId(nextLogId == null ? 1L : nextLogId + 1);
    log.setUserId(user.getUserId());
    log.setTenantId(tenantId);
    log.setQuestionId(q.getId());
    log.setKnowledgeId(q.getKnowledgeId());
    log.setQuestionType(q.getType());
    log.setUserAnswer(userAnswer);
    log.setIsRight(right ? 1 : 0);
    log.setSource("progress");
    log.setPaperId(paper.getId());
    log.setSpendMs(spendMs);
    log.setClassId(paper.getClassId());
    log.setPeopleId(paper.getPeopleId());
    log.setCourseId(paper.getCourseId() != null ? paper.getCourseId() : q.getExamId());
    log.setBizDate(LocalDate.now());
    log.setCreateTime(LocalDateTime.now());
    answerLogMapper.insert(log);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("recorded", true);
    out.put("isRight", right ? 1 : 0);
    out.put("removed", "wrong".equals(mode) && right);
    out.put("removedFromWrong", "wrong".equals(mode) && right);
    out.put("deletedWrongLogs", deleted);
    out.put("questionId", questionId);
    out.put("knowledgeId", q.getKnowledgeId());
    out.put("classId", paper.getClassId());
    return out;
  }

  /**
   * 错题重练中答对：与单题进度共用（答对出本；答错也会记进度）。
   */
  public Map<String, Object> resolveWrongOnCorrect(Map<String, Object> body) {
    Map<String, Object> req = body == null ? new LinkedHashMap<>() : new LinkedHashMap<>(body);
    return recordPaperProgress(req);
  }

  // ============================================================
  // 4. 考点预测：基于历史答题分析薄弱考点
  // ============================================================
  public Map<String, Object> knowledgePredict() {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = resolveExamTenantId(user);
    Long userId = user.getUserId();

    List<Map<String, Object>> stats = answerLogMapper.wrongStatsByKnowledge(tenantId, userId);
    if (stats.isEmpty()) {
      return Map.of("items", List.of(), "summary", Map.of("totalAnswered", 0, "totalWrong", 0, "advice", "暂无答题记录，先去刷题吧"));
    }

    List<Integer> knowledgeIds = stats.stream()
        .map(s -> toInt(s.get("knowledgeId")))
        .filter(java.util.Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
    Map<Integer, String> knowledgeTitle = loadKnowledgeTitle(knowledgeIds);

    int totalAnswered = 0;
    int totalWrong = 0;
    List<Map<String, Object>> items = new ArrayList<>();
    for (Map<String, Object> s : stats) {
      Integer kid = toInt(s.get("knowledgeId"));
      int wrong = toIntOrZero(s.get("wrongCount"));
      int tot = toIntOrZero(s.get("totalCount"));
      totalAnswered += tot;
      totalWrong += wrong;
      double wrongRate = tot == 0 ? 0 : (wrong * 100.0 / tot);
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("knowledgeId", kid);
      item.put("knowledgeName", knowledgeTitle.getOrDefault(kid, "未知考点"));
      item.put("wrongCount", wrong);
      item.put("totalCount", tot);
      item.put("wrongRate", Math.round(wrongRate * 10) / 10.0);
      item.put("level", wrongRate >= 60 ? "薄弱" : wrongRate >= 30 ? "一般" : "掌握");
      items.add(item);
    }
    // 按错误率倒序
    items.sort((a, b) -> {
      double ar = a.get("wrongRate") instanceof Number ? ((Number) a.get("wrongRate")).doubleValue() : 0;
      double br = b.get("wrongRate") instanceof Number ? ((Number) b.get("wrongRate")).doubleValue() : 0;
      return Double.compare(br, ar);
    });

    String advice;
    if (totalWrong == 0) {
      advice = "全部答对，继续保持！";
    } else {
      Object weakest = items.get(0).get("knowledgeName");
      advice = "建议优先复习「" + weakest + "」";
    }

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("totalAnswered", totalAnswered);
    summary.put("totalWrong", totalWrong);
    summary.put("wrongRate", totalAnswered == 0 ? 0 : Math.round(totalWrong * 1000.0 / totalAnswered) / 10.0);
    summary.put("advice", advice);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("items", items);
    data.put("summary", summary);
    return data;
  }

  // ============================================================
  // 5. 错题本：列出错题（带题目内容）
  // ============================================================
  public Map<String, Object> wrongBook(Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = resolveExamTenantId(user);
    Long userId = user.getUserId();
    Integer knowledgeId = intVal(body, "knowledgeId", null);
    Integer limit = intVal(body, "limit", 50);
    Integer page = intVal(body, "page", 1);
    int size = Math.min(limit, 100);
    int offset = (page - 1) * size;

    List<Integer> wrongIds = answerLogMapper.findWrongQuestionIds(tenantId, userId, knowledgeId, offset + size);
    if (wrongIds.isEmpty()) {
      return Map.of("list", List.of(), "total", 0, "page", page, "size", size);
    }
    List<Integer> pageIds = wrongIds.subList(Math.min(offset, wrongIds.size()), Math.min(offset + size, wrongIds.size()));
    List<ExamExamDetailEntity> questions = pageIds.isEmpty()
        ? List.of()
        : examDetailMapper.selectBatchIds(pageIds).stream()
            .filter(e -> e.getDeleted() == 0)
            .collect(Collectors.toList());

    List<Map<String, Object>> list = new ArrayList<>();
    for (ExamExamDetailEntity q : questions) {
      list.add(toQuestionView(q, true));
    }
    return Map.of("list", list, "total", wrongIds.size(), "page", page, "size", size);
  }

  /**
   * AI 精准学 E-P0：四宫格摘要（聚合错题本 + 考点预测，不改变原接口契约）。
   */
  public Map<String, Object> preciseOverview(Map<String, Object> body) {
    Map<String, Object> req = body == null ? Map.of() : body;
    Map<String, Object> wrongReq = new LinkedHashMap<>();
    wrongReq.put("page", 1);
    wrongReq.put("limit", 1);
    Integer knowledgeId = intVal(req, "knowledgeId", null);
    if (knowledgeId != null) {
      wrongReq.put("knowledgeId", knowledgeId);
    }
    Map<String, Object> wrong = wrongBook(wrongReq);
    Map<String, Object> predict = knowledgePredict();
    @SuppressWarnings("unchecked")
    Map<String, Object> summary = predict.get("summary") instanceof Map
        ? (Map<String, Object>) predict.get("summary")
        : Map.of();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = predict.get("items") instanceof List
        ? (List<Map<String, Object>>) predict.get("items")
        : List.of();
    long weakCount = items.stream()
        .filter(it -> "薄弱".equals(String.valueOf(it.get("level"))))
        .count();
    List<Map<String, Object>> topWeak = items.stream().limit(5).toList();

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("wrongTotal", wrong.getOrDefault("total", 0));
    out.put("weakCount", weakCount);
    out.put("totalAnswered", summary.getOrDefault("totalAnswered", 0));
    out.put("totalWrong", summary.getOrDefault("totalWrong", 0));
    out.put("wrongRate", summary.getOrDefault("wrongRate", 0));
    out.put("advice", summary.getOrDefault("advice", "先去刷几题，再回来看薄弱点。"));
    out.put("topWeak", topWeak);
    out.put("classId", req.get("classId"));
    out.put("hint", "考前冲刺请用「同步练习」；班级作业请走「我的班级」。");
    return out;
  }

  // ============================================================
  // 6. 试卷详情：加载已生成的卷
  // ============================================================
  public Map<String, Object> paperDetail(Integer paperId) {
    if (paperId == null || paperId <= 0) {
      throw new AppSmsException(-1, "试卷 id 为空");
    }
    AppExamMockPaperEntity paper = mockPaperMapper.selectById(paperId);
    if (paper == null) {
      throw new AppSmsException(-1, "试卷不存在");
    }
    return paperToView(paper, true);
  }

  // ============================================================
  // 7. 交卷：判分 + 写答题明细 + 更新试卷状态
  // ============================================================
  public Map<String, Object> submitPaper(Integer paperId, Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    if (paperId == null || paperId <= 0) {
      throw new AppSmsException(-1, "试卷 id 为空");
    }
    AppExamMockPaperEntity paper = mockPaperMapper.selectById(paperId);
    if (paper == null) {
      throw new AppSmsException(-1, "试卷不存在");
    }
    if (!"done".equals(paper.getStatus()) && !paper.getUserId().equals(user.getUserId())) {
      throw new AppSmsException(-1, "无权操作此试卷");
    }
    if (paper.getClassId() != null && paper.getClassId() > 0) {
      assertClassPracticeWindowOpen(paper.getClassId());
    }

    // answers: [{questionId, answer, spendMs}]
    Object answersObj = body == null ? null : body.get("answers");
    List<?> answers = answersObj instanceof List ? (List<?>) answersObj : List.of();
    Map<Integer, Map<String, Object>> answerMap = new HashMap<>();
    for (Object o : answers) {
      if (o instanceof Map<?, ?> m) {
        Integer qid = toInt(m.get("questionId"));
        if (qid != null) {
          answerMap.put(qid, (Map<String, Object>) m);
        }
      }
    }

    List<Integer> qIds = parseIds(paper.getQuestionIds());
    List<ExamExamDetailEntity> questions = qIds.isEmpty() ? List.of() : examDetailMapper.selectBatchIds(qIds);
    Map<Integer, ExamExamDetailEntity> qMap = questions.stream()
        .collect(Collectors.toMap(ExamExamDetailEntity::getId, e -> e));

    int rightCount = 0;
    BigDecimal score = BigDecimal.ZERO;
    List<Map<String, Object>> detailList = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    Long nextLogId = answerLogMapper.selectMaxId();
    long logIdCursor = nextLogId == null ? 1L : nextLogId + 1;
    for (Integer qid : qIds) {
      ExamExamDetailEntity q = qMap.get(qid);
      if (q == null) continue;
      Map<String, Object> ans = answerMap.get(qid);
      String userAnswer = ans == null ? "" : strVal(ans, "answer", "");
      long spendMs = ans == null ? 0 : longVal(ans, "spendMs", 0L);
      ParsedQuestion pq = parseQuestionForApp(q);
      boolean right = isUserAnswerCorrect(pq.questionType(), pq.correctAnswer(), userAnswer);
      if (right) {
        rightCount++;
        if (q.getScore() != null) {
          score = score.add(q.getScore());
        }
        // 错题重练答对：清掉历史错记，确保出本
        if ("wrong".equalsIgnoreCase(String.valueOf(paper.getMode()))) {
          answerLogMapper.deleteWrongLogsByQuestion(resolveExamTenantId(user), user.getUserId(), q.getId());
        }
      }
      // 写答题明细（班级上下文从试卷带回，避免客户端交卷漏传 classId）
      AppExamUserAnswerLogEntity log = new AppExamUserAnswerLogEntity();
      log.setId(logIdCursor++);
      log.setUserId(user.getUserId());
      log.setTenantId(resolveExamTenantId(user));
      log.setQuestionId(q.getId());
      log.setKnowledgeId(q.getKnowledgeId());
      log.setQuestionType(q.getType());
      log.setUserAnswer(userAnswer);
      log.setIsRight(right ? 1 : 0);
      log.setSource(paper.getMode());
      log.setPaperId(paper.getId());
      log.setSpendMs(spendMs);
      log.setClassId(paper.getClassId());
      log.setPeopleId(paper.getPeopleId());
      log.setCourseId(paper.getCourseId() != null ? paper.getCourseId() : q.getExamId());
      log.setBizDate(now.toLocalDate());
      log.setCreateTime(now);
      answerLogMapper.insert(log);

      Map<String, Object> d = new LinkedHashMap<>();
      d.put("questionId", q.getId());
      d.put("isRight", right ? 1 : 0);
      d.put("userAnswer", userAnswer);
      d.put("rightAnswer", pq.correctAnswer());
      d.put("score", q.getScore());
      d.put("resolve", q.getResolve());
      detailList.add(d);
    }

    paper.setRightCount(rightCount);
    paper.setScore(score.intValue());
    paper.setStatus("done");
    paper.setFinishTime(now);
    mockPaperMapper.updateById(paper);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("paperId", paper.getId());
    result.put("score", paper.getScore());
    result.put("totalPoint", paper.getTotalPoint());
    result.put("rightCount", rightCount);
    result.put("totalCount", paper.getTotalCount());
    result.put("passed", paper.getPassPoint() != null && paper.getScore() >= paper.getPassPoint());
    result.put("details", detailList);
    return result;
  }

  // ============================================================
  // 8. 模拟考历史：列出该用户的试卷
  // ============================================================
  public Map<String, Object> paperHistory(Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    String mode = strVal(body, "mode", null);
    Integer page = intVal(body, "page", 1);
    Integer size = intVal(body, "size", 10);
    int s = Math.min(size, 50);
    int offset = (page - 1) * s;

    LambdaQueryWrapper<AppExamMockPaperEntity> q = new LambdaQueryWrapper<AppExamMockPaperEntity>()
        .eq(AppExamMockPaperEntity::getTenantId, resolveExamTenantId(user))
        .eq(AppExamMockPaperEntity::getUserId, user.getUserId())
        .orderByDesc(AppExamMockPaperEntity::getId);
    if (StringUtils.hasText(mode)) {
      q.eq(AppExamMockPaperEntity::getMode, mode);
    }
    Long total = mockPaperMapper.selectCount(q);
    q.last("LIMIT " + s + " OFFSET " + offset);
    List<AppExamMockPaperEntity> papers = mockPaperMapper.selectList(q);

    List<Map<String, Object>> list = papers.stream()
        .map(p -> paperToView(p, false))
        .collect(Collectors.toList());
    return Map.of("list", list, "total", total, "page", page, "size", s);
  }

  // ============================================================
  // 私有工具
  // ============================================================

  private Map<String, Object> persistPaper(PlatformUserPrincipal user, String title, String mode,
                                           List<ExamExamDetailEntity> questions, int timeLimit,
                                           PracticeContext ctx) {
    AppExamMockPaperEntity paper = new AppExamMockPaperEntity();
    paper.setUserId(user.getUserId());
    paper.setTenantId(resolveExamTenantId(user));
    paper.setTitle(title);
    paper.setQuestionIds(questions.stream().map(e -> String.valueOf(e.getId())).collect(Collectors.joining(",")));
    paper.setTotalCount(questions.size());
    int total = questions.stream().map(ExamExamDetailEntity::getScore)
        .filter(java.util.Objects::nonNull).mapToInt(BigDecimal::intValue).sum();
    paper.setTotalPoint(total);
    paper.setPassPoint((int) Math.round(total * 0.6));
    paper.setTimeLimit(timeLimit);
    paper.setMode(mode);
    paper.setStatus("pending");
    paper.setCreateTime(LocalDateTime.now());
    if (ctx != null) {
      paper.setClassId(ctx.classId());
      paper.setPeopleId(ctx.peopleId());
      paper.setCourseId(ctx.courseId());
    }
    // 存量表可能无 AUTO_INCREMENT：迁移未跑时手动取号，避免 Field 'id' doesn't have a default value
    if (paper.getId() == null) {
      Integer maxId = mockPaperMapper.selectMaxId();
      paper.setId(maxId == null ? 1 : maxId + 1);
    }
    mockPaperMapper.insert(paper);
    Map<String, Object> view = paperToView(paper, true);
    if (ctx != null && ctx.classId() != null) {
      view.put("classId", ctx.classId());
      view.put("peopleId", ctx.peopleId());
    }
    return view;
  }

  /**
   * 解析班级刷题上下文：有 classId 则校验花名册、练习范围开关，并准备抽题白名单。
   */
  private PracticeContext resolvePracticeContext(Map<String, Object> body, PlatformUserPrincipal user) {
    Long classId = longVal(body, "classId", null);
    Integer courseId = intVal(body, "courseId", null);
    if (classId == null || classId <= 0) {
      return new PracticeContext(null, null, courseId, null, null, true);
    }

    TrainPeopleListEntity people = peopleListMapper.selectOne(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getPlatformUserId, user.getUserId())
            .last("LIMIT 1"));
    if (people == null) {
      throw new AppSmsException(-1, "您不在该班级花名册中，无法按班刷题");
    }

    TrainClassPracticeScopeEntity scope = practiceScopeMapper.selectOne(
        new LambdaQueryWrapper<TrainClassPracticeScopeEntity>()
            .eq(TrainClassPracticeScopeEntity::getClassId, classId)
            .last("LIMIT 1"));
    if (scope != null && scope.getEnabled() != null && scope.getEnabled() == 0) {
      throw new AppSmsException(-1, "本班练习范围已关闭，请从全局刷题入口练习");
    }
    assertPracticeWindowOpen(scope);

    Set<Integer> allowedCourses = null;
    Set<Integer> allowedKnowledges = null;
    List<TrainClassPracticeCourseEntity> courses = practiceCourseMapper.selectList(
        new LambdaQueryWrapper<TrainClassPracticeCourseEntity>()
            .eq(TrainClassPracticeCourseEntity::getClassId, classId));
    if (!courses.isEmpty()) {
      allowedCourses = courses.stream()
          .map(TrainClassPracticeCourseEntity::getExamCourseId)
          .filter(java.util.Objects::nonNull)
          .collect(Collectors.toCollection(HashSet::new));
    }
    List<TrainClassPracticeKnowledgeEntity> knowledges = practiceKnowledgeMapper.selectList(
        new LambdaQueryWrapper<TrainClassPracticeKnowledgeEntity>()
            .eq(TrainClassPracticeKnowledgeEntity::getClassId, classId));
    if (!knowledges.isEmpty()) {
      allowedKnowledges = knowledges.stream()
          .map(TrainClassPracticeKnowledgeEntity::getKnowledgeId)
          .filter(java.util.Objects::nonNull)
          .collect(Collectors.toCollection(HashSet::new));
    }

    Integer resolvedCourse = courseId;
    if (resolvedCourse == null) {
      resolvedCourse = questionsCourseHint(allowedCourses);
    }
    return new PracticeContext(classId, people.getId(), resolvedCourse, allowedCourses, allowedKnowledges, false);
  }

  private void assertClassPracticeWindowOpen(Long classId) {
    TrainClassPracticeScopeEntity scope = practiceScopeMapper.selectOne(
        new LambdaQueryWrapper<TrainClassPracticeScopeEntity>()
            .eq(TrainClassPracticeScopeEntity::getClassId, classId)
            .last("LIMIT 1"));
    if (scope != null && scope.getEnabled() != null && scope.getEnabled() == 0) {
      throw new AppSmsException(-1, "本班练习范围已关闭，请从全局刷题入口练习");
    }
    assertPracticeWindowOpen(scope);
  }

  /** 未配置开始/截止则不过期；有配置则按自然日判断（截止日含当天） */
  private static void assertPracticeWindowOpen(TrainClassPracticeScopeEntity scope) {
    if (scope == null) {
      return;
    }
    LocalDate today = LocalDate.now();
    LocalDateTime start = scope.getPracticeStartTime();
    LocalDateTime end = scope.getPracticeEndTime();
    if (start != null && today.isBefore(start.toLocalDate())) {
      throw new AppSmsException(-1, "本班刷题尚未开始（开放日期：" + start.toLocalDate() + "）");
    }
    if (end != null && today.isAfter(end.toLocalDate())) {
      throw new AppSmsException(-1, "本班刷题已截止（截止日期：" + end.toLocalDate() + "）");
    }
  }

  private static Integer questionsCourseHint(Set<Integer> allowedCourses) {
    if (allowedCourses == null || allowedCourses.isEmpty()) {
      return null;
    }
    return allowedCourses.iterator().next();
  }

  /**
   * 班级/科目范围内抽题池。
   * <p>不依赖 {@code exam_exam_detail.exam_id}（存量常为 0），优先按知识点树
   * {@code exam_knowledge_structure.course_id} 关联，与跟班题库选题口径一致。
   */
  private List<ExamExamDetailEntity> loadScopedQuestionPool(
      Long tenantId,
      PracticeContext ctx,
      Integer preferredCourseId,
      Integer difficultyMax) {
    Set<Integer> courseIds = new HashSet<>();
    if (preferredCourseId != null && preferredCourseId > 0) {
      courseIds.add(preferredCourseId);
    } else if (ctx != null && ctx.allowedCourses() != null) {
      courseIds.addAll(ctx.allowedCourses());
    }

    Set<Integer> whitelist = ctx != null && ctx.allowedKnowledges() != null
        ? new HashSet<>(ctx.allowedKnowledges())
        : Set.of();
    Set<Integer> knowledgeIds;
    if (!whitelist.isEmpty()) {
      knowledgeIds = expandKnowledgeDescendants(whitelist, courseIds);
      if (!courseIds.isEmpty() && !knowledgeIds.isEmpty()) {
        Set<Integer> courseKnowledges = new HashSet<>(listKnowledgeIdsByCourses(courseIds));
        knowledgeIds = knowledgeIds.stream().filter(courseKnowledges::contains).collect(Collectors.toCollection(HashSet::new));
      }
    } else if (!courseIds.isEmpty()) {
      knowledgeIds = new HashSet<>(listKnowledgeIdsByCourses(courseIds));
    } else {
      knowledgeIds = Set.of();
    }

    LambdaQueryWrapper<ExamExamDetailEntity> q = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId.intValue())
        .eq(ExamExamDetailEntity::getDeleted, 0)
        .eq(ExamExamDetailEntity::getStatus, 1)
        .in(ExamExamDetailEntity::getType, List.of("single", "multi", "judge"));
    if (difficultyMax != null) {
      q.le(ExamExamDetailEntity::getDifficulty, difficultyMax);
    }

    if (!knowledgeIds.isEmpty() && !courseIds.isEmpty()) {
      List<Integer> kids = new ArrayList<>(knowledgeIds);
      List<Integer> eids = new ArrayList<>(courseIds);
      q.and(w -> w.in(ExamExamDetailEntity::getKnowledgeId, kids)
          .or()
          .in(ExamExamDetailEntity::getExamId, eids));
    } else if (!knowledgeIds.isEmpty()) {
      q.in(ExamExamDetailEntity::getKnowledgeId, knowledgeIds);
    } else if (!courseIds.isEmpty()) {
      // 知识点树为空时退回 exam_id（仅覆盖已正确回填的题）
      q.in(ExamExamDetailEntity::getExamId, courseIds);
    }

    q.last("LIMIT 5000");
    return examDetailMapper.selectList(q);
  }

  private List<Integer> listKnowledgeIdsByCourses(Collection<Integer> courseIds) {
    if (courseIds == null || courseIds.isEmpty()) {
      return List.of();
    }
    return knowledgeMapper.selectList(new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .in(ExamKnowledgeStructureEntity::getCourseId, courseIds)
            .eq(ExamKnowledgeStructureEntity::getDeleted, 0)
            .select(ExamKnowledgeStructureEntity::getId))
        .stream()
        .map(ExamKnowledgeStructureEntity::getId)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
  }

  /** 白名单节点展开为自身 + 子孙（后台常勾选父级知识点）。 */
  private Set<Integer> expandKnowledgeDescendants(Set<Integer> seeds, Set<Integer> courseHint) {
    if (seeds == null || seeds.isEmpty()) {
      return Set.of();
    }
    Set<Integer> courseIds = courseHint == null ? new HashSet<>() : new HashSet<>(courseHint);
    if (courseIds.isEmpty()) {
      List<ExamKnowledgeStructureEntity> seedNodes = knowledgeMapper.selectBatchIds(seeds);
      for (ExamKnowledgeStructureEntity n : seedNodes) {
        if (n.getCourseId() != null) {
          courseIds.add(n.getCourseId());
        }
      }
    }
    List<ExamKnowledgeStructureEntity> all = courseIds.isEmpty()
        ? knowledgeMapper.selectBatchIds(seeds)
        : knowledgeMapper.selectList(new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .in(ExamKnowledgeStructureEntity::getCourseId, courseIds)
            .eq(ExamKnowledgeStructureEntity::getDeleted, 0));
    Map<Integer, List<Integer>> children = new HashMap<>();
    for (ExamKnowledgeStructureEntity n : all) {
      int pid = n.getParentId() == null ? 0 : n.getParentId();
      children.computeIfAbsent(pid, k -> new ArrayList<>()).add(n.getId());
    }
    Set<Integer> out = new HashSet<>();
    ArrayDeque<Integer> dq = new ArrayDeque<>(seeds);
    while (!dq.isEmpty()) {
      Integer id = dq.poll();
      if (id == null || !out.add(id)) {
        continue;
      }
      for (Integer c : children.getOrDefault(id, List.of())) {
        dq.add(c);
      }
    }
    return out;
  }

  private List<ExamExamDetailEntity> applyPracticeScopeFilter(List<ExamExamDetailEntity> pool, PracticeContext ctx) {
    if (ctx == null || ctx.skipScopeFilter() || pool == null || pool.isEmpty()) {
      return pool;
    }
    Set<Integer> courses = ctx.allowedCourses();
    Set<Integer> knowledges = ctx.allowedKnowledges();
    if ((courses == null || courses.isEmpty()) && (knowledges == null || knowledges.isEmpty())) {
      return pool;
    }
    Set<Integer> effectiveKnowledges;
    if (knowledges != null && !knowledges.isEmpty()) {
      effectiveKnowledges = expandKnowledgeDescendants(knowledges, courses == null ? Set.of() : courses);
    } else if (courses != null && !courses.isEmpty()) {
      effectiveKnowledges = new HashSet<>(listKnowledgeIdsByCourses(courses));
    } else {
      effectiveKnowledges = Set.of();
    }
    Map<Integer, Integer> knowledgeCourse = loadKnowledgeCourseMap(effectiveKnowledges);
    return pool.stream().filter(q -> {
      if (q.getKnowledgeId() != null && !effectiveKnowledges.isEmpty()
          && effectiveKnowledges.contains(q.getKnowledgeId())) {
        return true;
      }
      if (courses != null && !courses.isEmpty()
          && q.getExamId() != null && q.getExamId() > 0 && courses.contains(q.getExamId())) {
        return true;
      }
      if (courses != null && !courses.isEmpty() && q.getKnowledgeId() != null) {
        Integer cid = knowledgeCourse.get(q.getKnowledgeId());
        return cid != null && courses.contains(cid);
      }
      return false;
    }).collect(Collectors.toList());
  }

  private Map<Integer, Integer> loadKnowledgeCourseMap(Collection<Integer> knowledgeIds) {
    if (knowledgeIds == null || knowledgeIds.isEmpty()) {
      return Map.of();
    }
    List<Integer> ids = knowledgeIds instanceof List
        ? (List<Integer>) knowledgeIds
        : new ArrayList<>(knowledgeIds);
    // selectBatchIds 分批，避免超大 IN
    Map<Integer, Integer> out = new HashMap<>();
    int step = 800;
    for (int i = 0; i < ids.size(); i += step) {
      List<Integer> slice = ids.subList(i, Math.min(i + step, ids.size()));
      for (ExamKnowledgeStructureEntity n : knowledgeMapper.selectBatchIds(slice)) {
        if (n.getId() != null && n.getCourseId() != null) {
          out.put(n.getId(), n.getCourseId());
        }
      }
    }
    return out;
  }

  private record PracticeContext(
      Long classId,
      Long peopleId,
      Integer courseId,
      Set<Integer> allowedCourses,
      Set<Integer> allowedKnowledges,
      boolean skipScopeFilter) {}

  private Map<String, Object> paperToView(AppExamMockPaperEntity paper, boolean withQuestions) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("paperId", paper.getId());
    data.put("title", paper.getTitle());
    data.put("mode", paper.getMode());
    data.put("totalCount", paper.getTotalCount());
    data.put("totalPoint", paper.getTotalPoint());
    data.put("passPoint", paper.getPassPoint());
    data.put("timeLimit", paper.getTimeLimit());
    data.put("status", paper.getStatus());
    data.put("score", paper.getScore());
    data.put("rightCount", paper.getRightCount());
    data.put("createTime", paper.getCreateTime());
    data.put("finishTime", paper.getFinishTime());
    if (withQuestions && StringUtils.hasText(paper.getQuestionIds())) {
      List<Integer> ids = parseIds(paper.getQuestionIds());
      List<ExamExamDetailEntity> questions = ids.isEmpty() ? List.of() : examDetailMapper.selectBatchIds(ids);
      Map<Integer, ExamExamDetailEntity> byId = questions.stream()
          .filter(q -> q.getId() != null)
          .collect(Collectors.toMap(ExamExamDetailEntity::getId, q -> q, (a, b) -> a));
      // 练习类即时反馈：未交卷也返回标准答案与解析；模拟卷交卷前不露答案
      boolean showAnswer = "done".equals(paper.getStatus()) || isPracticeInstantMode(paper.getMode());
      List<Map<String, Object>> views = new ArrayList<>();
      for (Integer id : ids) {
        ExamExamDetailEntity q = byId.get(id);
        if (q != null) {
          views.add(toQuestionView(q, showAnswer));
        }
      }
      data.put("questions", views);
    }
    return data;
  }

  /** 随机/顺序/错题重练：答题过程中即出答案与解析 */
  private static boolean isPracticeInstantMode(String mode) {
    if (!StringUtils.hasText(mode)) {
      return false;
    }
    String m = mode.trim().toLowerCase(Locale.ROOT);
    return "random".equals(m)
        || "wrong".equals(m)
        || "drill".equals(m)
        || m.startsWith("seq");
  }

  private Map<String, Object> toQuestionView(ExamExamDetailEntity q, boolean showAnswer) {
    ParsedQuestion pq = parseQuestionForApp(q);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("questionId", q.getId());
    m.put("type", pq.questionType());
    m.put("knowledgeId", q.getKnowledgeId());
    m.put("difficulty", q.getDifficulty());
    m.put("score", q.getScore());
    m.put("stem", pq.stem());
    m.put("options", pq.options());
    if (showAnswer) {
      m.put("answer", pq.correctAnswer());
      m.put("resolve", q.getResolve());
    }
    return m;
  }

  /**
   * 现网题库：content=题干（常 HTML），answer=选项 JSON（含 isCorrect）。
   * 与跟班作业 {@link FollowTaskBankQuestionConverter} 同口径。
   */
  private ParsedQuestion parseQuestionForApp(ExamExamDetailEntity q) {
    try {
      FollowTaskQuestionReq snap = bankQuestionConverter.convert(q, 0);
      List<Map<String, Object>> options = new ArrayList<>();
      if (snap.getOptions() != null) {
        for (Object raw : snap.getOptions()) {
          if (!(raw instanceof Map<?, ?> om)) {
            continue;
          }
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("tag", String.valueOf(om.get("key") != null ? om.get("key") : ""));
          row.put("content", String.valueOf(om.get("text") != null ? om.get("text") : ""));
          options.add(row);
        }
      }
      return new ParsedQuestion(
          snap.getQuestionType() != null ? snap.getQuestionType() : String.valueOf(q.getType()),
          snap.getStem() != null ? snap.getStem() : "",
          options,
          snap.getAnswer());
    } catch (Exception ex) {
      // 兼容旧「题干\\nA.xx」格式
      String[] parts = splitStemAndOptions(q.getContent());
      List<Map<String, Object>> options = parts.length > 1
          ? java.util.Arrays.stream(parts, 1, parts.length).map(this::optionView).collect(Collectors.toList())
          : List.of();
      return new ParsedQuestion(
          q.getType() == null ? "single" : q.getType(),
          parts[0],
          options,
          q.getAnswer());
    }
  }

  private static boolean isUserAnswerCorrect(String questionType, Object correct, String userRaw) {
    if (!StringUtils.hasText(userRaw) || correct == null) {
      return false;
    }
    String user = userRaw.trim();
    if (correct instanceof List<?> list) {
      Set<String> expect = new LinkedHashSet<>();
      for (Object o : list) {
        if (o != null && StringUtils.hasText(String.valueOf(o))) {
          expect.add(String.valueOf(o).trim().toUpperCase(Locale.ROOT));
        }
      }
      Set<String> got = Arrays.stream(user.replace("，", ",").split("[,\\s]+"))
          .map(String::trim)
          .filter(StringUtils::hasText)
          .map(s -> s.toUpperCase(Locale.ROOT))
          .collect(Collectors.toCollection(LinkedHashSet::new));
      return !expect.isEmpty() && expect.equals(got);
    }
    return String.valueOf(correct).trim().equalsIgnoreCase(user);
  }

  private record ParsedQuestion(
      String questionType,
      String stem,
      List<Map<String, Object>> options,
      Object correctAnswer) {}

  /** 把 "A. 选项内容" 拆成 {tag:"A", content:"选项内容"} */
  @SuppressWarnings("unchecked")
  private Map<String, Object> optionView(String raw) {
    String s = raw == null ? "" : raw.trim();
    Map<String, Object> opt = new LinkedHashMap<>();
    if (s.length() >= 2 && s.charAt(1) == '.') {
      opt.put("tag", String.valueOf(s.charAt(0)));
      opt.put("content", s.substring(2).trim());
    } else {
      opt.put("tag", "");
      opt.put("content", s);
    }
    return opt;
  }

  /** 把 content 按 "\n" 拆成 [题干, 选项1, 选项2, ...] */
  private static String[] splitStemAndOptions(String content) {
    if (content == null || content.isEmpty()) return new String[]{""};
    return content.split("\n");
  }

  private Map<Integer, String> loadKnowledgeTitle(List<Integer> ids) {
    if (ids.isEmpty()) return Map.of();
    List<ExamKnowledgeStructureEntity> list = knowledgeMapper.selectBatchIds(ids);
    return list.stream().collect(Collectors.toMap(ExamKnowledgeStructureEntity::getId,
        ExamKnowledgeStructureEntity::getTitle, (a, b) -> a));
  }

  private static List<Integer> parseIds(String csv) {
    if (!StringUtils.hasText(csv)) return List.of();
    List<Integer> ids = new ArrayList<>();
    for (String s : csv.split(",")) {
      try {
        ids.add(Integer.parseInt(s.trim()));
      } catch (NumberFormatException ignore) {}
    }
    return ids;
  }

  private static PlatformUserPrincipal requireLogin() {
    try {
      PlatformUserPrincipal u = SecurityContext.currentUser();
      if (u == null || u.getUserId() == null) {
        throw new AppSmsException(-1, "请先登录");
      }
      return u;
    } catch (IllegalStateException e) {
      throw new AppSmsException(-1, "请先登录");
    }
  }

  /**
   * App principal.tenantId 恒为 0，题库/答卷租户必须以 header {@code X-Tenant-Id} 为准。
   */
  private static Long resolveExamTenantId(PlatformUserPrincipal user) {
    Long ctx = TenantContextHolder.getTenantId();
    if (ctx != null && ctx > 0) {
      return ctx;
    }
    if (user != null && user.getTenantId() != null && user.getTenantId() > 0) {
      return user.getTenantId();
    }
    return 1L;
  }

  private static String strVal(Map<String, Object> body, String key, String def) {
    if (body == null || body.get(key) == null) return def;
    String s = String.valueOf(body.get(key)).trim();
    return s.isEmpty() ? def : s;
  }

  private static Integer intVal(Map<String, Object> body, String key, Integer def) {
    if (body == null || body.get(key) == null) return def;
    try {
      return Integer.parseInt(String.valueOf(body.get(key)).trim());
    } catch (NumberFormatException e) {
      return def;
    }
  }

  private static boolean boolVal(Map<String, Object> body, String key, boolean def) {
    if (body == null || body.get(key) == null) {
      return def;
    }
    Object v = body.get(key);
    if (v instanceof Boolean b) {
      return b;
    }
    String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
    if (s.isEmpty()) {
      return def;
    }
    return "1".equals(s) || "true".equals(s) || "yes".equals(s) || "y".equals(s);
  }

  private static Long longVal(Map<String, Object> body, String key, Long def) {
    if (body == null || body.get(key) == null) return def;
    try {
      return Long.parseLong(String.valueOf(body.get(key)).trim());
    } catch (NumberFormatException e) {
      return def;
    }
  }

  private static Integer toInt(Object o) {
    if (o == null) return null;
    try {
      return Integer.parseInt(String.valueOf(o));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** 不会返回 null：解析失败返回 0，避免拆箱 NPE */
  private static int toIntOrZero(Object o) {
    if (o == null) return 0;
    try {
      return Integer.parseInt(String.valueOf(o));
    } catch (NumberFormatException e) {
      // SUM/COUNT 在 MyBatis 里可能是 Long/BigDecimal，先转 long 再转 int
      try {
        return (int) Long.parseLong(String.valueOf(o));
      } catch (NumberFormatException e2) {
        return 0;
      }
    }
  }
}
