package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.exam.mapper.AppExamUserAnswerLogMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeCourseEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeKnowledgeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeScopeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeCourseMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeKnowledgeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeScopeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 班级刷题学情：按花名册聚合 exam_user_answer_log(class_id) */
@Service
public class ClassPracticeAnalyticsApplicationService {

  private final TrainInfoClassMapper classMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final AppExamUserAnswerLogMapper answerLogMapper;
  private final TrainClassPracticeScopeMapper scopeMapper;
  private final TrainClassPracticeCourseMapper practiceCourseMapper;
  private final TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper;
  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;
  private final TrainingOperatorScopeService operatorScopeService;

  public ClassPracticeAnalyticsApplicationService(
      TrainInfoClassMapper classMapper,
      TrainPeopleListMapper peopleListMapper,
      AppExamUserAnswerLogMapper answerLogMapper,
      TrainClassPracticeScopeMapper scopeMapper,
      TrainClassPracticeCourseMapper practiceCourseMapper,
      TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper,
      ExamKnowledgeStructureMapper knowledgeStructureMapper,
      TrainingOperatorScopeService operatorScopeService) {
    this.classMapper = classMapper;
    this.peopleListMapper = peopleListMapper;
    this.answerLogMapper = answerLogMapper;
    this.scopeMapper = scopeMapper;
    this.practiceCourseMapper = practiceCourseMapper;
    this.practiceKnowledgeMapper = practiceKnowledgeMapper;
    this.knowledgeStructureMapper = knowledgeStructureMapper;
    this.operatorScopeService = operatorScopeService;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> summary(Long classId, LocalDate from, LocalDate to, boolean inScopeOnly) {
    TrainInfoClassEntity clazz = requireClass(classId);
    DateRange range = resolveRange(from, to);
    List<Integer> scopeKnowledgeIds = inScopeOnly ? resolveScopeKnowledgeFilter(classId) : null;

    List<TrainPeopleListEntity> students = listStudents(classId);
    long withAccount = students.stream().filter(s -> s.getPlatformUserId() != null && s.getPlatformUserId() > 0).count();

    List<Map<String, Object>> byUser = answerLogMapper.aggregateByUser(
        SecurityContext.currentUser().getTenantId(), classId, range.from(), range.toExclusive(), scopeKnowledgeIds);
    Map<Long, Map<String, Object>> userStats = indexByUserId(byUser);

    int activeUsers = 0;
    long answerTotal = 0;
    long rightTotal = 0;
    for (TrainPeopleListEntity s : students) {
      if (s.getPlatformUserId() == null) continue;
      Map<String, Object> st = userStats.get(s.getPlatformUserId());
      if (st == null) continue;
      activeUsers++;
      answerTotal += toLong(st.get("answerCount"));
      rightTotal += toLong(st.get("rightCount"));
    }

    LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
    int active7d = 0;
    for (Map<String, Object> st : byUser) {
      LocalDateTime last = toDateTime(st.get("lastPracticeAt"));
      if (last != null && !last.isBefore(weekAgo)) {
        active7d++;
      }
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("classTitle", clazz.getTitle());
    out.put("from", range.fromDate().toString());
    out.put("to", range.toDate().toString());
    out.put("inScopeOnly", inScopeOnly);
    out.put("scopeKnowledgeCount", scopeKnowledgeIds == null ? null : scopeKnowledgeIds.size());
    out.put("rosterCount", students.size());
    out.put("withAccountCount", withAccount);
    out.put("activeUserCount", activeUsers);
    out.put("participationRate", rate(activeUsers, students.size()));
    out.put("answerCount", answerTotal);
    out.put("rightCount", rightTotal);
    out.put("accuracyRate", rate(rightTotal, answerTotal));
    out.put("avgAnswersPerActive", activeUsers == 0 ? 0 : round1(answerTotal * 1.0 / activeUsers));
    out.put("activeUserCount7d", active7d);
    out.put("hint", answerTotal == 0
        ? "暂无带班级上下文的刷题数据。请学员从班级入口刷题（App 传 classId）后再查看。"
        : null);
    return out;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> students(
      Long classId, LocalDate from, LocalDate to, boolean inScopeOnly, String keyword, int page, int pageSize) {
    requireClass(classId);
    DateRange range = resolveRange(from, to);
    List<Integer> scopeKnowledgeIds = inScopeOnly ? resolveScopeKnowledgeFilter(classId) : null;
    List<TrainPeopleListEntity> students = listStudents(classId);
    if (StringUtils.hasText(keyword)) {
      String kw = keyword.trim().toLowerCase();
      students = students.stream().filter(s ->
          (s.getRealname() != null && s.getRealname().toLowerCase().contains(kw))
              || (s.getPhone() != null && s.getPhone().contains(kw))
              || (s.getIdCard() != null && s.getIdCard().toLowerCase().contains(kw))
      ).toList();
    }

    List<Map<String, Object>> byUser = answerLogMapper.aggregateByUser(
        SecurityContext.currentUser().getTenantId(), classId, range.from(), range.toExclusive(), scopeKnowledgeIds);
    Map<Long, Map<String, Object>> userStats = indexByUserId(byUser);

    LocalDate today = LocalDate.now();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (TrainPeopleListEntity s : students) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("peopleId", s.getId());
      row.put("realname", s.getRealname());
      row.put("phone", maskPhone(s.getPhone()));
      row.put("idCard", maskIdCard(s.getIdCard()));
      row.put("platformUserId", s.getPlatformUserId());
      row.put("hasAccount", s.getPlatformUserId() != null && s.getPlatformUserId() > 0);
      Map<String, Object> st = s.getPlatformUserId() == null ? null : userStats.get(s.getPlatformUserId());
      long answers = st == null ? 0 : toLong(st.get("answerCount"));
      long rights = st == null ? 0 : toLong(st.get("rightCount"));
      long papers = st == null ? 0 : toLong(st.get("paperCount"));
      long days = st == null ? 0 : toLong(st.get("practiceDays"));
      LocalDateTime last = st == null ? null : toDateTime(st.get("lastPracticeAt"));
      row.put("answerCount", answers);
      row.put("rightCount", rights);
      row.put("accuracyRate", rate(rights, answers));
      row.put("paperCount", papers);
      row.put("practiceDays", days);
      row.put("lastPracticeAt", last);
      row.put("idleDays", last == null ? null : ChronoUnit.DAYS.between(last.toLocalDate(), today));
      rows.add(row);
    }

    rows.sort(Comparator
        .comparing((Map<String, Object> r) -> r.get("lastPracticeAt") == null ? 1 : 0)
        .thenComparing((Map<String, Object> r) -> (LocalDateTime) r.get("lastPracticeAt"),
            Comparator.nullsLast(Comparator.reverseOrder())));

    int p = Math.max(page, 1);
    int ps = Math.min(Math.max(pageSize, 1), 100);
    int fromIdx = (p - 1) * ps;
    int toIdx = Math.min(fromIdx + ps, rows.size());
    List<Map<String, Object>> pageRows = fromIdx >= rows.size() ? List.of() : rows.subList(fromIdx, toIdx);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("from", range.fromDate().toString());
    out.put("to", range.toDate().toString());
    out.put("inScopeOnly", inScopeOnly);
    out.put("page", p);
    out.put("pageSize", ps);
    out.put("total", rows.size());
    out.put("items", pageRows);
    return out;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> knowledges(Long classId, LocalDate from, LocalDate to, boolean inScopeOnly, int limit) {
    requireClass(classId);
    DateRange range = resolveRange(from, to);
    List<Integer> scopeKnowledgeIds = inScopeOnly ? resolveScopeKnowledgeFilter(classId) : null;
    int lim = Math.min(Math.max(limit, 1), 100);

    List<Map<String, Object>> raw = answerLogMapper.aggregateByKnowledge(
        SecurityContext.currentUser().getTenantId(), classId, range.from(), range.toExclusive(),
        scopeKnowledgeIds, lim);
    Set<Integer> kids = raw.stream().map(m -> toInt(m.get("knowledgeId"))).filter(Objects::nonNull).collect(Collectors.toSet());
    Map<Integer, ExamKnowledgeStructureEntity> titles = kids.isEmpty() ? Map.of()
        : knowledgeStructureMapper.selectBatchIds(kids).stream()
            .collect(Collectors.toMap(ExamKnowledgeStructureEntity::getId, k -> k, (a, b) -> a));

    List<Map<String, Object>> items = new ArrayList<>();
    for (Map<String, Object> m : raw) {
      Integer kid = toInt(m.get("knowledgeId"));
      ExamKnowledgeStructureEntity node = kid == null ? null : titles.get(kid);
      long answers = toLong(m.get("answerCount"));
      long wrongs = toLong(m.get("wrongCount"));
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("knowledgeId", kid);
      row.put("title", node != null ? node.getTitle() : ("#" + kid));
      row.put("courseId", node != null ? node.getCourseId() : null);
      row.put("answerCount", answers);
      row.put("wrongCount", wrongs);
      row.put("rightCount", toLong(m.get("rightCount")));
      row.put("wrongRate", rate(wrongs, answers));
      row.put("userCount", toLong(m.get("userCount")));
      items.add(row);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("from", range.fromDate().toString());
    out.put("to", range.toDate().toString());
    out.put("inScopeOnly", inScopeOnly);
    out.put("items", items);
    return out;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> daily(Long classId, LocalDate from, LocalDate to, boolean inScopeOnly) {
    requireClass(classId);
    DateRange range = resolveRange(from, to);
    List<Integer> scopeKnowledgeIds = inScopeOnly ? resolveScopeKnowledgeFilter(classId) : null;

    List<Map<String, Object>> raw = answerLogMapper.aggregateDaily(
        SecurityContext.currentUser().getTenantId(), classId, range.from(), range.toExclusive(), scopeKnowledgeIds);

    List<Map<String, Object>> items = new ArrayList<>();
    for (Map<String, Object> m : raw) {
      long answers = toLong(m.get("answerCount"));
      long rights = toLong(m.get("rightCount"));
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("bizDate", String.valueOf(m.get("bizDate")));
      row.put("answerCount", answers);
      row.put("rightCount", rights);
      row.put("accuracyRate", rate(rights, answers));
      row.put("userCount", toLong(m.get("userCount")));
      row.put("paperCount", toLong(m.get("paperCount")));
      items.add(row);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("from", range.fromDate().toString());
    out.put("to", range.toDate().toString());
    out.put("inScopeOnly", inScopeOnly);
    out.put("items", items);
    return out;
  }

  private List<TrainPeopleListEntity> listStudents(Long classId) {
    return peopleListMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getType, "student")
        .orderByAsc(TrainPeopleListEntity::getId));
  }

  /**
   * 范围内模式：有知识点白名单则用之；否则若有科目白名单则展开科目下全部知识点；皆空则不过滤（null）。
   */
  private List<Integer> resolveScopeKnowledgeFilter(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainClassPracticeScopeEntity scope = scopeMapper.selectOne(new LambdaQueryWrapper<TrainClassPracticeScopeEntity>()
        .eq(TrainClassPracticeScopeEntity::getTenantId, tenantId)
        .eq(TrainClassPracticeScopeEntity::getClassId, classId));
    if (scope != null && scope.getEnabled() != null && scope.getEnabled() == 0) {
      return List.of(-1); // 停用范围时「范围内」视图无数据
    }
    List<TrainClassPracticeKnowledgeEntity> ks = practiceKnowledgeMapper.selectList(
        new LambdaQueryWrapper<TrainClassPracticeKnowledgeEntity>()
            .eq(TrainClassPracticeKnowledgeEntity::getTenantId, tenantId)
            .eq(TrainClassPracticeKnowledgeEntity::getClassId, classId));
    if (!ks.isEmpty()) {
      return ks.stream().map(TrainClassPracticeKnowledgeEntity::getKnowledgeId).filter(Objects::nonNull).distinct().toList();
    }
    List<TrainClassPracticeCourseEntity> courses = practiceCourseMapper.selectList(
        new LambdaQueryWrapper<TrainClassPracticeCourseEntity>()
            .eq(TrainClassPracticeCourseEntity::getTenantId, tenantId)
            .eq(TrainClassPracticeCourseEntity::getClassId, classId));
    if (courses.isEmpty()) {
      return null;
    }
    List<Integer> courseIds = courses.stream().map(TrainClassPracticeCourseEntity::getExamCourseId).toList();
    return knowledgeStructureMapper.selectList(new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .in(ExamKnowledgeStructureEntity::getCourseId, courseIds))
        .stream().map(ExamKnowledgeStructureEntity::getId).toList();
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    if (classId == null || classId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级 ID 无效");
    }
    TrainInfoClassEntity clazz = classMapper.selectOne(new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getId, classId)
        .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    operatorScopeService.assertClassAccessible(clazz);
    return clazz;
  }

  private record DateRange(LocalDate fromDate, LocalDate toDate, LocalDateTime from, LocalDateTime toExclusive) {}

  private DateRange resolveRange(LocalDate from, LocalDate to) {
    LocalDate end = to != null ? to : LocalDate.now();
    LocalDate start = from != null ? from : end.minusDays(29);
    if (start.isAfter(end)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "开始日期不能晚于结束日期");
    }
    if (ChronoUnit.DAYS.between(start, end) > 180) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "统计区间最长 180 天");
    }
    return new DateRange(start, end, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
  }

  private Map<Long, Map<String, Object>> indexByUserId(List<Map<String, Object>> rows) {
    Map<Long, Map<String, Object>> map = new LinkedHashMap<>();
    for (Map<String, Object> row : rows) {
      Long uid = toLongObj(row.get("userId"));
      if (uid != null) map.put(uid, row);
    }
    return map;
  }

  private static Double rate(long num, long den) {
    if (den <= 0) return null;
    return BigDecimal.valueOf(num * 100.0 / den).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }

  private static double round1(double v) {
    return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }

  private static long toLong(Object v) {
    if (v == null) return 0;
    if (v instanceof Number n) return n.longValue();
    try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0; }
  }

  private static Long toLongObj(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.longValue();
    try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
  }

  private static Integer toInt(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.intValue();
    try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
  }

  private static LocalDateTime toDateTime(Object v) {
    if (v == null) return null;
    if (v instanceof LocalDateTime ldt) return ldt;
    if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
    if (v instanceof LocalDate ld) return ld.atTime(LocalTime.MIN);
    try { return LocalDateTime.parse(String.valueOf(v).replace(' ', 'T').substring(0, 19)); }
    catch (Exception e) { return null; }
  }

  private static String maskPhone(String phone) {
    if (!StringUtils.hasText(phone) || phone.length() < 7) return phone;
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }

  private static String maskIdCard(String id) {
    if (!StringUtils.hasText(id) || id.length() < 8) return id;
    return id.substring(0, 4) + "**********" + id.substring(id.length() - 4);
  }
}
