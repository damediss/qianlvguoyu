package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.exam.mapper.AppExamUserAnswerLogMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskQuestionReq;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeKnowledgeEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeKnowledgeMapper;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 学员本班刷题覆盖进度：范围内题池 vs exam_user_answer_log（带 class_id）。
 */
@Service
public class ClassPracticeCoverageApplicationService {

  private final ClassPracticeScopeApplicationService scopeService;
  private final AppExamUserAnswerLogMapper answerLogMapper;
  private final TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper;
  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;
  private final ExamExamDetailMapper examDetailMapper;
  private final FollowTaskBankQuestionConverter bankQuestionConverter;

  public ClassPracticeCoverageApplicationService(
      ClassPracticeScopeApplicationService scopeService,
      AppExamUserAnswerLogMapper answerLogMapper,
      TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper,
      ExamKnowledgeStructureMapper knowledgeStructureMapper,
      ExamExamDetailMapper examDetailMapper,
      FollowTaskBankQuestionConverter bankQuestionConverter) {
    this.scopeService = scopeService;
    this.answerLogMapper = answerLogMapper;
    this.practiceKnowledgeMapper = practiceKnowledgeMapper;
    this.knowledgeStructureMapper = knowledgeStructureMapper;
    this.examDetailMapper = examDetailMapper;
    this.bankQuestionConverter = bankQuestionConverter;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getCoverageForAppMember(Long classId, Long userId) {
    if (classId == null || classId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级无效");
    }
    if (userId == null || userId <= 0) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    Long tenantId = TenantContextHolder.getTenantId();
    if (tenantId == null || tenantId <= 0) {
      tenantId = com.guoyu.platform.common.tenant.TenantContextHolder.DEFAULT_TENANT_ID;
    }

    Map<String, Object> scope = scopeService.getScopeForAppMember(classId);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> knowledgeRows =
        scope.get("knowledges") instanceof List
            ? (List<Map<String, Object>>) scope.get("knowledges")
            : List.of();

    List<Integer> courseIds = toIntList(scope.get("courseIds"));
    List<Integer> whitelistIds = knowledgeRows.stream()
        .map(r -> toInt(r.get("knowledgeId")))
        .filter(id -> id != null && id > 0)
        .toList();

    Map<Integer, Set<Integer>> expandedByRoot = buildExpandedByRoot(tenantId, courseIds, whitelistIds);
    Set<Integer> allExpanded = new HashSet<>();
    for (Set<Integer> s : expandedByRoot.values()) {
      allExpanded.addAll(s);
    }

    List<Map<String, Object>> latest =
        answerLogMapper.findLatestAnswersByClassUser(tenantId, userId, classId);
    if (latest == null) {
      latest = List.of();
    }

    // 全班唯一题覆盖（避免白名单父子重叠重复计数）
    Map<String, long[]> globalType = emptyTypeBuckets();
    long globalDone = 0;
    long globalWrong = 0;
    long globalRight = 0;
    Set<Integer> countedQ = new HashSet<>();

    // 叶子知识点直接累计，再 rollup 到白名单根
    Map<Integer, Map<String, long[]>> directDone = new HashMap<>();
    Map<Integer, Map<String, long[]>> directWrong = new HashMap<>();

    for (Map<String, Object> row : latest) {
      Integer qid = toInt(row.get("questionId"));
      Integer kid = toInt(firstPresent(row, "knowledgeId", "knowledge_id"));
      String type = normalizeType(firstPresent(row, "questionType", "question_type", "type"));
      Integer rightVal = toInt(firstPresent(row, "isRight", "is_right"));
      int isRight = rightVal == null ? 0 : rightVal;
      if (qid == null || kid == null || type == null) {
        continue;
      }
      if (!allExpanded.isEmpty() && !allExpanded.contains(kid)) {
        continue;
      }
      if (!countedQ.add(qid)) {
        continue;
      }
      globalDone++;
      globalType.get(type)[0]++;
      if (isRight == 1) {
        globalRight++;
      } else {
        globalWrong++;
        globalType.get(type)[1]++;
        directWrong.computeIfAbsent(kid, k -> emptyTypeBuckets()).get(type)[0]++;
      }
      directDone.computeIfAbsent(kid, k -> emptyTypeBuckets()).get(type)[0]++;
    }

    Map<String, Object> byType = new LinkedHashMap<>();
    @SuppressWarnings("unchecked")
    Map<String, Object> scopeTypeCounts =
        scope.get("typeCounts") instanceof Map
            ? (Map<String, Object>) scope.get("typeCounts")
            : Map.of();
    for (String t : List.of("single", "multi", "judge")) {
      long totalT = toLong(scopeTypeCounts.get(t));
      long doneT = globalType.get(t)[0];
      long wrongT = globalType.get(t)[1];
      byType.put(t, typeStat(totalT, doneT, wrongT));
    }

    long total = toLong(scopeTypeCounts.get("total"));
    if (total <= 0) {
      total = knowledgeRows.stream().mapToLong(r -> toLong(r.get("questionCount"))).sum();
    }
    long todo = Math.max(0, total - globalDone);
    double doneRate = total <= 0 ? 0 : Math.round(globalDone * 1000.0 / total) / 10.0;
    double latestRightRate =
        globalDone <= 0 ? 0 : Math.round(globalRight * 1000.0 / globalDone) / 10.0;

    List<Map<String, Object>> knowledgeOut = new ArrayList<>();
    for (Map<String, Object> kr : knowledgeRows) {
      Integer rootId = toInt(kr.get("knowledgeId"));
      if (rootId == null) {
        continue;
      }
      Set<Integer> expanded = expandedByRoot.getOrDefault(rootId, Set.of(rootId));
      Map<String, long[]> doneBuckets = emptyTypeBuckets();
      Map<String, long[]> wrongBuckets = emptyTypeBuckets();
      long done = 0;
      long wrong = 0;
      for (Integer leaf : expanded) {
        Map<String, long[]> d = directDone.get(leaf);
        if (d != null) {
          for (String t : List.of("single", "multi", "judge")) {
            long c = d.get(t)[0];
            doneBuckets.get(t)[0] += c;
            done += c;
          }
        }
        Map<String, long[]> w = directWrong.get(leaf);
        if (w != null) {
          for (String t : List.of("single", "multi", "judge")) {
            long c = w.get(t)[0];
            wrongBuckets.get(t)[0] += c;
            wrong += c;
          }
        }
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> tc =
          kr.get("typeCounts") instanceof Map ? (Map<String, Object>) kr.get("typeCounts") : Map.of();
      long kTotal = toLong(kr.get("questionCount"));
      if (kTotal <= 0) {
        kTotal = toLong(tc.get("total"));
      }
      Map<String, Object> doneTypeCounts = new LinkedHashMap<>();
      doneTypeCounts.put("single", doneBuckets.get("single")[0]);
      doneTypeCounts.put("multi", doneBuckets.get("multi")[0]);
      doneTypeCounts.put("judge", doneBuckets.get("judge")[0]);
      doneTypeCounts.put("total", done);

      Map<String, Object> row = new LinkedHashMap<>();
      row.put("knowledgeId", rootId);
      row.put("title", kr.get("title"));
      row.put("total", kTotal);
      row.put("done", done);
      row.put("todo", Math.max(0, kTotal - done));
      row.put("wrong", wrong);
      row.put("typeCounts", tc);
      row.put("doneTypeCounts", doneTypeCounts);
      knowledgeOut.add(row);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("total", total);
    out.put("done", globalDone);
    out.put("todo", todo);
    out.put("wrong", globalWrong);
    out.put("doneRate", doneRate);
    out.put("latestRightRate", latestRightRate);
    out.put("byType", byType);
    out.put("knowledges", knowledgeOut);
    out.put("hint", "仅统计从本班入口练习的记录；已做=做过至少一次，仍错=最近一次答错");
    return out;
  }

  /**
   * 分页列出本班已做题（轻量字段：题干预览 + 最近对错）。
   *
   * @param status done=全部已做（默认）；wrong=仍错
   */
  @Transactional(readOnly = true)
  public Map<String, Object> listCoverageQuestionsForAppMember(
      Long classId, Long userId, String status, String type, int page, int pageSize) {
    if (classId == null || classId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级无效");
    }
    if (userId == null || userId <= 0) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    Long tenantId = TenantContextHolder.getTenantId();
    if (tenantId == null || tenantId <= 0) {
      tenantId = com.guoyu.platform.common.tenant.TenantContextHolder.DEFAULT_TENANT_ID;
    }

    String statusKey = StringUtils.hasText(status) ? status.trim().toLowerCase(Locale.ROOT) : "done";
    if (!"done".equals(statusKey) && !"wrong".equals(statusKey)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "status 仅支持 done / wrong");
    }
    String typeKey = normalizeType(type);

    Map<String, Object> scope = scopeService.getScopeForAppMember(classId);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> knowledgeRows =
        scope.get("knowledges") instanceof List
            ? (List<Map<String, Object>>) scope.get("knowledges")
            : List.of();
    List<Integer> courseIds = toIntList(scope.get("courseIds"));
    List<Integer> whitelistIds = knowledgeRows.stream()
        .map(r -> toInt(r.get("knowledgeId")))
        .filter(id -> id != null && id > 0)
        .toList();
    Map<Integer, Set<Integer>> expandedByRoot = buildExpandedByRoot(tenantId, courseIds, whitelistIds);
    Set<Integer> allExpanded = new HashSet<>();
    for (Set<Integer> s : expandedByRoot.values()) {
      allExpanded.addAll(s);
    }
    Map<Integer, String> knowledgeTitles = new HashMap<>();
    for (Map<String, Object> kr : knowledgeRows) {
      Integer kid = toInt(kr.get("knowledgeId"));
      if (kid != null) {
        knowledgeTitles.put(kid, String.valueOf(kr.getOrDefault("title", "#" + kid)));
      }
    }

    List<Map<String, Object>> latest =
        answerLogMapper.findLatestAnswersByClassUser(tenantId, userId, classId);
    if (latest == null) {
      latest = List.of();
    }

    List<Map<String, Object>> filtered = new ArrayList<>();
    Set<Integer> seenQ = new HashSet<>();
    for (Map<String, Object> row : latest) {
      Integer qid = toInt(row.get("questionId"));
      Integer kid = toInt(firstPresent(row, "knowledgeId", "knowledge_id"));
      String qType = normalizeType(firstPresent(row, "questionType", "question_type", "type"));
      Integer rightVal = toInt(firstPresent(row, "isRight", "is_right"));
      int isRight = rightVal == null ? 0 : rightVal;
      if (qid == null || qType == null) {
        continue;
      }
      if (!allExpanded.isEmpty() && kid != null && !allExpanded.contains(kid)) {
        continue;
      }
      if (!seenQ.add(qid)) {
        continue;
      }
      if ("wrong".equals(statusKey) && isRight == 1) {
        continue;
      }
      if (typeKey != null && !typeKey.equals(qType)) {
        continue;
      }
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("questionId", qid);
      item.put("knowledgeId", kid);
      item.put("questionType", qType);
      item.put("latestRight", isRight == 1);
      item.put("status", isRight == 1 ? "right" : "wrong");
      Object userAns = firstPresent(row, "userAnswer", "user_answer");
      item.put("userAnswerRaw", userAns == null ? null : String.valueOf(userAns));
      filtered.add(item);
    }

    filtered.sort(Comparator.comparing((Map<String, Object> m) -> toInt(m.get("questionId")),
        Comparator.nullsLast(Integer::compareTo)).reversed());

    int p = Math.max(page, 1);
    int size = Math.min(Math.max(pageSize, 1), 50);
    int total = filtered.size();
    int from = Math.min((p - 1) * size, total);
    int to = Math.min(from + size, total);
    List<Map<String, Object>> pageRows = filtered.subList(from, to);

    Set<Integer> qids = pageRows.stream()
        .map(r -> toInt(r.get("questionId")))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<Integer, ExamExamDetailEntity> detailMap = new HashMap<>();
    if (!qids.isEmpty()) {
      for (ExamExamDetailEntity d : examDetailMapper.selectBatchIds(qids)) {
        if (d != null && d.getId() != null) {
          detailMap.put(d.getId(), d);
        }
      }
    }

    List<Map<String, Object>> items = new ArrayList<>();
    for (Map<String, Object> row : pageRows) {
      Integer qid = toInt(row.get("questionId"));
      ExamExamDetailEntity detail = qid == null ? null : detailMap.get(qid);
      Integer kid = toInt(row.get("knowledgeId"));
      if (kid == null && detail != null) {
        kid = detail.getKnowledgeId();
      }
      String title = kid == null ? null : knowledgeTitles.get(kid);
      if (title == null && kid != null) {
        ExamKnowledgeStructureEntity node = knowledgeStructureMapper.selectById(kid);
        title = node != null ? node.getTitle() : ("#" + kid);
      }
      Map<String, Object> item = new LinkedHashMap<>(row);
      item.put("knowledgeId", kid);
      item.put("knowledgeTitle", title);
      item.put("stemPreview", stemPreview(detail != null ? detail.getContent() : null));

      String userRaw = row.get("userAnswerRaw") == null ? null : String.valueOf(row.get("userAnswerRaw"));
      item.remove("userAnswerRaw");
      if (detail != null) {
        try {
          FollowTaskQuestionReq snap = bankQuestionConverter.convert(detail, 0);
          item.put("correctAnswerText", formatCorrectAnswerText(snap));
          item.put("userAnswerText", formatUserAnswerText(snap, userRaw));
          if (StringUtils.hasText(snap.getAnalysis())) {
            item.put("analysis", stripHtml(snap.getAnalysis()));
          }
        } catch (Exception ex) {
          item.put("correctAnswerText", fallbackAnswerText(detail.getAnswer()));
          item.put("userAnswerText", StringUtils.hasText(userRaw) ? userRaw.trim() : "—");
          if (StringUtils.hasText(detail.getResolve())) {
            item.put("analysis", stripHtml(detail.getResolve()));
          }
        }
      } else {
        item.put("correctAnswerText", "—");
        item.put("userAnswerText", StringUtils.hasText(userRaw) ? userRaw.trim() : "—");
      }
      items.add(item);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("status", statusKey);
    out.put("type", typeKey);
    out.put("page", p);
    out.put("pageSize", size);
    out.put("total", total);
    out.put("items", items);
    return out;
  }

  private static String stemPreview(String content) {
    if (!StringUtils.hasText(content)) {
      return "";
    }
    String first = content.split("\n", 2)[0];
    return ellipsize(stripHtml(first), 80);
  }

  private static String stripHtml(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    return raw
        .replaceAll("(?i)<br\\s*/?>", " ")
        .replaceAll("(?i)</p\\s*>", " ")
        .replaceAll("<[^>]+>", "")
        .replace("&nbsp;", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private static String ellipsize(String s, int max) {
    if (s == null) {
      return "";
    }
    if (s.length() <= max) {
      return s;
    }
    return s.substring(0, max) + "…";
  }

  private static String formatCorrectAnswerText(FollowTaskQuestionReq snap) {
    if (snap == null) {
      return "—";
    }
    Map<String, String> optText = optionTextByKey(snap.getOptions());
    Object ans = snap.getAnswer();
    List<String> keys = answerKeys(ans);
    if (keys.isEmpty()) {
      return ans == null ? "—" : String.valueOf(ans);
    }
    List<String> parts = new ArrayList<>();
    for (String key : keys) {
      String text = optText.get(key);
      if (StringUtils.hasText(text)) {
        parts.add(key + ". " + text);
      } else {
        parts.add(displayJudgeOrKey(key));
      }
    }
    return String.join("；", parts);
  }

  private static String formatUserAnswerText(FollowTaskQuestionReq snap, String userRaw) {
    if (!StringUtils.hasText(userRaw)) {
      return "—";
    }
    Map<String, String> optText = optionTextByKey(snap == null ? null : snap.getOptions());
    List<String> keys = answerKeys(userRaw);
    if (keys.isEmpty()) {
      return userRaw.trim();
    }
    List<String> parts = new ArrayList<>();
    for (String key : keys) {
      String text = optText.get(key);
      if (StringUtils.hasText(text)) {
        parts.add(key + ". " + text);
      } else {
        parts.add(displayJudgeOrKey(key));
      }
    }
    return String.join("；", parts);
  }

  private static Map<String, String> optionTextByKey(List<?> options) {
    Map<String, String> map = new LinkedHashMap<>();
    if (options == null) {
      return map;
    }
    for (Object raw : options) {
      if (!(raw instanceof Map<?, ?> om)) {
        continue;
      }
      Object keyObj = om.get("key");
      Object textObj = om.get("text");
      if (keyObj == null) {
        continue;
      }
      String key = String.valueOf(keyObj).trim();
      String text = textObj == null ? "" : stripHtml(String.valueOf(textObj));
      if (StringUtils.hasText(key)) {
        map.put(key, text);
        map.put(key.toUpperCase(Locale.ROOT), text);
        map.put(key.toLowerCase(Locale.ROOT), text);
      }
    }
    return map;
  }

  private static List<String> answerKeys(Object ans) {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    if (ans == null) {
      return List.of();
    }
    if (ans instanceof List<?> list) {
      for (Object o : list) {
        if (o != null && StringUtils.hasText(String.valueOf(o))) {
          keys.add(String.valueOf(o).trim());
        }
      }
      return new ArrayList<>(keys);
    }
    String raw = String.valueOf(ans).trim();
    if (!StringUtils.hasText(raw)) {
      return List.of();
    }
    // JSON array string
    if (raw.startsWith("[") && raw.endsWith("]")) {
      String inner = raw.substring(1, raw.length() - 1);
      for (String part : inner.split(",")) {
        String p = part.trim().replace("\"", "").replace("'", "");
        if (StringUtils.hasText(p)) {
          keys.add(p);
        }
      }
      if (!keys.isEmpty()) {
        return new ArrayList<>(keys);
      }
    }
    for (String part : raw.replace("，", ",").split("[,\\s|/]+")) {
      String p = part.trim();
      if (StringUtils.hasText(p)) {
        keys.add(p);
      }
    }
    return new ArrayList<>(keys);
  }

  private static String displayJudgeOrKey(String key) {
    String n = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    return switch (n) {
      case "true", "1", "对", "正确", "a" -> "正确";
      case "false", "0", "错", "错误", "b" -> "错误";
      default -> key;
    };
  }

  private static String fallbackAnswerText(String answer) {
    if (!StringUtils.hasText(answer)) {
      return "—";
    }
    String plain = stripHtml(answer);
    return ellipsize(plain, 120);
  }

  /** 本班已做 question_id 集合（供抽题 excludeDone）。 */
  @Transactional(readOnly = true)
  public Set<Integer> findDoneQuestionIds(Long classId, Long userId) {
    Long tenantId = TenantContextHolder.getTenantId();
    if (tenantId == null || tenantId <= 0) {
      tenantId = com.guoyu.platform.common.tenant.TenantContextHolder.DEFAULT_TENANT_ID;
    }
    List<Integer> ids = answerLogMapper.findDoneQuestionIdsByClass(tenantId, userId, classId);
    return ids == null ? Set.of() : new HashSet<>(ids);
  }

  private Map<Integer, Set<Integer>> buildExpandedByRoot(
      Long tenantId, List<Integer> courseIds, List<Integer> whitelistIds) {
    Map<Integer, Set<Integer>> out = new LinkedHashMap<>();
    if (whitelistIds == null || whitelistIds.isEmpty()) {
      return out;
    }
    Set<Integer> courseHint = new HashSet<>();
    if (courseIds != null) {
      for (Integer cid : courseIds) {
        if (cid != null && cid > 0) {
          courseHint.add(cid);
        }
      }
    }
    if (courseHint.isEmpty()) {
      List<TrainClassPracticeKnowledgeEntity> ks = practiceKnowledgeMapper.selectList(
          new LambdaQueryWrapper<TrainClassPracticeKnowledgeEntity>()
              .eq(TrainClassPracticeKnowledgeEntity::getTenantId, tenantId)
              .in(TrainClassPracticeKnowledgeEntity::getKnowledgeId, whitelistIds));
      for (TrainClassPracticeKnowledgeEntity k : ks) {
        if (k.getExamCourseId() != null && k.getExamCourseId() > 0) {
          courseHint.add(k.getExamCourseId());
        }
      }
    }

    Map<Integer, List<Integer>> children = new HashMap<>();
    List<ExamKnowledgeStructureEntity> treeNodes = courseHint.isEmpty()
        ? knowledgeStructureMapper.selectBatchIds(whitelistIds)
        : knowledgeStructureMapper.selectList(
            new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
                .in(ExamKnowledgeStructureEntity::getCourseId, courseHint)
                .eq(ExamKnowledgeStructureEntity::getDeleted, 0)
                .select(
                    ExamKnowledgeStructureEntity::getId,
                    ExamKnowledgeStructureEntity::getParentId));
    for (ExamKnowledgeStructureEntity n : treeNodes) {
      if (n == null || n.getId() == null) {
        continue;
      }
      int pid = n.getParentId() == null ? 0 : n.getParentId();
      children.computeIfAbsent(pid, k -> new ArrayList<>()).add(n.getId());
    }

    for (Integer root : whitelistIds) {
      if (root == null || root <= 0) {
        continue;
      }
      Set<Integer> seen = new HashSet<>();
      ArrayDeque<Integer> dq = new ArrayDeque<>();
      dq.add(root);
      while (!dq.isEmpty()) {
        Integer cur = dq.poll();
        if (cur == null || !seen.add(cur)) {
          continue;
        }
        for (Integer c : children.getOrDefault(cur, List.of())) {
          dq.add(c);
        }
      }
      out.put(root, seen);
    }
    return out;
  }

  private static Map<String, Object> typeStat(long total, long done, long wrong) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("total", total);
    m.put("done", done);
    m.put("todo", Math.max(0, total - done));
    m.put("wrong", wrong);
    return m;
  }

  private static Map<String, long[]> emptyTypeBuckets() {
    Map<String, long[]> m = new HashMap<>();
    m.put("single", new long[] {0, 0});
    m.put("multi", new long[] {0, 0});
    m.put("judge", new long[] {0, 0});
    return m;
  }

  private static String normalizeType(Object raw) {
    if (raw == null) {
      return null;
    }
    String t = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
    return switch (t) {
      case "single", "danxuan", "单选", "1" -> "single";
      case "multi", "duoxuan", "多选", "2" -> "multi";
      case "judge", "pandan", "判断", "3" -> "judge";
      default -> null;
    };
  }

  private static Object firstPresent(Map<String, Object> row, String... keys) {
    if (row == null) {
      return null;
    }
    for (String k : keys) {
      if (row.containsKey(k) && row.get(k) != null) {
        return row.get(k);
      }
    }
    for (Map.Entry<String, Object> e : row.entrySet()) {
      if (e.getKey() == null || e.getValue() == null) {
        continue;
      }
      for (String k : keys) {
        if (e.getKey().equalsIgnoreCase(k)) {
          return e.getValue();
        }
      }
    }
    return null;
  }

  private static Integer toInt(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(v).trim());
    } catch (Exception e) {
      return null;
    }
  }

  private static long toLong(Object v) {
    if (v == null) {
      return 0L;
    }
    if (v instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(v).trim());
    } catch (Exception e) {
      return 0L;
    }
  }

  private static List<Integer> toIntList(Object raw) {
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }
    List<Integer> out = new ArrayList<>();
    for (Object o : list) {
      Integer i = toInt(o);
      if (i != null && i > 0) {
        out.add(i);
      }
    }
    return out;
  }
}
