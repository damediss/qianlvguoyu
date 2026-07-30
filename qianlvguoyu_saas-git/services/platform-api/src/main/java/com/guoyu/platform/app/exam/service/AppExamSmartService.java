package com.guoyu.platform.app.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.exam.entity.AppExamMockPaperEntity;
import com.guoyu.platform.app.exam.entity.AppExamUserAnswerLogEntity;
import com.guoyu.platform.app.exam.mapper.AppExamMockPaperMapper;
import com.guoyu.platform.app.exam.mapper.AppExamUserAnswerLogMapper;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  public AppExamSmartService(ExamExamDetailMapper examDetailMapper,
                             ExamKnowledgeStructureMapper knowledgeMapper,
                             AppExamMockPaperMapper mockPaperMapper,
                             AppExamUserAnswerLogMapper answerLogMapper) {
    this.examDetailMapper = examDetailMapper;
    this.knowledgeMapper = knowledgeMapper;
    this.mockPaperMapper = mockPaperMapper;
    this.answerLogMapper = answerLogMapper;
  }

  // ============================================================
  // 1. AI 组卷：按考点×难度智能抽题，生成一张模拟卷
  // ============================================================
  public Map<String, Object> generatePaper(Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = user.getTenantId();
    Long userId = user.getUserId();

    Integer courseId = intVal(body, "courseId", null);
    Integer total = intVal(body, "total", DEFAULT_QUESTION_COUNT);
    Integer timeLimit = intVal(body, "timeLimit", DEFAULT_TIME_LIMIT_MIN);
    Integer difficultyMax = intVal(body, "difficultyMax", 5);
    String title = strVal(body, "title", "智能组卷");

    // 1) 抽题候选：按课程/考点过滤
    LambdaQueryWrapper<ExamExamDetailEntity> q = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId.intValue())
        .eq(ExamExamDetailEntity::getDeleted, 0)
        .eq(ExamExamDetailEntity::getStatus, 1)
        .le(ExamExamDetailEntity::getDifficulty, difficultyMax);
    if (courseId != null) {
      q.eq(ExamExamDetailEntity::getExamId, courseId);
    }
    List<ExamExamDetailEntity> pool = examDetailMapper.selectList(q);
    if (pool.isEmpty()) {
      throw new AppSmsException(-1, "题库为空，无法组卷");
    }

    // 2) 按考点分组
    Map<Integer, List<ExamExamDetailEntity>> byKnowledge = pool.stream()
        .filter(e -> e.getKnowledgeId() != null)
        .collect(Collectors.groupingBy(ExamExamDetailEntity::getKnowledgeId));
    List<Integer> knowledgeIds = new ArrayList<>(byKnowledge.keySet());

    // 3) 分层抽样：按考点数量平均分配题量，剩余随机补足
    List<ExamExamDetailEntity> picked = new ArrayList<>();
    if (!knowledgeIds.isEmpty() && total > 0) {
      int perKnowledge = Math.max(1, total / knowledgeIds.size());
      for (Integer kid : knowledgeIds) {
        List<ExamExamDetailEntity> ks = new ArrayList<>(byKnowledge.get(kid));
        Collections.shuffle(ks);
        picked.addAll(ks.subList(0, Math.min(perKnowledge, ks.size())));
      }
    }
    // 不归属考点的题目作随机补充池
    List<ExamExamDetailEntity> orphan = pool.stream()
        .filter(e -> e.getKnowledgeId() == null).collect(Collectors.toList());
    Collections.shuffle(orphan);
    int i = 0;
    while (picked.size() < total && i < orphan.size()) {
      picked.add(orphan.get(i++));
    }
    // 仍不足则从已选池重复随机（保证题量）
    while (picked.size() < total && picked.size() < pool.size()) {
      picked.add(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
    }
    if (picked.size() > total) {
      picked = new ArrayList<>(picked.subList(0, total));
    }

    return persistPaper(user, title, "mock", picked, timeLimit);
  }

  // ============================================================
  // 2. 随机练习：不生成试卷，直接返回题目
  // ============================================================
  public Map<String, Object> randomPractice(Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    Integer courseId = intVal(body, "courseId", null);
    Integer total = intVal(body, "total", DEFAULT_QUESTION_COUNT);

    LambdaQueryWrapper<ExamExamDetailEntity> q = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, user.getTenantId().intValue())
        .eq(ExamExamDetailEntity::getDeleted, 0)
        .eq(ExamExamDetailEntity::getStatus, 1);
    if (courseId != null) {
      q.eq(ExamExamDetailEntity::getExamId, courseId);
    }
    List<ExamExamDetailEntity> pool = examDetailMapper.selectList(q);
    if (pool.isEmpty()) {
      throw new AppSmsException(-1, "题库为空");
    }
    Collections.shuffle(pool);
    List<ExamExamDetailEntity> picked = pool.subList(0, Math.min(total, pool.size()));
    return persistPaper(user, "随机练习", "random", picked, 0);
  }

  // ============================================================
  // 3. 错题重练：把用户历史错题组成一张卷
  // ============================================================
  public Map<String, Object> wrongReplay(Map<String, Object> body) {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = user.getTenantId();
    Long userId = user.getUserId();

    Integer knowledgeId = intVal(body, "knowledgeId", null);
    Integer limit = intVal(body, "limit", WRONG_REPLAY_LIMIT);

    List<Integer> wrongIds = answerLogMapper.findWrongQuestionIds(tenantId, userId, knowledgeId, limit);
    if (wrongIds.isEmpty()) {
      throw new AppSmsException(-1, "暂无错题，快去刷题吧");
    }
    List<ExamExamDetailEntity> questions = examDetailMapper.selectBatchIds(wrongIds).stream()
        .filter(e -> e.getDeleted() == 0)
        .collect(Collectors.toList());
    return persistPaper(user, "错题重练", "wrong", questions, 0);
  }

  // ============================================================
  // 4. 考点预测：基于历史答题分析薄弱考点
  // ============================================================
  public Map<String, Object> knowledgePredict() {
    PlatformUserPrincipal user = requireLogin();
    Long tenantId = user.getTenantId();
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
    Long tenantId = user.getTenantId();
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
    for (Integer qid : qIds) {
      ExamExamDetailEntity q = qMap.get(qid);
      if (q == null) continue;
      Map<String, Object> ans = answerMap.get(qid);
      String userAnswer = ans == null ? "" : strVal(ans, "answer", "");
      long spendMs = ans == null ? 0 : longVal(ans, "spendMs", 0L);
      boolean right = StringUtils.hasText(userAnswer) && userAnswer.trim().equalsIgnoreCase(
          q.getAnswer() == null ? "" : q.getAnswer().trim());
      if (right) {
        rightCount++;
        if (q.getScore() != null) {
          score = score.add(q.getScore());
        }
      }
      // 写答题明细
      AppExamUserAnswerLogEntity log = new AppExamUserAnswerLogEntity();
      log.setUserId(user.getUserId());
      log.setTenantId(user.getTenantId());
      log.setQuestionId(q.getId());
      log.setKnowledgeId(q.getKnowledgeId());
      log.setQuestionType(q.getType());
      log.setUserAnswer(userAnswer);
      log.setIsRight(right ? 1 : 0);
      log.setSource(paper.getMode());
      log.setPaperId(paper.getId());
      log.setSpendMs(spendMs);
      log.setCreateTime(now);
      answerLogMapper.insert(log);

      Map<String, Object> d = new LinkedHashMap<>();
      d.put("questionId", q.getId());
      d.put("isRight", right ? 1 : 0);
      d.put("userAnswer", userAnswer);
      d.put("rightAnswer", q.getAnswer());
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
        .eq(AppExamMockPaperEntity::getTenantId, user.getTenantId())
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
                                           List<ExamExamDetailEntity> questions, int timeLimit) {
    AppExamMockPaperEntity paper = new AppExamMockPaperEntity();
    paper.setUserId(user.getUserId());
    paper.setTenantId(user.getTenantId());
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
    mockPaperMapper.insert(paper);
    return paperToView(paper, true);
  }

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
      // 已交卷显示答案，未交卷不显示
      boolean showAnswer = "done".equals(paper.getStatus());
      data.put("questions", questions.stream().map(q -> toQuestionView(q, showAnswer)).collect(Collectors.toList()));
    }
    return data;
  }

  private Map<String, Object> toQuestionView(ExamExamDetailEntity q, boolean showAnswer) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("questionId", q.getId());
    m.put("type", q.getType());
    m.put("knowledgeId", q.getKnowledgeId());
    m.put("difficulty", q.getDifficulty());
    m.put("score", q.getScore());
    // 拆分题干和选项：content 格式 "题干\nA. xxx\nB. yyy"
    String[] parts = splitStemAndOptions(q.getContent());
    m.put("stem", parts[0]);
    m.put("options", parts.length > 1
        ? java.util.Arrays.stream(parts, 1, parts.length).map(this::optionView).collect(Collectors.toList())
        : List.of());
    if (showAnswer) {
      m.put("answer", q.getAnswer());
      m.put("resolve", q.getResolve());
    }
    return m;
  }

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
