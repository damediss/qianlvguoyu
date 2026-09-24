package com.guoyu.platform.app.exam.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewWrongItemEntity;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewWrongItemMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeCourseEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeKnowledgeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeScopeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassReviewSyncEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskQuestionReq;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeCourseMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeKnowledgeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeScopeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassReviewSyncMapper;
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
  private static final ObjectMapper JSON = new ObjectMapper();

  private final ExamExamDetailMapper examDetailMapper;
  private final ExamKnowledgeStructureMapper knowledgeMapper;
  private final AppExamMockPaperMapper mockPaperMapper;
  private final AppExamUserAnswerLogMapper answerLogMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainClassPracticeScopeMapper practiceScopeMapper;
  private final TrainClassPracticeCourseMapper practiceCourseMapper;
  private final TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper;
  private final FollowTaskBankQuestionConverter bankQuestionConverter;
  private final TrainClassReviewSyncMapper classReviewSyncMapper;
  private final AiReviewPlanMapper reviewPlanMapper;
  private final AiReviewWrongItemMapper reviewWrongItemMapper;

  public AppExamSmartService(ExamExamDetailMapper examDetailMapper,
                             ExamKnowledgeStructureMapper knowledgeMapper,
                             AppExamMockPaperMapper mockPaperMapper,
                             AppExamUserAnswerLogMapper answerLogMapper,
                             TrainPeopleListMapper peopleListMapper,
                             TrainClassPracticeScopeMapper practiceScopeMapper,
                             TrainClassPracticeCourseMapper practiceCourseMapper,
                             TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper,
                             FollowTaskBankQuestionConverter bankQuestionConverter,
                             TrainClassReviewSyncMapper classReviewSyncMapper,
                             AiReviewPlanMapper reviewPlanMapper,
                             AiReviewWrongItemMapper reviewWrongItemMapper) {
    this.examDetailMapper = examDetailMapper;
    this.knowledgeMapper = knowledgeMapper;
    this.mockPaperMapper = mockPaperMapper;
    this.answerLogMapper = answerLogMapper;
    this.peopleListMapper = peopleListMapper;
    this.practiceScopeMapper = practiceScopeMapper;
    this.practiceCourseMapper = practiceCourseMapper;
    this.practiceKnowledgeMapper = practiceKnowledgeMapper;
    this.bankQuestionConverter = bankQuestionConverter;
    this.classReviewSyncMapper = classReviewSyncMapper;
    this.reviewPlanMapper = reviewPlanMapper;
    this.reviewWrongItemMapper = reviewWrongItemMapper;
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
   * 精准学推题：按弱项加权推送「同考点 + 同题型」相似题。
   * <p>优先：错题本种子多、考点错误率高、弱题型、preferKnowledgeIds；池内优先未做过的新题。
   * 试卷 mode=drill，答错即时解析。
   */
  public Map<String, Object> similarWrongPractice(Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = resolveExamTenantId(user);
    Long userId = user.getUserId();
    PracticeContext ctx = resolvePracticeContext(body, user);

    Integer knowledgeId = intVal(body, "knowledgeId", null);
    int total = intVal(body, "total", DEFAULT_QUESTION_COUNT);
    if (total <= 0) {
      total = DEFAULT_QUESTION_COUNT;
    }
    total = Math.min(Math.max(total, 10), 50);

    Set<Integer> preferKids = parsePreferKnowledgeIds(body);
    if (knowledgeId != null && knowledgeId > 0) {
      preferKids.add(knowledgeId);
    }
    // 同步学红卡/星标考点：只作加权信号，答卷仍写通道 A
    Set<Integer> syncBoostKids = loadSyncBoostKnowledgeIds(ctx.classId(), userId);
    preferKids.addAll(syncBoostKids);

    List<Integer> wrongIds = answerLogMapper.findWrongQuestionIds(tenantId, userId, knowledgeId, 120);
    Map<Integer, ExamExamDetailEntity> wrongById = wrongIds.isEmpty()
        ? Map.of()
        : examDetailMapper.selectBatchIds(wrongIds).stream()
            .filter(e -> e.getDeleted() == null || e.getDeleted() == 0)
            .collect(Collectors.toMap(ExamExamDetailEntity::getId, e -> e, (a, b) -> a, LinkedHashMap::new));

    List<ExamExamDetailEntity> seedWrongs = new ArrayList<>();
    Map<String, Integer> bucketSeedCount = new HashMap<>();
    Set<String> typeBuckets = new LinkedHashSet<>();
    Set<Integer> knowledgeBuckets = new LinkedHashSet<>();
    for (Integer id : wrongIds) {
      ExamExamDetailEntity q = wrongById.get(id);
      if (q == null) {
        continue;
      }
      seedWrongs.add(q);
      String type = normalizeQuestionType(q.getType());
      if (q.getKnowledgeId() != null && StringUtils.hasText(type)) {
        String bucket = q.getKnowledgeId() + "|" + type;
        typeBuckets.add(bucket);
        knowledgeBuckets.add(q.getKnowledgeId());
        bucketSeedCount.merge(bucket, 1, Integer::sum);
      }
    }
    seedWrongs = applyPracticeScopeFilter(seedWrongs, ctx);
    knowledgeBuckets.addAll(preferKids);

    Map<Integer, Double> knowledgeWrongRate = loadKnowledgeWrongRateMap(tenantId, userId);
    Map<String, Double> typeWrongRate = loadTypeWrongRateMap(tenantId, userId);

    // prefer 考点无其最弱题型补桶，保证「未掌握知识点」也能进主配额
    for (Integer kid : preferKids) {
      if (kid == null || kid <= 0) {
        continue;
      }
      String weakType = weakestTypeForKnowledge(kid, typeWrongRate, typeBuckets);
      String bucket = kid + "|" + weakType;
      typeBuckets.add(bucket);
      knowledgeBuckets.add(kid);
      bucketSeedCount.putIfAbsent(bucket, 0);
    }

    Map<String, Double> bucketScore = new HashMap<>();
    for (String bucket : typeBuckets) {
      bucketScore.put(bucket,
          scoreBucket(bucket, bucketSeedCount, knowledgeWrongRate, typeWrongRate, preferKids, syncBoostKids));
    }

    Integer courseId = ctx.courseId();
    List<ExamExamDetailEntity> pool = loadScopedQuestionPool(tenantId, ctx, courseId, null);
    Set<Integer> seedIds = seedWrongs.stream()
        .map(ExamExamDetailEntity::getId)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toCollection(HashSet::new));
    Set<Integer> answeredIds = new HashSet<>(
        answerLogMapper.findAnsweredQuestionIds(tenantId, userId));

    Map<String, List<ExamExamDetailEntity>> similarByBucket = new HashMap<>();
    for (ExamExamDetailEntity q : pool) {
      if (q.getId() == null || seedIds.contains(q.getId())) {
        continue;
      }
      if (!matchSimilarBucket(q, knowledgeId, typeBuckets, knowledgeBuckets)) {
        continue;
      }
      String type = normalizeQuestionType(q.getType());
      if (q.getKnowledgeId() == null || !StringUtils.hasText(type)) {
        continue;
      }
      String bucket = q.getKnowledgeId() + "|" + type;
      similarByBucket.computeIfAbsent(bucket, k -> new ArrayList<>()).add(q);
      bucketScore.putIfAbsent(bucket,
          scoreBucket(bucket, bucketSeedCount, knowledgeWrongRate, typeWrongRate, preferKids, syncBoostKids));
    }

    List<String> orderedBuckets = new ArrayList<>(bucketScore.keySet());
    orderedBuckets.sort((a, b) -> {
      int c = Double.compare(bucketScore.getOrDefault(b, 0d), bucketScore.getOrDefault(a, 0d));
      if (c != 0) {
        return c;
      }
      return Integer.compare(bucketSeedCount.getOrDefault(b, 0), bucketSeedCount.getOrDefault(a, 0));
    });

    List<ExamExamDetailEntity> picked = new ArrayList<>();
    Set<Integer> pickedIds = new HashSet<>();
    int primaryQuota = Math.max(1, (int) Math.ceil(total * 0.6));
    // 先掺入部分原错题，答对可直接出本，避免整包都是新题导致错题本不动
    int seedQuota = Math.min(rankedSeedLimit(seedWrongs.size(), total), total);
    List<ExamExamDetailEntity> earlySeeds = new ArrayList<>(seedWrongs);
    earlySeeds.sort((a, b) -> {
      double ar = a.getKnowledgeId() == null ? 0 : knowledgeWrongRate.getOrDefault(a.getKnowledgeId(), 0d);
      double br = b.getKnowledgeId() == null ? 0 : knowledgeWrongRate.getOrDefault(b.getKnowledgeId(), 0d);
      return Double.compare(br, ar);
    });
    for (ExamExamDetailEntity q : earlySeeds) {
      if (picked.size() >= seedQuota) {
        break;
      }
      if (q.getId() != null && pickedIds.add(q.getId())) {
        picked.add(q);
      }
    }

    // 主配额：高分桶优先，桶内未做过 > 做过
    for (String bucket : orderedBuckets) {
      if (picked.size() >= primaryQuota) {
        break;
      }
      List<ExamExamDetailEntity> cands = similarByBucket.getOrDefault(bucket, List.of());
      pickWeightedFromCandidates(cands, answeredIds, seedIds, picked, pickedIds,
          Math.min(primaryQuota - picked.size(), Math.max(2, primaryQuota / Math.max(1, Math.min(5, orderedBuckets.size())))));
    }
    // 补满主配额
    for (String bucket : orderedBuckets) {
      if (picked.size() >= primaryQuota) {
        break;
      }
      pickWeightedFromCandidates(similarByBucket.getOrDefault(bucket, List.of()),
          answeredIds, seedIds, picked, pickedIds, primaryQuota - picked.size());
    }
    // 剩余题量：其余桶
    for (String bucket : orderedBuckets) {
      if (picked.size() >= total) {
        break;
      }
      pickWeightedFromCandidates(similarByBucket.getOrDefault(bucket, List.of()),
          answeredIds, seedIds, picked, pickedIds, total - picked.size());
    }

    // 同型新题不足：按考点错误率回填原错题
    List<ExamExamDetailEntity> rankedSeeds = new ArrayList<>(seedWrongs);
    rankedSeeds.sort((a, b) -> {
      double ar = a.getKnowledgeId() == null ? 0 : knowledgeWrongRate.getOrDefault(a.getKnowledgeId(), 0d);
      double br = b.getKnowledgeId() == null ? 0 : knowledgeWrongRate.getOrDefault(b.getKnowledgeId(), 0d);
      int c = Double.compare(br, ar);
      if (c != 0) {
        return c;
      }
      boolean ap = a.getKnowledgeId() != null && preferKids.contains(a.getKnowledgeId());
      boolean bp = b.getKnowledgeId() != null && preferKids.contains(b.getKnowledgeId());
      return Boolean.compare(bp, ap);
    });
    for (ExamExamDetailEntity q : rankedSeeds) {
      if (picked.size() >= total) {
        break;
      }
      if (q.getId() != null && pickedIds.add(q.getId())) {
        picked.add(q);
      }
    }

    if (picked.size() < total && !knowledgeBuckets.isEmpty()) {
      List<ExamExamDetailEntity> sameKnowledge = pool.stream()
          .filter(q -> q.getId() != null && !pickedIds.contains(q.getId()))
          .filter(q -> q.getKnowledgeId() != null && knowledgeBuckets.contains(q.getKnowledgeId()))
          .sorted((a, b) -> {
            boolean au = !answeredIds.contains(a.getId());
            boolean bu = !answeredIds.contains(b.getId());
            return Boolean.compare(bu, au);
          })
          .collect(Collectors.toCollection(ArrayList::new));
      for (ExamExamDetailEntity q : sameKnowledge) {
        if (picked.size() >= total) {
          break;
        }
        if (pickedIds.add(q.getId())) {
          picked.add(q);
        }
      }
    }

    if (picked.isEmpty()) {
      throw new AppSmsException(-1, "暂无同类型可推练习题，请先刷题积累错题");
    }

    Map<String, Object> packFocus = buildPackFocus(picked, knowledgeWrongRate, typeWrongRate, preferKids, syncBoostKids);
    Map<String, Object> view = persistPaper(user, "薄弱同型强化", "drill", picked, 0, ctx);
    view.put("packFocus", packFocus);
    view.put("syncBoostKnowledgeCount", syncBoostKids.size());
    return view;
  }

  private static int rankedSeedLimit(int seedSize, int total) {
    if (seedSize <= 0 || total <= 0) {
      return 0;
    }
    // 约 30% 原错题，至少 3 题（有则），至多一半
    int n = Math.max(3, (int) Math.ceil(total * 0.3));
    return Math.min(seedSize, Math.min(n, Math.max(1, total / 2)));
  }

  private static Set<Integer> parsePreferKnowledgeIds(Map<String, Object> body) {
    Set<Integer> out = new LinkedHashSet<>();
    if (body == null) {
      return out;
    }
    Object raw = body.get("preferKnowledgeIds");
    if (raw == null) {
      raw = body.get("knowledgeIds");
    }
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
    return out;
  }

  private Map<Integer, Double> loadKnowledgeWrongRateMap(Long tenantId, Long userId) {
    Map<Integer, Double> out = new HashMap<>();
    for (Map<String, Object> s : answerLogMapper.wrongStatsByKnowledge(tenantId, userId)) {
      Integer kid = toInt(s.get("knowledgeId"));
      if (kid == null) {
        continue;
      }
      int wrong = toIntOrZero(s.get("wrongCount"));
      int tot = toIntOrZero(s.get("totalCount"));
      out.put(kid, tot == 0 ? 0d : wrong * 100.0 / tot);
    }
    return out;
  }

  private Map<String, Double> loadTypeWrongRateMap(Long tenantId, Long userId) {
    Map<String, Double> out = new HashMap<>();
    for (Map<String, Object> s : answerLogMapper.wrongStatsByQuestionType(tenantId, userId)) {
      String type = normalizeQuestionType(s.get("questionType") == null ? null : String.valueOf(s.get("questionType")));
      if (!StringUtils.hasText(type)) {
        type = "unknown";
      }
      int wrong = toIntOrZero(s.get("wrongCount"));
      int tot = toIntOrZero(s.get("totalCount"));
      out.put(type, tot == 0 ? 0d : wrong * 100.0 / tot);
    }
    return out;
  }

  private static String weakestTypeForKnowledge(
      Integer kid, Map<String, Double> typeWrongRate, Set<String> existingBuckets) {
    String best = "single";
    double bestRate = -1;
    for (String t : List.of("single", "multi", "judge")) {
      if (existingBuckets.contains(kid + "|" + t)) {
        return t;
      }
      double r = typeWrongRate.getOrDefault(t, 0d);
      if (r > bestRate) {
        bestRate = r;
        best = t;
      }
    }
    return best;
  }

  /** package-visible：单测校验弱项加权 */
  static double scoreBucket(
      String bucket,
      Map<String, Integer> bucketSeedCount,
      Map<Integer, Double> knowledgeWrongRate,
      Map<String, Double> typeWrongRate,
      Set<Integer> preferKids,
      Set<Integer> syncBoostKids) {
    String[] parts = bucket.split("\\|", 2);
    Integer kid = toInt(parts[0]);
    String type = parts.length > 1 ? parts[1] : "unknown";
    int seeds = bucketSeedCount.getOrDefault(bucket, 0);
    double kr = kid == null ? 0 : knowledgeWrongRate.getOrDefault(kid, 0d);
    double tr = typeWrongRate.getOrDefault(type, 0d);
    double prefer = kid != null && preferKids.contains(kid) ? 40 : 0;
    double syncBoost = kid != null && syncBoostKids != null && syncBoostKids.contains(kid) ? 50 : 0;
    double weakBoost = kr >= 60 ? 30 : (kr >= 30 ? 15 : 0);
    return seeds * 12.0 + kr * 0.5 + tr * 0.35 + prefer + syncBoost + weakBoost;
  }

  /**
   * 本班同步学红卡/星标对应考点（只读加权；不读 C 答卷写入 A）。
   */
  private Set<Integer> loadSyncBoostKnowledgeIds(Long classId, Long platformUserId) {
    Set<Integer> out = new LinkedHashSet<>();
    if (classId == null || classId <= 0 || platformUserId == null || platformUserId <= 0) {
      return out;
    }
    try {
      TrainClassReviewSyncEntity cfg = classReviewSyncMapper.selectOne(
          new LambdaQueryWrapper<TrainClassReviewSyncEntity>()
              .eq(TrainClassReviewSyncEntity::getClassId, classId)
              .eq(TrainClassReviewSyncEntity::getEnabled, 1)
              .eq(TrainClassReviewSyncEntity::getDeleted, 0)
              .last("LIMIT 1"));
      if (cfg == null || cfg.getExamId() == null || cfg.getExamId() <= 0) {
        return out;
      }
      AiReviewPlanEntity plan = reviewPlanMapper.selectOne(new LambdaQueryWrapper<AiReviewPlanEntity>()
          .eq(AiReviewPlanEntity::getPlatformUserId, platformUserId)
          .eq(AiReviewPlanEntity::getExamId, cfg.getExamId())
          .ne(AiReviewPlanEntity::getStatus, "abandoned")
          .last("LIMIT 1"));
      if (plan == null || plan.getId() == null) {
        return out;
      }
      List<AiReviewWrongItemEntity> rows = reviewWrongItemMapper.selectList(
          new LambdaQueryWrapper<AiReviewWrongItemEntity>()
              .eq(AiReviewWrongItemEntity::getPlanId, plan.getId())
              .and(w -> w.eq(AiReviewWrongItemEntity::getRemoved, 0)
                  .or()
                  .isNull(AiReviewWrongItemEntity::getRemoved))
              .and(w -> w.eq(AiReviewWrongItemEntity::getCardLevel, "red")
                  .or()
                  .gt(AiReviewWrongItemEntity::getStar, 0)));
      for (AiReviewWrongItemEntity row : rows) {
        if (row.getKnowledgeId() != null && row.getKnowledgeId() > 0) {
          out.add(row.getKnowledgeId().intValue());
        }
      }
    } catch (Exception e) {
      // 同步学加权失败不影响专项推题主路径
    }
    return out;
  }

  private static void pickWeightedFromCandidates(
      List<ExamExamDetailEntity> cands,
      Set<Integer> answeredIds,
      Set<Integer> seedIds,
      List<ExamExamDetailEntity> picked,
      Set<Integer> pickedIds,
      int need) {
    if (need <= 0 || cands == null || cands.isEmpty()) {
      return;
    }
    List<ExamExamDetailEntity> fresh = new ArrayList<>();
    List<ExamExamDetailEntity> seen = new ArrayList<>();
    for (ExamExamDetailEntity q : cands) {
      if (q.getId() == null || pickedIds.contains(q.getId()) || seedIds.contains(q.getId())) {
        continue;
      }
      if (answeredIds.contains(q.getId())) {
        seen.add(q);
      } else {
        fresh.add(q);
      }
    }
    Collections.shuffle(fresh);
    Collections.shuffle(seen);
    for (ExamExamDetailEntity q : fresh) {
      if (need <= 0) {
        return;
      }
      if (pickedIds.add(q.getId())) {
        picked.add(q);
        need--;
      }
    }
    for (ExamExamDetailEntity q : seen) {
      if (need <= 0) {
        return;
      }
      if (pickedIds.add(q.getId())) {
        picked.add(q);
        need--;
      }
    }
  }

  private Map<String, Object> buildPackFocus(
      List<ExamExamDetailEntity> picked,
      Map<Integer, Double> knowledgeWrongRate,
      Map<String, Double> typeWrongRate,
      Set<Integer> preferKids,
      Set<Integer> syncBoostKids) {
    Map<Integer, Integer> kidCount = new HashMap<>();
    Map<String, Integer> typeCount = new HashMap<>();
    for (ExamExamDetailEntity q : picked) {
      if (q.getKnowledgeId() != null) {
        kidCount.merge(q.getKnowledgeId(), 1, Integer::sum);
      }
      String t = normalizeQuestionType(q.getType());
      if (StringUtils.hasText(t)) {
        typeCount.merge(t, 1, Integer::sum);
      }
    }
    Integer topKid = kidCount.entrySet().stream()
        .max(Comparator
            .comparingDouble((Map.Entry<Integer, Integer> e) ->
                knowledgeWrongRate.getOrDefault(e.getKey(), 0d)
                    + (preferKids.contains(e.getKey()) ? 20 : 0)
                    + (syncBoostKids != null && syncBoostKids.contains(e.getKey()) ? 25 : 0)
                    + e.getValue())
            .thenComparingInt(Map.Entry::getValue))
        .map(Map.Entry::getKey)
        .orElse(null);
    String topType = typeCount.entrySet().stream()
        .max(Comparator
            .comparingDouble((Map.Entry<String, Integer> e) ->
                typeWrongRate.getOrDefault(e.getKey(), 0d) + e.getValue())
            .thenComparingInt(Map.Entry::getValue))
        .map(Map.Entry::getKey)
        .orElse(null);

    String knowledgeName = null;
    if (topKid != null) {
      knowledgeName = loadKnowledgeTitle(List.of(topKid)).getOrDefault(topKid, "薄弱考点");
    }
    String typeName = topType == null ? null : questionTypeLabel(topType);
    String hint;
    if (knowledgeName != null && typeName != null) {
      hint = "本包侧重：" + knowledgeName + " · " + typeName;
    } else if (knowledgeName != null) {
      hint = "本包侧重：" + knowledgeName;
    } else if (typeName != null) {
      hint = "本包侧重：" + typeName;
    } else {
      hint = "按你的错题推送同类型题";
    }
    Map<String, Object> focus = new LinkedHashMap<>();
    focus.put("knowledgeId", topKid);
    focus.put("knowledgeName", knowledgeName);
    focus.put("type", topType);
    focus.put("typeName", typeName);
    focus.put("hint", hint);
    focus.put("questionCount", picked.size());
    return focus;
  }

  /**
   * 同型匹配：优先「考点+题型」与错题一致；无错题桶时按指定 knowledgeId；
   * 仅有考点桶时退化为同考点。
   */
  private static boolean matchSimilarBucket(
      ExamExamDetailEntity q,
      Integer focusKnowledgeId,
      Set<String> typeBuckets,
      Set<Integer> knowledgeBuckets) {
    if (q.getKnowledgeId() == null) {
      return false;
    }
    if (focusKnowledgeId != null && focusKnowledgeId > 0 && !focusKnowledgeId.equals(q.getKnowledgeId())) {
      return false;
    }
    String type = normalizeQuestionType(q.getType());
    if (!typeBuckets.isEmpty()) {
      return typeBuckets.contains(q.getKnowledgeId() + "|" + type);
    }
    return knowledgeBuckets.contains(q.getKnowledgeId());
  }

  /**
   * 练习答对出本：清除本题错记；drill/precise 时同考点同题型错题一并出本
   * （同型强化答对新题时，错题本数字才会随之下降）。
   */
  private int clearWrongBookOnPracticeCorrect(
      Long tenantId, Long userId, ExamExamDetailEntity q, String modeRaw) {
    if (q == null || q.getId() == null || tenantId == null || userId == null) {
      return 0;
    }
    String mode = modeRaw == null ? "" : modeRaw.trim().toLowerCase(Locale.ROOT);
    if (!isWrongBookClearMode(mode)) {
      return 0;
    }
    int deleted = answerLogMapper.deleteWrongLogsByQuestion(tenantId, userId, q.getId());
    if (!("drill".equals(mode) || "precise".equals(mode))) {
      return deleted;
    }
    if (q.getKnowledgeId() == null) {
      return deleted;
    }
    String type = normalizeQuestionType(q.getType());
    if (!StringUtils.hasText(type)) {
      return deleted;
    }
    List<Integer> wrongIds = answerLogMapper.findWrongQuestionIds(tenantId, userId, q.getKnowledgeId(), 60);
    if (wrongIds.isEmpty()) {
      return deleted;
    }
    Map<Integer, ExamExamDetailEntity> byId = examDetailMapper.selectBatchIds(wrongIds).stream()
        .filter(e -> e.getDeleted() == null || e.getDeleted() == 0)
        .collect(Collectors.toMap(ExamExamDetailEntity::getId, e -> e, (a, b) -> a));
    for (Integer wid : wrongIds) {
      if (wid == null || wid.equals(q.getId())) {
        continue;
      }
      ExamExamDetailEntity other = byId.get(wid);
      if (other == null) {
        continue;
      }
      if (type.equals(normalizeQuestionType(other.getType()))) {
        deleted += answerLogMapper.deleteWrongLogsByQuestion(tenantId, userId, wid);
      }
    }
    return deleted;
  }

  private static boolean isWrongBookClearMode(String mode) {
    if (!StringUtils.hasText(mode)) {
      return false;
    }
    return "wrong".equals(mode)
        || "drill".equals(mode)
        || "precise".equals(mode)
        || "random".equals(mode)
        || mode.startsWith("seq")
        || mode.contains("practice")
        || mode.contains("drill");
  }

  /** 精准学练后效果：近 7 日错误率 + 最近一次 drill 正确率 */
  public Map<String, Object> precisePracticeEffect() {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = resolveExamTenantId(user);
    Long userId = user.getUserId();
    LocalDateTime weekFrom = LocalDateTime.now().minusDays(7);
    LocalDateTime prevFrom = LocalDateTime.now().minusDays(14);

    Map<String, Object> week = answerLogMapper.answerStatsSince(tenantId, userId, weekFrom);
    Map<String, Object> prev = answerLogMapper.answerStatsSince(tenantId, userId, prevFrom);
    int weekWrong = toIntOrZero(week == null ? null : week.get("wrongCount"));
    int weekTotal = toIntOrZero(week == null ? null : week.get("totalCount"));
    int prevWrong = toIntOrZero(prev == null ? null : prev.get("wrongCount"));
    int prevTotal = toIntOrZero(prev == null ? null : prev.get("totalCount"));
    // prev 窗口含本周，差集近似上周
    int lastWeekWrong = Math.max(0, prevWrong - weekWrong);
    int lastWeekTotal = Math.max(0, prevTotal - weekTotal);
    double weekRate = weekTotal == 0 ? 0 : Math.round(weekWrong * 1000.0 / weekTotal) / 10.0;
    double lastWeekRate = lastWeekTotal == 0 ? 0 : Math.round(lastWeekWrong * 1000.0 / lastWeekTotal) / 10.0;
    Double delta = lastWeekTotal == 0 ? null : Math.round((weekRate - lastWeekRate) * 10) / 10.0;

    long cleared = 0L;
    try {
      cleared = answerLogMapper.countClearedWrongSince(tenantId, userId, weekFrom);
    } catch (Exception ignored) {
      cleared = 0L;
    }

    AppExamMockPaperEntity lastDrill = mockPaperMapper.selectOne(
        new LambdaQueryWrapper<AppExamMockPaperEntity>()
            .eq(AppExamMockPaperEntity::getTenantId, tenantId)
            .eq(AppExamMockPaperEntity::getUserId, userId)
            .eq(AppExamMockPaperEntity::getMode, "drill")
            .eq(AppExamMockPaperEntity::getStatus, "done")
            .orderByDesc(AppExamMockPaperEntity::getId)
            .last("LIMIT 1"));

    Map<String, Object> effect = new LinkedHashMap<>();
    effect.put("weekWrongRate", weekRate);
    effect.put("weekWrongRateDelta", delta);
    effect.put("weekAnswered", weekTotal);
    effect.put("weekWrong", weekWrong);
    effect.put("clearedWrongCount", cleared);
    if (lastDrill != null) {
      int right = lastDrill.getRightCount() == null ? 0 : lastDrill.getRightCount();
      int cnt = lastDrill.getTotalCount() == null ? 0 : lastDrill.getTotalCount();
      // 兼容旧卷未写 rightCount：用 score/totalPoint 粗算
      if (cnt > 0 && lastDrill.getRightCount() == null && lastDrill.getScore() != null
          && lastDrill.getTotalPoint() != null && lastDrill.getTotalPoint() > 0) {
        right = (int) Math.round(cnt * (lastDrill.getScore() * 1.0 / lastDrill.getTotalPoint()));
      }
      double acc = cnt == 0 ? 0 : Math.round(right * 1000.0 / cnt) / 10.0;
      effect.put("lastPackAccuracy", acc);
      effect.put("lastPackRight", right);
      effect.put("lastPackTotal", cnt);
      effect.put("lastPackPaperId", lastDrill.getId());
    } else {
      effect.put("lastPackAccuracy", null);
    }
    return effect;
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
    if (right) {
      deleted = clearWrongBookOnPracticeCorrect(tenantId, user.getUserId(), q, mode);
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
    out.put("removed", deleted > 0);
    out.put("removedFromWrong", deleted > 0);
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

  /** 按题型统计答题/错题与错误率（精准学 Hub） */
  public List<Map<String, Object>> wrongStatsByQuestionType() {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = resolveExamTenantId(user);
    Long userId = user.getUserId();
    List<Map<String, Object>> rows = answerLogMapper.wrongStatsByQuestionType(tenantId, userId);
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> s : rows) {
      String rawType = s.get("questionType") == null ? "unknown" : String.valueOf(s.get("questionType"));
      String type = normalizeQuestionType(rawType);
      if (type == null || type.isBlank()) {
        type = "unknown";
      }
      int wrong = toIntOrZero(s.get("wrongCount"));
      int tot = toIntOrZero(s.get("totalCount"));
      double wrongRate = tot == 0 ? 0 : Math.round(wrong * 1000.0 / tot) / 10.0;
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("type", type);
      item.put("typeName", questionTypeLabel(type));
      item.put("wrongCount", wrong);
      item.put("totalCount", tot);
      item.put("wrongRate", wrongRate);
      out.add(item);
    }
    out.sort(Comparator.comparingInt(a -> typeSortKey(String.valueOf(a.get("type")))));
    return out;
  }

  private static String questionTypeLabel(String type) {
    return switch (type == null ? "" : type) {
      case "single" -> "单选题";
      case "multi" -> "多选题";
      case "judge" -> "判断题";
      case "subjective" -> "主观题";
      default -> "其他题型";
    };
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

    // total 必须全量统计，不能用带 LIMIT 的列表 size（否则 Hub 会卡在 limit=8）
    long total = answerLogMapper.countWrongQuestionIds(tenantId, userId, knowledgeId);
    if (total <= 0) {
      return Map.of("list", List.of(), "total", 0, "page", page, "size", size);
    }
    List<Integer> wrongIds = answerLogMapper.findWrongQuestionIds(tenantId, userId, knowledgeId, offset + size);
    List<Integer> pageIds = wrongIds.isEmpty()
        ? List.of()
        : wrongIds.subList(Math.min(offset, wrongIds.size()), Math.min(offset + size, wrongIds.size()));
    List<ExamExamDetailEntity> questions = pageIds.isEmpty()
        ? List.of()
        : examDetailMapper.selectBatchIds(pageIds).stream()
            .filter(e -> e.getDeleted() == 0)
            .collect(Collectors.toList());

    List<Map<String, Object>> list = new ArrayList<>();
    for (ExamExamDetailEntity q : questions) {
      list.add(toQuestionView(q, true));
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("list", list);
    out.put("total", total);
    out.put("page", page);
    out.put("size", size);
    return out;
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
        clearWrongBookOnPracticeCorrect(
            resolveExamTenantId(user), user.getUserId(), q, String.valueOf(paper.getMode()));
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
      boolean showAnswer = "done".equals(paper.getStatus())
          || isPracticeInstantMode(paper.getMode())
          || (paper.getClassId() != null && paper.getClassId() > 0);
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

  /**
   * 练习即时反馈：除模拟考 mock 外，random / wrong / drill / seq_* 等均下发答案与解析。
   */
  private static boolean isPracticeInstantMode(String mode) {
    if (!StringUtils.hasText(mode)) {
      return false;
    }
    String m = mode.trim().toLowerCase(Locale.ROOT);
    if ("mock".equals(m)) {
      return false;
    }
    return "random".equals(m)
        || "wrong".equals(m)
        || "drill".equals(m)
        || "precise".equals(m)
        || m.startsWith("seq")
        || m.contains("practice")
        || m.contains("drill");
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
      String resolve = q.getResolve();
      if (!StringUtils.hasText(resolve)) {
        // 兼容：部分题解析写在 answer JSON 根字段 analysis/resolve
        resolve = extractResolveFromAnswerJson(q.getAnswer());
      }
      m.put("resolve", resolve);
      m.put("analysis", resolve);
    }
    return m;
  }

  /** 从 answer JSON 根节点尝试读取解析文案（题库历史脏数据兼容）。 */
  @SuppressWarnings("unchecked")
  private String extractResolveFromAnswerJson(String answerRaw) {
    if (!StringUtils.hasText(answerRaw)) {
      return null;
    }
    String s = answerRaw.trim();
    if (!s.startsWith("{")) {
      return null;
    }
    try {
      Map<String, Object> root = JSON.readValue(s, new TypeReference<Map<String, Object>>() {});
      for (String key : List.of("resolve", "analysis", "jiexi", "explain", "解析")) {
        Object v = root.get(key);
        if (v != null && StringUtils.hasText(String.valueOf(v))) {
          return String.valueOf(v).trim();
        }
      }
    } catch (Exception ignored) {
      // ignore
    }
    return null;
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
