package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.ClassPracticeScopeSaveReq;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeCourseEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeKnowledgeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeScopeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeCourseMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeKnowledgeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeScopeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 班级刷题练习范围（科目 + 知识点白名单）。App 后接；后台先配置入库。
 */
@Service
public class ClassPracticeScopeApplicationService {

  private final TrainInfoClassMapper classMapper;
  private final TrainClassPracticeScopeMapper scopeMapper;
  private final TrainClassPracticeCourseMapper courseMapper;
  private final TrainClassPracticeKnowledgeMapper knowledgeMapper;
  private final ExamCourseMapper examCourseMapper;
  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;
  private final ExamExamDetailMapper examDetailMapper;
  private final TrainingOperatorScopeService operatorScopeService;

  public ClassPracticeScopeApplicationService(
      TrainInfoClassMapper classMapper,
      TrainClassPracticeScopeMapper scopeMapper,
      TrainClassPracticeCourseMapper courseMapper,
      TrainClassPracticeKnowledgeMapper knowledgeMapper,
      ExamCourseMapper examCourseMapper,
      ExamKnowledgeStructureMapper knowledgeStructureMapper,
      ExamExamDetailMapper examDetailMapper,
      TrainingOperatorScopeService operatorScopeService) {
    this.classMapper = classMapper;
    this.scopeMapper = scopeMapper;
    this.courseMapper = courseMapper;
    this.knowledgeMapper = knowledgeMapper;
    this.examCourseMapper = examCourseMapper;
    this.knowledgeStructureMapper = knowledgeStructureMapper;
    this.examDetailMapper = examDetailMapper;
    this.operatorScopeService = operatorScopeService;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listCourseOptions(String keyword, Long workTypeId) {
    LambdaQueryWrapper<ExamCourseEntity> q = new LambdaQueryWrapper<ExamCourseEntity>()
        .orderByAsc(ExamCourseEntity::getSortNumber)
        .orderByAsc(ExamCourseEntity::getId)
        .last("LIMIT 500");
    if (workTypeId != null && workTypeId > 0) {
      q.eq(ExamCourseEntity::getWorkTypeId, workTypeId.intValue());
    }
    if (StringUtils.hasText(keyword)) {
      q.like(ExamCourseEntity::getTitle, keyword.trim());
    }
    List<Map<String, Object>> items = examCourseMapper.selectList(q).stream().map(c -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", c.getId());
      row.put("title", c.getTitle());
      row.put("level", c.getLevel());
      row.put("workTypeId", c.getWorkTypeId());
      return row;
    }).toList();
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("items", items);
    return out;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> knowledgeTree(Integer courseId) {
    if (courseId == null || courseId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请指定科目");
    }
    List<ExamKnowledgeStructureEntity> all = knowledgeStructureMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .eq(ExamKnowledgeStructureEntity::getCourseId, courseId)
            .orderByAsc(ExamKnowledgeStructureEntity::getSortNumber)
            .orderByAsc(ExamKnowledgeStructureEntity::getId));
    Map<Integer, List<ExamKnowledgeStructureEntity>> byParent = new LinkedHashMap<>();
    for (ExamKnowledgeStructureEntity n : all) {
      int pid = n.getParentId() == null ? 0 : n.getParentId();
      byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(n);
    }
    return buildKnowledgeNodes(byParent, 0);
  }

  private List<Map<String, Object>> buildKnowledgeNodes(
      Map<Integer, List<ExamKnowledgeStructureEntity>> byParent, int parentId) {
    List<ExamKnowledgeStructureEntity> children = byParent.getOrDefault(parentId, List.of());
    List<Map<String, Object>> out = new ArrayList<>();
    for (ExamKnowledgeStructureEntity n : children) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", n.getId());
      row.put("title", n.getTitle());
      row.put("courseId", n.getCourseId());
      row.put("parentId", n.getParentId());
      List<Map<String, Object>> kids = buildKnowledgeNodes(byParent, n.getId());
      if (!kids.isEmpty()) {
        row.put("children", kids);
      }
      out.add(row);
    }
    return out;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getScope(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return buildScopeView(clazz, tenantId);
  }

  /**
   * App 班成员只读练习范围（跟班刷题对齐）。
   * 不走管理端组织权限；租户取 {@link TenantContextHolder}（App principal.tenantId 恒为 0）。
   */
  @Transactional(readOnly = true)
  public Map<String, Object> getScopeForAppMember(Long classId) {
    if (classId == null || classId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级 ID 无效");
    }
    TrainInfoClassEntity clazz = classMapper.selectOne(new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getId, classId)
        .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    Long tenantId = TenantContextHolder.getTenantId();
    if (tenantId == null || tenantId <= 0) {
      tenantId = 1L;
    }
    return buildScopeView(clazz, tenantId);
  }

  private Map<String, Object> buildScopeView(TrainInfoClassEntity clazz, Long tenantId) {
    Long classId = clazz.getId();
    TrainClassPracticeScopeEntity scope = scopeMapper.selectOne(new LambdaQueryWrapper<TrainClassPracticeScopeEntity>()
        .eq(TrainClassPracticeScopeEntity::getTenantId, tenantId)
        .eq(TrainClassPracticeScopeEntity::getClassId, classId));

    List<TrainClassPracticeCourseEntity> courses = courseMapper.selectList(
        new LambdaQueryWrapper<TrainClassPracticeCourseEntity>()
            .eq(TrainClassPracticeCourseEntity::getTenantId, tenantId)
            .eq(TrainClassPracticeCourseEntity::getClassId, classId)
            .orderByAsc(TrainClassPracticeCourseEntity::getId));
    List<TrainClassPracticeKnowledgeEntity> knowledges = knowledgeMapper.selectList(
        new LambdaQueryWrapper<TrainClassPracticeKnowledgeEntity>()
            .eq(TrainClassPracticeKnowledgeEntity::getTenantId, tenantId)
            .eq(TrainClassPracticeKnowledgeEntity::getClassId, classId)
            .orderByAsc(TrainClassPracticeKnowledgeEntity::getId));

    List<Integer> courseIds = courses.stream().map(TrainClassPracticeCourseEntity::getExamCourseId).toList();
    List<Integer> knowledgeIds = knowledges.stream().map(TrainClassPracticeKnowledgeEntity::getKnowledgeId).toList();

    Map<Integer, String> courseTitles = loadCourseTitles(courseIds);
    Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap = loadKnowledges(knowledgeIds);
    Map<Integer, Map<String, Long>> knowledgeTypeCounts =
        loadWhitelistTypeCounts(tenantId, courseIds, knowledgeIds);

    List<Map<String, Object>> courseRows = new ArrayList<>();
    for (Integer id : courseIds) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("courseId", id);
      row.put("title", courseTitles.getOrDefault(id, "#" + id));
      courseRows.add(row);
    }
    List<Map<String, Object>> knowledgeRows = new ArrayList<>();
    long scopeSingle = 0;
    long scopeMulti = 0;
    long scopeJudge = 0;
    for (TrainClassPracticeKnowledgeEntity k : knowledges) {
      ExamKnowledgeStructureEntity node = knowledgeMap.get(k.getKnowledgeId());
      Map<String, Long> tc = knowledgeTypeCounts.getOrDefault(k.getKnowledgeId(), Map.of());
      long single = tc.getOrDefault("single", 0L);
      long multi = tc.getOrDefault("multi", 0L);
      long judge = tc.getOrDefault("judge", 0L);
      scopeSingle += single;
      scopeMulti += multi;
      scopeJudge += judge;
      Map<String, Object> typeCounts = new LinkedHashMap<>();
      typeCounts.put("single", single);
      typeCounts.put("multi", multi);
      typeCounts.put("judge", judge);
      typeCounts.put("total", single + multi + judge);
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("knowledgeId", k.getKnowledgeId());
      row.put("courseId", k.getExamCourseId());
      row.put("title", node != null ? node.getTitle() : ("#" + k.getKnowledgeId()));
      row.put("questionCount", single + multi + judge);
      row.put("typeCounts", typeCounts);
      knowledgeRows.add(row);
    }

    Map<String, Object> scopeTypeCounts = new LinkedHashMap<>();
    scopeTypeCounts.put("single", scopeSingle);
    scopeTypeCounts.put("multi", scopeMulti);
    scopeTypeCounts.put("judge", scopeJudge);
    scopeTypeCounts.put("total", scopeSingle + scopeMulti + scopeJudge);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("classTitle", clazz.getTitle());
    out.put("workTypeId", clazz.getWorkTypeId());
    out.put("workTypeLevel", clazz.getWorkTypeLevel());
    out.put("configured", scope != null || !courseIds.isEmpty());
    out.put("enabled", scope == null ? 1 : (scope.getEnabled() == null ? 1 : scope.getEnabled()));
    out.put("remark", scope == null ? null : scope.getRemark());
    out.put("practiceStartTime", scope == null || scope.getPracticeStartTime() == null
        ? null : scope.getPracticeStartTime().toLocalDate().toString());
    out.put("practiceEndTime", scope == null || scope.getPracticeEndTime() == null
        ? null : scope.getPracticeEndTime().toLocalDate().toString());
    out.put("courseIds", courseIds);
    out.put("knowledgeIds", knowledgeIds);
    out.put("courses", courseRows);
    out.put("knowledges", knowledgeRows);
    out.put("typeCounts", scopeTypeCounts);
    out.put("appBound", true);
    out.put("hint", "未设置刷题日期则不过期；设置后仅在起止日期（含当天）内允许学员按班刷题");
    return out;
  }

  /**
   * 按班级工种/等级推荐科目（不落库，供前端一键填充）。
   */
  @Transactional(readOnly = true)
  public Map<String, Object> suggestCourses(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    List<ExamCourseEntity> matched = List.of();
    if (clazz.getWorkTypeId() != null && clazz.getWorkTypeId() > 0) {
      LambdaQueryWrapper<ExamCourseEntity> q = new LambdaQueryWrapper<ExamCourseEntity>()
          .eq(ExamCourseEntity::getWorkTypeId, clazz.getWorkTypeId().intValue())
          .orderByAsc(ExamCourseEntity::getSortNumber)
          .orderByAsc(ExamCourseEntity::getId);
      if (clazz.getWorkTypeLevel() != null && clazz.getWorkTypeLevel() > 0) {
        q.eq(ExamCourseEntity::getLevel, clazz.getWorkTypeLevel());
      }
      matched = examCourseMapper.selectList(q);
      if (matched.isEmpty() && clazz.getWorkTypeLevel() != null) {
        matched = examCourseMapper.selectList(new LambdaQueryWrapper<ExamCourseEntity>()
            .eq(ExamCourseEntity::getWorkTypeId, clazz.getWorkTypeId().intValue())
            .orderByAsc(ExamCourseEntity::getSortNumber)
            .orderByAsc(ExamCourseEntity::getId));
      }
    }
    List<Map<String, Object>> courses = matched.stream().map(c -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("courseId", c.getId());
      row.put("title", c.getTitle());
      row.put("level", c.getLevel());
      return row;
    }).toList();
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("workTypeId", clazz.getWorkTypeId());
    out.put("workTypeLevel", clazz.getWorkTypeLevel());
    out.put("courses", courses);
    return out;
  }

  @Transactional
  public Map<String, Object> saveScope(Long classId, ClassPracticeScopeSaveReq req) {
    operatorScopeService.requireOrgOperator();
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LocalDateTime now = LocalDateTime.now();

    Set<Integer> courseIds = normalizeIds(req.getCourseIds());
    Set<Integer> knowledgeIds = normalizeIds(req.getKnowledgeIds());

    Map<Integer, ExamCourseEntity> courseMap = Map.of();
    if (!courseIds.isEmpty()) {
      List<ExamCourseEntity> found = examCourseMapper.selectBatchIds(courseIds);
      courseMap = found.stream().collect(Collectors.toMap(ExamCourseEntity::getId, c -> c, (a, b) -> a));
      if (courseMap.size() != courseIds.size()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "部分科目不存在或已删除");
      }
    }

    // 以「所选科目下的知识点树」为准校验；course_id 字段异常或切换科目残留的 ID 予以剔除
    Map<Integer, Integer> knowledgeToCourse = indexKnowledgeIdsByCourse(courseIds);
    Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap = new LinkedHashMap<>();
    Set<Integer> validKnowledgeIds = new LinkedHashSet<>();
    if (!knowledgeIds.isEmpty()) {
      if (courseIds.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "选择知识点前请先选择科目");
      }
      List<ExamKnowledgeStructureEntity> found = knowledgeStructureMapper.selectBatchIds(knowledgeIds);
      Map<Integer, ExamKnowledgeStructureEntity> loaded = found.stream()
          .collect(Collectors.toMap(ExamKnowledgeStructureEntity::getId, k -> k, (a, b) -> a));
      for (Integer kid : knowledgeIds) {
        ExamKnowledgeStructureEntity node = loaded.get(kid);
        if (node == null) {
          continue;
        }
        Integer belongCourse = resolveBelongCourseId(node, courseIds, knowledgeToCourse);
        if (belongCourse == null) {
          continue;
        }
        // 落库时用归属科目，避免 course_id 为空/错挂
        if (node.getCourseId() == null || !Objects.equals(node.getCourseId(), belongCourse)) {
          node.setCourseId(belongCourse);
        }
        knowledgeMap.put(kid, node);
        validKnowledgeIds.add(kid);
      }
    }

    replaceCourses(tenantId, classId, courseIds, now);
    replaceKnowledges(tenantId, classId, validKnowledgeIds, knowledgeMap, now);

    TrainClassPracticeScopeEntity scope = scopeMapper.selectOne(new LambdaQueryWrapper<TrainClassPracticeScopeEntity>()
        .eq(TrainClassPracticeScopeEntity::getTenantId, tenantId)
        .eq(TrainClassPracticeScopeEntity::getClassId, classId));
    int enabled = req.getEnabled() != null && req.getEnabled() == 0 ? 0 : 1;
    String remark = StringUtils.hasText(req.getRemark()) ? req.getRemark().trim() : null;
    // 按日：开始日 00:00:00，截止日 23:59:59（含当天）
    LocalDateTime practiceStart = req.getPracticeStartTime() == null
        ? null : req.getPracticeStartTime().atStartOfDay();
    LocalDateTime practiceEnd = req.getPracticeEndTime() == null
        ? null : req.getPracticeEndTime().atTime(23, 59, 59);
    if (practiceStart != null && practiceEnd != null && practiceStart.toLocalDate().isAfter(practiceEnd.toLocalDate())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "刷题开始日期不能晚于截止日期");
    }
    if (scope == null) {
      // 软删行仍占 uk_class_practice_scope(tenant_id, class_id)，插入前物理清幽灵
      scopeMapper.physicalDeleteByClass(tenantId, classId);
      scope = new TrainClassPracticeScopeEntity();
      scope.setTenantId(tenantId);
      scope.setClassId(classId);
      scope.setEnabled(enabled);
      scope.setRemark(remark);
      scope.setPracticeStartTime(practiceStart);
      scope.setPracticeEndTime(practiceEnd);
      scope.setCreateTime(now);
      scope.setUpdateTime(now);
      scope.setDeleted(0);
      scopeMapper.insert(scope);
    } else {
      scope.setEnabled(enabled);
      scope.setRemark(remark);
      scope.setPracticeStartTime(practiceStart);
      scope.setPracticeEndTime(practiceEnd);
      scope.setUpdateTime(now);
      scopeMapper.updateById(scope);
    }

    return getScope(clazz.getId());
  }

  /** 删班时清理练习范围（物理删，避免唯一键被软删行占住） */
  @Transactional
  public int cleanupByClassId(Long classId) {
    if (classId == null) {
      return 0;
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    int n = 0;
    n += knowledgeMapper.physicalDeleteByClass(tenantId, classId);
    n += courseMapper.physicalDeleteByClass(tenantId, classId);
    n += scopeMapper.physicalDeleteByClass(tenantId, classId);
    return n;
  }

  /**
   * 所选科目下全部知识点 id → 科目 id（用于归属判定，不依赖节点 course_id 字段是否准确）。
   */
  private Map<Integer, Integer> indexKnowledgeIdsByCourse(Set<Integer> courseIds) {
    Map<Integer, Integer> out = new LinkedHashMap<>();
    if (courseIds.isEmpty()) {
      return out;
    }
    List<ExamKnowledgeStructureEntity> rows = knowledgeStructureMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .in(ExamKnowledgeStructureEntity::getCourseId, courseIds));
    for (ExamKnowledgeStructureEntity row : rows) {
      if (row.getId() != null && row.getCourseId() != null) {
        out.put(row.getId(), row.getCourseId());
      }
    }
    return out;
  }

  private Integer resolveBelongCourseId(
      ExamKnowledgeStructureEntity node,
      Set<Integer> selectedCourses,
      Map<Integer, Integer> knowledgeToCourse) {
    if (node == null || node.getId() == null) {
      return null;
    }
    Integer mapped = knowledgeToCourse.get(node.getId());
    if (mapped != null && selectedCourses.contains(mapped)) {
      return mapped;
    }
    if (node.getCourseId() != null && selectedCourses.contains(node.getCourseId())) {
      return node.getCourseId();
    }
    // 向上追溯父节点，兼容子节点 course_id 为空/错挂
    Integer parentId = node.getParentId();
    int guard = 0;
    while (parentId != null && parentId > 0 && guard++ < 32) {
      Integer parentCourse = knowledgeToCourse.get(parentId);
      if (parentCourse != null && selectedCourses.contains(parentCourse)) {
        return parentCourse;
      }
      ExamKnowledgeStructureEntity parent = knowledgeStructureMapper.selectById(parentId);
      if (parent == null) {
        break;
      }
      if (parent.getCourseId() != null && selectedCourses.contains(parent.getCourseId())) {
        return parent.getCourseId();
      }
      parentId = parent.getParentId();
    }
    return null;
  }

  private void replaceCourses(Long tenantId, Long classId, Set<Integer> courseIds, LocalDateTime now) {
    // 物理删：全局 logic-delete 软删会与 uk_class_practice_course 冲突
    courseMapper.physicalDeleteByClass(tenantId, classId);
    for (Integer courseId : courseIds) {
      TrainClassPracticeCourseEntity row = new TrainClassPracticeCourseEntity();
      row.setTenantId(tenantId);
      row.setClassId(classId);
      row.setExamCourseId(courseId);
      row.setCreateTime(now);
      row.setDeleted(0);
      courseMapper.insert(row);
    }
  }

  private void replaceKnowledges(
      Long tenantId,
      Long classId,
      Set<Integer> knowledgeIds,
      Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap,
      LocalDateTime now) {
    // 物理删：全局 logic-delete 软删会与 uk_class_practice_knowledge 冲突
    knowledgeMapper.physicalDeleteByClass(tenantId, classId);
    for (Integer knowledgeId : knowledgeIds) {
      ExamKnowledgeStructureEntity node = knowledgeMap.get(knowledgeId);
      if (node == null) {
        continue;
      }
      TrainClassPracticeKnowledgeEntity row = new TrainClassPracticeKnowledgeEntity();
      row.setTenantId(tenantId);
      row.setClassId(classId);
      row.setExamCourseId(node.getCourseId());
      row.setKnowledgeId(knowledgeId);
      row.setCreateTime(now);
      row.setDeleted(0);
      knowledgeMapper.insert(row);
    }
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

  private Map<Integer, String> loadCourseTitles(List<Integer> courseIds) {
    if (courseIds.isEmpty()) {
      return Map.of();
    }
    return examCourseMapper.selectBatchIds(courseIds).stream()
        .collect(Collectors.toMap(ExamCourseEntity::getId, ExamCourseEntity::getTitle, (a, b) -> a));
  }

  private Map<Integer, ExamKnowledgeStructureEntity> loadKnowledges(List<Integer> knowledgeIds) {
    if (knowledgeIds.isEmpty()) {
      return Map.of();
    }
    return knowledgeStructureMapper.selectBatchIds(knowledgeIds).stream()
        .collect(Collectors.toMap(ExamKnowledgeStructureEntity::getId, k -> k, (a, b) -> a));
  }

  /**
   * 白名单知识点题量：含自身 + 子孙挂题（与 App 抽题 expand 口径一致）。
   */
  private Map<Integer, Long> loadWhitelistQuestionCounts(
      Long tenantId, List<Integer> courseIds, List<Integer> whitelistIds) {
    Map<Integer, Map<String, Long>> byType = loadWhitelistTypeCounts(tenantId, courseIds, whitelistIds);
    Map<Integer, Long> out = new LinkedHashMap<>();
    for (Map.Entry<Integer, Map<String, Long>> e : byType.entrySet()) {
      long sum = e.getValue().values().stream().mapToLong(Long::longValue).sum();
      out.put(e.getKey(), sum);
    }
    return out;
  }

  /** 白名单知识点分题型题量（含子孙）。 */
  private Map<Integer, Map<String, Long>> loadWhitelistTypeCounts(
      Long tenantId, List<Integer> courseIds, List<Integer> whitelistIds) {
    if (whitelistIds == null || whitelistIds.isEmpty()) {
      return Map.of();
    }
    int tid = tenantId == null || tenantId <= 0 ? 1 : tenantId.intValue();
    // knowledgeId -> (type -> count) 叶子直接挂题
    Map<Integer, Map<String, Long>> direct = new HashMap<>();
    List<Map<String, Object>> raw =
        examDetailMapper.countPracticeQuestionsByKnowledgeAndTypeGrouped(tid);
    if (raw != null) {
      for (Map<String, Object> row : raw) {
        Integer kid = toInt(firstPresent(row, "knowledgeId", "knowledge_id", "KNOWLEDGEID"));
        String type = normalizeTypeKey(firstPresent(row, "questionType", "type", "TYPE"));
        long cnt = toLong(firstPresent(row, "cnt", "CNT", "count"));
        if (kid == null || type == null || cnt <= 0) {
          continue;
        }
        direct.computeIfAbsent(kid, k -> new HashMap<>()).merge(type, cnt, Long::sum);
      }
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
      for (ExamKnowledgeStructureEntity n : knowledgeStructureMapper.selectBatchIds(whitelistIds)) {
        if (n != null && n.getCourseId() != null && n.getCourseId() > 0) {
          courseHint.add(n.getCourseId());
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

    Map<Integer, Map<String, Long>> out = new LinkedHashMap<>();
    for (Integer root : whitelistIds) {
      if (root == null || root <= 0) {
        continue;
      }
      Map<String, Long> sum = new LinkedHashMap<>();
      sum.put("single", 0L);
      sum.put("multi", 0L);
      sum.put("judge", 0L);
      ArrayDeque<Integer> dq = new ArrayDeque<>();
      Set<Integer> seen = new HashSet<>();
      dq.add(root);
      while (!dq.isEmpty()) {
        Integer cur = dq.poll();
        if (cur == null || !seen.add(cur)) {
          continue;
        }
        Map<String, Long> d = direct.get(cur);
        if (d != null) {
          for (Map.Entry<String, Long> e : d.entrySet()) {
            sum.merge(e.getKey(), e.getValue(), Long::sum);
          }
        }
        for (Integer c : children.getOrDefault(cur, List.of())) {
          dq.add(c);
        }
      }
      out.put(root, sum);
    }
    return out;
  }

  private static String normalizeTypeKey(Object raw) {
    if (raw == null) {
      return null;
    }
    String t = String.valueOf(raw).trim().toLowerCase();
    return switch (t) {
      case "single", "danxuan", "单选", "1" -> "single";
      case "multi", "duoxuan", "多选", "2" -> "multi";
      case "judge", "pandan", "判断", "3" -> "judge";
      default -> null;
    };
  }

  private static Object firstPresent(Map<String, Object> row, String... keys) {
    if (row == null || keys == null) {
      return null;
    }
    for (String k : keys) {
      if (row.containsKey(k) && row.get(k) != null) {
        return row.get(k);
      }
    }
    // 兼容部分驱动返回大小写不一的 key
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

  private static Integer toInt(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(o).trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static long toLong(Object o) {
    if (o == null) {
      return 0L;
    }
    if (o instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(o).trim());
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private static Set<Integer> normalizeIds(List<Integer> raw) {
    if (raw == null || raw.isEmpty()) {
      return Set.of();
    }
    Set<Integer> out = new LinkedHashSet<>();
    for (Integer id : raw) {
      if (id != null && id > 0) {
        out.add(id);
      }
    }
    return out;
  }
}
