package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.dto.ExamMakeExamPlanCreateReq;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.entity.ExamMakeExamPlanEntity;
import com.guoyu.platform.exam.entity.ExamPaperDetailEntity;
import com.guoyu.platform.exam.entity.ExamPaperEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.mapper.ExamMakeExamPlanMapper;
import com.guoyu.platform.exam.mapper.ExamPaperDetailMapper;
import com.guoyu.platform.exam.mapper.ExamPaperMapper;
import com.guoyu.platform.exam.service.ExamMakeExamPlanApplicationService;
import com.guoyu.platform.exam.vo.ExamMakeExamPlanVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.FollowBankDrawReq;
import com.guoyu.platform.trainingdepartment.dto.FollowMockFromPlanReq;
import com.guoyu.platform.trainingdepartment.dto.FollowMockWithPlanReq;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskAnswerReq;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskQuestionReq;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskSaveReq;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskSubmitReq;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeCourseEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeKnowledgeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowTaskAnswerEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowTaskEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowTaskItemEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowTaskQuestionEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeCourseMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeKnowledgeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowTaskAnswerMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowTaskItemMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowTaskMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowTaskQuestionMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleMapper;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import com.guoyu.platform.trainingdepartment.vo.FollowStudentTaskVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskAnswerResultVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskItemVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskQuestionStatVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskQuestionVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskResultVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskStatsVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskVO;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 跟班作业/随堂练 —— 《跟班作业与随堂练实现方案》M3-a/b。
 */
@Service
@Slf4j
public class TrainFollowTaskApplicationService {

  public static final String STATUS_DRAFT = "draft";
  public static final String STATUS_PUBLISHED = "published";
  public static final String STATUS_CLOSED = "closed";

  public static final String ITEM_PENDING = "pending";
  public static final String ITEM_SUBMITTED = "submitted";
  public static final String ITEM_GRADED = "graded";

  public static final String TYPE_HOMEWORK = "homework";
  public static final String TYPE_PRACTICE = "practice";
  /** 班级模拟考（跟班同卷；答卷仍落 follow 四表） */
  public static final String TYPE_MOCK = "mock";
  /** 列表过滤：作业 Tab / 老师布置（不含模拟考） */
  public static final String FILTER_ASSIGNMENT = "assignment";

  private static final Set<String> TASK_TYPES = Set.of(TYPE_HOMEWORK, TYPE_PRACTICE, TYPE_MOCK);
  private static final Set<String> Q_TYPES = Set.of("single", "multi", "judge");

  private static final Map<String, String> TASK_TYPE_NAMES = Map.of(
      TYPE_HOMEWORK, "课堂作业",
      TYPE_PRACTICE, "随堂练",
      TYPE_MOCK, "模拟考");
  private static final Map<String, String> STATUS_NAMES = Map.of(
      STATUS_DRAFT, "草稿",
      STATUS_PUBLISHED, "已发布",
      STATUS_CLOSED, "已关闭");
  private static final Map<String, String> ITEM_STATUS_NAMES = Map.of(
      ITEM_PENDING, "未交",
      ITEM_SUBMITTED, "已交",
      ITEM_GRADED, "已批");

  private final TrainFollowTaskMapper taskMapper;
  private final TrainFollowTaskQuestionMapper questionMapper;
  private final TrainFollowTaskItemMapper itemMapper;
  private final TrainFollowTaskAnswerMapper answerMapper;
  private final TrainInfoClassMapper classMapper;
  private final TrainPeopleListMapper peopleMapper;
  private final TrainScheduleMapper scheduleMapper;
  private final TrainClassRoleApplicationService roleService;
  private final ExamExamDetailMapper examDetailMapper;
  private final ExamCourseMapper examCourseMapper;
  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;
  private final TrainClassPracticeCourseMapper practiceCourseMapper;
  private final TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper;
  private final FollowTaskBankQuestionConverter bankConverter;
  private final ObjectMapper objectMapper;
  private final ExamMakeExamPlanApplicationService makeExamPlanService;
  private final ExamMakeExamPlanMapper makeExamPlanMapper;
  private final ExamPaperMapper examPaperMapper;
  private final ExamPaperDetailMapper examPaperDetailMapper;

  public TrainFollowTaskApplicationService(
      TrainFollowTaskMapper taskMapper,
      TrainFollowTaskQuestionMapper questionMapper,
      TrainFollowTaskItemMapper itemMapper,
      TrainFollowTaskAnswerMapper answerMapper,
      TrainInfoClassMapper classMapper,
      TrainPeopleListMapper peopleMapper,
      TrainScheduleMapper scheduleMapper,
      TrainClassRoleApplicationService roleService,
      ExamExamDetailMapper examDetailMapper,
      ExamCourseMapper examCourseMapper,
      ExamKnowledgeStructureMapper knowledgeStructureMapper,
      TrainClassPracticeCourseMapper practiceCourseMapper,
      TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper,
      FollowTaskBankQuestionConverter bankConverter,
      ObjectMapper objectMapper,
      ExamMakeExamPlanApplicationService makeExamPlanService,
      ExamMakeExamPlanMapper makeExamPlanMapper,
      ExamPaperMapper examPaperMapper,
      ExamPaperDetailMapper examPaperDetailMapper) {
    this.taskMapper = taskMapper;
    this.questionMapper = questionMapper;
    this.itemMapper = itemMapper;
    this.answerMapper = answerMapper;
    this.classMapper = classMapper;
    this.peopleMapper = peopleMapper;
    this.scheduleMapper = scheduleMapper;
    this.roleService = roleService;
    this.examDetailMapper = examDetailMapper;
    this.examCourseMapper = examCourseMapper;
    this.knowledgeStructureMapper = knowledgeStructureMapper;
    this.practiceCourseMapper = practiceCourseMapper;
    this.practiceKnowledgeMapper = practiceKnowledgeMapper;
    this.bankConverter = bankConverter;
    this.objectMapper = objectMapper;
    this.makeExamPlanService = makeExamPlanService;
    this.makeExamPlanMapper = makeExamPlanMapper;
    this.examPaperMapper = examPaperMapper;
    this.examPaperDetailMapper = examPaperDetailMapper;
  }

  // ---------- 老师 App ----------

  @Transactional
  public FollowTaskVO createDraft(Long platformUserId, FollowTaskSaveReq req) {
    Long classId = req.getClassId();
    if (classId == null || classId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "classId 无效");
    }
    assertTeacher(platformUserId, classId);
    TrainInfoClassEntity clazz = requireClass(classId);
    String taskType = normalizeTaskType(req.getTaskType());
    validateMount(req, taskType);
    Long tenantId = resolveTenantId(classId);

    TrainFollowTaskEntity task = new TrainFollowTaskEntity();
    fillTaskFromReq(task, req, clazz, taskType, tenantId, platformUserId);
    task.setStatus(STATUS_DRAFT);
    taskMapper.insert(task);
    replaceQuestions(task, resolveQuestions(req, classId, tenantId), platformUserId);
    return toTaskVO(task, true, false);
  }

  @Transactional
  public FollowTaskVO updateDraft(Long platformUserId, Long taskId, FollowTaskSaveReq req) {
    TrainFollowTaskEntity task = requireTask(taskId);
    assertTeacher(platformUserId, task.getClassId());
    if (!STATUS_DRAFT.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可修改");
    }
    if (!Objects.equals(task.getCreatedBy(), platformUserId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅布置人可修改草稿");
    }
    String taskType = normalizeTaskType(req.getTaskType());
    validateMount(req, taskType);
    TrainInfoClassEntity clazz = requireClass(task.getClassId());
    fillTaskFromReq(task, req, clazz, taskType, task.getTenantId(), platformUserId);
    taskMapper.updateById(task);
    replaceQuestions(task, resolveQuestions(req, task.getClassId(), task.getTenantId()), platformUserId);
    return toTaskVO(task, true, false);
  }

  /**
   * 老师选题：按班级练习范围筛选客观题题库。
   * <p>科目关联优先走知识点树 {@code exam_knowledge_structure.course_id}，
   * 兼容 {@code exam_exam_detail.exam_id} 未回填（常为 0）的存量数据。
   */
  @Transactional(readOnly = true)
  public Map<String, Object> searchBankQuestions(
      Long platformUserId,
      Long classId,
      Integer courseId,
      Integer knowledgeId,
      String questionType,
      String keyword,
      int page,
      int pageSize) {
    assertTeacher(platformUserId, classId);
    requireClass(classId);
    Long tenantId = resolveTenantId(classId);
    ScopeFilter scope = loadScopeFilter(classId, tenantId);

    int p = Math.max(page, 1);
    int size = Math.min(Math.max(pageSize, 1), 50);

    List<Integer> scopedCourses = !scope.courseIds.isEmpty()
        ? scope.courseIds
        : scope.fallbackCourseIds;
    List<Integer> filterCourses;
    if (courseId != null && courseId > 0) {
      if (!scope.courseIds.isEmpty() && !scope.courseIds.contains(courseId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "科目不在本班练习范围内");
      }
      if (scope.courseIds.isEmpty() && !scopedCourses.isEmpty() && !scopedCourses.contains(courseId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "科目不在推荐范围内");
      }
      filterCourses = List.of(courseId);
    } else {
      filterCourses = scopedCourses;
    }

    BankScopeQuery bankScope = resolveBankScopeQuery(filterCourses, scope.knowledgeIds, knowledgeId);
    Map<String, Object> emptyBase = new LinkedHashMap<>();
    emptyBase.put("classId", classId);
    emptyBase.put("page", p);
    emptyBase.put("pageSize", size);
    emptyBase.put("scopeConfigured", scope.configured);
    emptyBase.put("courseIds", scopedCourses);
    emptyBase.put("knowledgeIds", scope.knowledgeIds);
    emptyBase.put("items", List.of());

    if (bankScope.impossible()) {
      emptyBase.put("total", 0L);
      emptyBase.put("typeCounts", emptyTypeCounts());
      emptyBase.put("emptyHint", bankScope.emptyHint() != null
          ? bankScope.emptyHint()
          : "当前科目/知识点下暂无可用客观题");
      return emptyBase;
    }

    LambdaQueryWrapper<ExamExamDetailEntity> q = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getDeleted, 0)
        .eq(ExamExamDetailEntity::getStatus, 1)
        .in(ExamExamDetailEntity::getType, List.of(
            "single", "multi", "judge", "danxuan", "duoxuan", "pandan"))
        .orderByDesc(ExamExamDetailEntity::getId);
    if (tenantId != null && tenantId > 0) {
      q.eq(ExamExamDetailEntity::getTenantId, tenantId.intValue());
    }
    applyBankScopeToWrapper(q, bankScope);
    if (StringUtils.hasText(questionType)) {
      String nt = FollowTaskBankQuestionConverter.normalizeType(questionType);
      if (FollowTaskBankQuestionConverter.isObjective(questionType)) {
        q.in(ExamExamDetailEntity::getType, typeAliases(nt));
      }
    }
    if (StringUtils.hasText(keyword)) {
      q.like(ExamExamDetailEntity::getContent, keyword.trim());
    }

    Page<ExamExamDetailEntity> mp = examDetailMapper.selectPage(new Page<>(p, size), q);
    Map<Integer, Integer> knowledgeCourseMap = loadKnowledgeCourseMap(
        mp.getRecords().stream().map(ExamExamDetailEntity::getKnowledgeId).toList());
    List<Map<String, Object>> items = new ArrayList<>();
    for (ExamExamDetailEntity d : mp.getRecords()) {
      if (!FollowTaskBankQuestionConverter.isObjective(d.getType())) {
        continue;
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", d.getId());
      row.put("courseId", resolveQuestionCourseId(d, filterCourses, knowledgeCourseMap));
      row.put("knowledgeId", d.getKnowledgeId());
      row.put("questionType", FollowTaskBankQuestionConverter.normalizeType(d.getType()));
      row.put("difficulty", d.getDifficulty());
      row.put("score", d.getScore());
      row.put("stemPreview", stemPreviewFromContent(d.getContent()));
      items.add(row);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("page", p);
    out.put("pageSize", size);
    out.put("total", mp.getTotal());
    out.put("scopeConfigured", scope.configured);
    out.put("courseIds", scopedCourses);
    out.put("knowledgeIds", scope.knowledgeIds);
    out.put("typeCounts", aggregateTypeCounts(tenantId, bankScope));
    out.put("items", items);
    if (mp.getTotal() == 0) {
      out.put("emptyHint", buildBankEmptyHint(scope, filterCourses, keyword, questionType));
    }
    return out;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> bankScopeMeta(Long platformUserId, Long classId) {
    assertTeacher(platformUserId, classId);
    return bankScopeMetaInternal(classId);
  }

  private Map<String, Object> bankScopeMetaInternal(Long classId) {
    requireClass(classId);
    Long tenantId = resolveTenantId(classId);
    ScopeFilter scope = loadScopeFilter(classId, tenantId);
    List<Integer> courseIds = scope.courseIds.isEmpty() ? scope.fallbackCourseIds : scope.courseIds;
    Map<Integer, String> titles = Map.of();
    if (!courseIds.isEmpty()) {
      titles = examCourseMapper.selectBatchIds(courseIds).stream()
          .collect(Collectors.toMap(ExamCourseEntity::getId, ExamCourseEntity::getTitle, (a, b) -> a));
    }
    List<Map<String, Object>> courses = new ArrayList<>();
    for (Integer id : courseIds) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("courseId", id);
      row.put("title", titles.getOrDefault(id, "#" + id));
      courses.add(row);
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("scopeConfigured", scope.configured);
    out.put("courses", courses);
    out.put("knowledgeIds", scope.knowledgeIds);
    out.put("hint", scope.configured
        ? "按本班练习范围选题（科目经知识点树关联）"
        : (courses.isEmpty() ? "未配置练习范围，将检索本租户客观题" : "未配置范围，已用工种推荐科目筛选"));
    if (courses.isEmpty() && !scope.configured) {
      out.put("emptyHint", "未配置练习范围且无推荐科目，暂无科目可筛。请先在后台配置班级练习范围");
    }
    return out;
  }

  /**
   * 题库选题：某科目下的知识点分类树。
   * 有白名单时裁剪为「白名单节点 + 其祖先」（便于导航）；选题时选中父节点会展开为子孙白名单。
   */
  @Transactional(readOnly = true)
  public Map<String, Object> bankKnowledgeTree(Long platformUserId, Long classId, Integer courseId) {
    assertTeacher(platformUserId, classId);
    requireClass(classId);
    if (courseId == null || courseId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请指定科目");
    }
    Long tenantId = resolveTenantId(classId);
    ScopeFilter scope = loadScopeFilter(classId, tenantId);
    List<Integer> allowedCourses = !scope.courseIds.isEmpty()
        ? scope.courseIds
        : scope.fallbackCourseIds;
    if (!allowedCourses.isEmpty() && !allowedCourses.contains(courseId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "科目不在本班练习/推荐范围内");
    }

    List<ExamKnowledgeStructureEntity> all = knowledgeStructureMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .eq(ExamKnowledgeStructureEntity::getCourseId, courseId)
            .eq(ExamKnowledgeStructureEntity::getDeleted, 0)
            .orderByAsc(ExamKnowledgeStructureEntity::getSortNumber)
            .orderByAsc(ExamKnowledgeStructureEntity::getId));
    Set<Integer> whitelist = scope.knowledgeIds.isEmpty()
        ? Set.of()
        : new HashSet<>(scope.knowledgeIds);

    Map<Integer, List<ExamKnowledgeStructureEntity>> byParent = new LinkedHashMap<>();
    for (ExamKnowledgeStructureEntity n : all) {
      int pid = n.getParentId() == null ? 0 : n.getParentId();
      byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(n);
    }
    Set<Integer> keepIds = resolveKnowledgeTreeKeepIds(all, whitelist);
    List<Map<String, Object>> tree = buildBankKnowledgeNodes(byParent, 0, keepIds, whitelist);

    BankScopeQuery courseScope = resolveBankScopeQuery(List.of(courseId), scope.knowledgeIds, null);
    Map<Integer, Integer> directCounts = Map.of();
    Map<String, Object> typeCounts = emptyTypeCounts();
    if (!courseScope.impossible()) {
      directCounts = aggregateDirectKnowledgeCounts(tenantId, courseScope);
      fillKnowledgeQuestionCounts(tree, directCounts);
      typeCounts = aggregateTypeCounts(tenantId, courseScope);
    } else {
      fillKnowledgeQuestionCounts(tree, Map.of());
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("courseId", courseId);
    out.put("whitelistOnly", !whitelist.isEmpty());
    out.put("tree", tree);
    out.put("typeCounts", typeCounts);
    out.put("hint", tree.isEmpty()
        ? "该科目下暂无知识点分类"
        : (whitelist.isEmpty() ? "按知识点分类选题" : "仅展示本班练习范围知识点"));
    return out;
  }

  /**
   * 智能抽题：按当前科目/知识点范围，分别配置单选/多选/判断抽取数量。
   */
  @Transactional(readOnly = true)
  public Map<String, Object> drawBankQuestions(
      Long platformUserId, Long classId, FollowBankDrawReq req) {
    assertTeacher(platformUserId, classId);
    return drawBankQuestionsInternal(classId, req);
  }

  /** 管理端抽题（PreAuthorize 控权，不校验 App 老师角色） */
  @Transactional(readOnly = true)
  public Map<String, Object> drawBankQuestionsAdmin(Long classId, FollowBankDrawReq req) {
    requireClass(classId);
    return drawBankQuestionsInternal(classId, req);
  }

  private Map<String, Object> drawBankQuestionsInternal(Long classId, FollowBankDrawReq req) {
    if (req == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请配置抽题数量");
    }
    int singleN = clampDrawCount(req.getSingleCount());
    int multiN = clampDrawCount(req.getMultiCount());
    int judgeN = clampDrawCount(req.getJudgeCount());
    if (singleN + multiN + judgeN <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少配置一种题型的抽题数量");
    }

    Long tenantId = resolveTenantId(classId);
    ScopeFilter scope = loadScopeFilter(classId, tenantId);
    List<Integer> scopedCourses = !scope.courseIds.isEmpty()
        ? scope.courseIds
        : scope.fallbackCourseIds;
    List<Integer> filterCourses = resolveFilterCourses(req.getCourseId(), scope, scopedCourses);
    BankScopeQuery bankScope = resolveBankScopeQuery(filterCourses, scope.knowledgeIds, req.getKnowledgeId());

    Map<String, Object> drawn = new LinkedHashMap<>();
    drawn.put("single", 0);
    drawn.put("multi", 0);
    drawn.put("judge", 0);
    Map<String, Object> shortage = new LinkedHashMap<>();
    List<ExamExamDetailEntity> picked = new ArrayList<>();
    Set<Integer> exclude = new HashSet<>();
    if (req.getExcludeIds() != null) {
      for (Integer id : req.getExcludeIds()) {
        if (id != null && id > 0) {
          exclude.add(id);
        }
      }
    }
    if (Boolean.TRUE.equals(req.getExcludeOtherMock())) {
      exclude.addAll(listMockUsedExamDetailIds(classId, req.getExcludeTaskId()));
    }

    if (bankScope.impossible()) {
      Map<String, Object> empty = new LinkedHashMap<>();
      empty.put("classId", classId);
      empty.put("items", List.of());
      empty.put("drawn", drawn);
      empty.put("shortage", Map.of(
          "single", singleN,
          "multi", multiN,
          "judge", judgeN));
      empty.put("typeCounts", emptyTypeCounts());
      empty.put("emptyHint", bankScope.emptyHint() != null
          ? bankScope.emptyHint()
          : "当前范围内无可抽客观题");
      return empty;
    }

    picked.addAll(randomPickByType(tenantId, bankScope, "single", singleN, exclude, drawn, shortage));
    picked.addAll(randomPickByType(tenantId, bankScope, "multi", multiN, exclude, drawn, shortage));
    picked.addAll(randomPickByType(tenantId, bankScope, "judge", judgeN, exclude, drawn, shortage));

    Map<Integer, Integer> knowledgeCourseMap = loadKnowledgeCourseMap(
        picked.stream().map(ExamExamDetailEntity::getKnowledgeId).toList());
    List<Map<String, Object>> items = new ArrayList<>();
    for (ExamExamDetailEntity d : picked) {
      items.add(toBankItemView(d, filterCourses, knowledgeCourseMap));
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("courseId", req.getCourseId());
    out.put("knowledgeId", req.getKnowledgeId());
    out.put("items", items);
    out.put("drawn", drawn);
    out.put("shortage", shortage);
    out.put("typeCounts", aggregateTypeCounts(tenantId, bankScope));
    int want = singleN + multiN + judgeN;
    if (items.size() < want) {
      out.put("hint", "题库不足，已尽量抽取 " + items.size() + "/" + want + " 题");
    } else {
      out.put("hint", "已抽取 " + items.size() + " 题");
    }
    return out;
  }

  private static int clampDrawCount(Integer raw) {
    if (raw == null || raw <= 0) {
      return 0;
    }
    return Math.min(raw, 50);
  }

  private List<Integer> resolveFilterCourses(
      Integer courseId, ScopeFilter scope, List<Integer> scopedCourses) {
    if (courseId != null && courseId > 0) {
      if (!scope.courseIds.isEmpty() && !scope.courseIds.contains(courseId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "科目不在本班练习范围内");
      }
      if (scope.courseIds.isEmpty() && !scopedCourses.isEmpty() && !scopedCourses.contains(courseId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "科目不在推荐范围内");
      }
      return List.of(courseId);
    }
    return scopedCourses;
  }

  private List<ExamExamDetailEntity> randomPickByType(
      Long tenantId,
      BankScopeQuery bankScope,
      String normalizedType,
      int need,
      Set<Integer> exclude,
      Map<String, Object> drawn,
      Map<String, Object> shortage) {
    if (need <= 0) {
      return List.of();
    }
    LambdaQueryWrapper<ExamExamDetailEntity> q = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getDeleted, 0)
        .eq(ExamExamDetailEntity::getStatus, 1)
        .in(ExamExamDetailEntity::getType, typeAliases(normalizedType))
        .select(
            ExamExamDetailEntity::getId,
            ExamExamDetailEntity::getExamId,
            ExamExamDetailEntity::getKnowledgeId,
            ExamExamDetailEntity::getType,
            ExamExamDetailEntity::getDifficulty,
            ExamExamDetailEntity::getScore,
            ExamExamDetailEntity::getContent);
    if (tenantId != null && tenantId > 0) {
      q.eq(ExamExamDetailEntity::getTenantId, tenantId.intValue());
    }
    applyBankScopeToWrapper(q, bankScope);
    if (!exclude.isEmpty()) {
      q.notIn(ExamExamDetailEntity::getId, exclude);
    }
    // 先取候选池再洗牌，避免 ORDER BY RAND() 在大表上全表排序
    int poolLimit = Math.min(Math.max(need * 20, 200), 800);
    q.last("LIMIT " + poolLimit);
    List<ExamExamDetailEntity> pool = new ArrayList<>(examDetailMapper.selectList(q));
    if (pool.isEmpty()) {
      shortage.put(normalizedType, need);
      drawn.put(normalizedType, 0);
      return List.of();
    }
    Collections.shuffle(pool, ThreadLocalRandom.current());
    int take = Math.min(need, pool.size());
    List<ExamExamDetailEntity> got = pool.subList(0, take);
    for (ExamExamDetailEntity d : got) {
      if (d.getId() != null) {
        exclude.add(d.getId());
      }
    }
    drawn.put(normalizedType, take);
    if (take < need) {
      shortage.put(normalizedType, need - take);
    }
    return new ArrayList<>(got);
  }

  private Map<String, Object> toBankItemView(
      ExamExamDetailEntity d,
      List<Integer> filterCourses,
      Map<Integer, Integer> knowledgeCourseMap) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", d.getId());
    row.put("courseId", resolveQuestionCourseId(d, filterCourses, knowledgeCourseMap));
    row.put("knowledgeId", d.getKnowledgeId());
    row.put("questionType", FollowTaskBankQuestionConverter.normalizeType(d.getType()));
    row.put("difficulty", d.getDifficulty());
    row.put("score", d.getScore());
    row.put("stemPreview", stemPreviewFromContent(d.getContent()));
    return row;
  }

  /** 题干预览：去掉 HTML 标签与多余空白，截断展示。 */
  private static String stemPreviewFromContent(String content) {
    if (content == null || content.isBlank()) {
      return "";
    }
    String first = content.split("\n", 2)[0];
    String stem = first
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
    if (stem.length() > 80) {
      stem = stem.substring(0, 80) + "…";
    }
    return stem;
  }

  @Transactional
  public FollowTaskVO publish(Long platformUserId, Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    assertTeacher(platformUserId, task.getClassId());
    return publishInternal(task, platformUserId);
  }

  @Transactional
  public FollowTaskVO close(Long platformUserId, Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    assertTeacher(platformUserId, task.getClassId());
    return closeInternal(task, platformUserId);
  }

  @Transactional
  public FollowTaskVO reopen(Long platformUserId, Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    assertTeacher(platformUserId, task.getClassId());
    return reopenInternal(task, platformUserId);
  }

  @Transactional(readOnly = true)
  public List<FollowTaskVO> listByClassForTeacher(Long platformUserId, Long classId) {
    assertTeacher(platformUserId, classId);
    return listByClassInternal(classId, true, FILTER_ASSIGNMENT);
  }

  @Transactional(readOnly = true)
  public List<FollowTaskItemVO> listItemsForTeacher(Long platformUserId, Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    assertTeacher(platformUserId, task.getClassId());
    return listItemsInternal(taskId);
  }

  // ---------- 学员 App ----------

  @Transactional(readOnly = true)
  public List<FollowStudentTaskVO> listStudentTasks(Long platformUserId, Long classId) {
    return listStudentTasks(platformUserId, classId, FILTER_ASSIGNMENT);
  }

  @Transactional(readOnly = true)
  public List<FollowStudentTaskVO> listStudentTasks(Long platformUserId, Long classId, String taskTypeFilter) {
    roleService.assertClassMember(platformUserId, classId);
    TrainPeopleListEntity people = requireStudentPeople(platformUserId, classId);
    LambdaQueryWrapper<TrainFollowTaskEntity> q = new LambdaQueryWrapper<TrainFollowTaskEntity>()
        .eq(TrainFollowTaskEntity::getClassId, classId)
        .in(TrainFollowTaskEntity::getStatus, STATUS_PUBLISHED, STATUS_CLOSED)
        .eq(TrainFollowTaskEntity::getDeleted, 0)
        .orderByDesc(TrainFollowTaskEntity::getPublishedAt);
    applyTaskTypeFilter(q, taskTypeFilter);
    List<TrainFollowTaskEntity> tasks = taskMapper.selectList(q);
    List<FollowStudentTaskVO> out = new ArrayList<>();
    for (TrainFollowTaskEntity t : tasks) {
      TrainFollowTaskItemEntity item = findItem(t.getId(), people.getId());
      if (item == null) {
        continue;
      }
      String itemStatus = item.getStatus();
      BigDecimal score = item.getScore();
      Boolean passed = item.getPassed() != null && item.getPassed() == 1;
      // 空答卷对学员展示为待做，便于从列表进入补交
      if ((ITEM_GRADED.equals(itemStatus) || ITEM_SUBMITTED.equals(itemStatus))
          && !hasAnyAnswerContent(item.getId())) {
        itemStatus = ITEM_PENDING;
        score = null;
        passed = false;
      }
      out.add(new FollowStudentTaskVO(
          String.valueOf(t.getId()),
          t.getClassId(),
          t.getTaskType(),
          TASK_TYPE_NAMES.getOrDefault(t.getTaskType(), t.getTaskType()),
          t.getTitle(),
          t.getStatus(),
          itemStatus,
          ITEM_STATUS_NAMES.getOrDefault(itemStatus, itemStatus),
          score,
          passed,
          Objects.equals(t.getShowAnalysisAfterSubmit(), 1),
          t.getDeadlineAt() == null ? null : t.getDeadlineAt().toString(),
          t.getPublishedAt() == null ? null : t.getPublishedAt().toString()));
    }
    return out;
  }

  @Transactional
  public Map<String, Object> getPaper(Long platformUserId, Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    if (!STATUS_PUBLISHED.equals(task.getStatus()) && !STATUS_CLOSED.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务未发布");
    }
    TrainPeopleListEntity people = requireStudentPeople(platformUserId, task.getClassId());
    TrainFollowTaskItemEntity item = findItem(taskId, people.getId());
    if (item == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "不在应交名单");
    }
    // 开卷即记 started_at（幂等），用于答题时长
    markStartedIfNeeded(item, platformUserId);

    List<FollowTaskQuestionVO> qs = listQuestions(taskId).stream()
        .map(q -> toQuestionVO(q, false))
        .toList();
    // 已交卷但作答内容全空（常见于客户端 Number(雪花ID) 精度丢失）：对前端视为待作答，便于补交
    String itemStatus = item.getStatus();
    if ((ITEM_GRADED.equals(itemStatus) || ITEM_SUBMITTED.equals(itemStatus))
        && !hasAnyAnswerContent(item.getId())) {
      itemStatus = ITEM_PENDING;
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("taskId", String.valueOf(task.getId()));
    out.put("title", task.getTitle());
    out.put("taskType", task.getTaskType());
    out.put("description", task.getDescription());
    out.put("itemStatus", itemStatus);
    out.put("startedAt", item.getStartedAt() == null ? null : item.getStartedAt().toString());
    out.put("questions", qs);
    return out;
  }

  /** 学员显式开答（幂等）；也可由 getPaper 隐式触发 */
  @Transactional
  public Map<String, Object> startTask(Long platformUserId, Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    if (!STATUS_PUBLISHED.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务未开放");
    }
    TrainPeopleListEntity people = requireStudentPeople(platformUserId, task.getClassId());
    TrainFollowTaskItemEntity item = findItem(taskId, people.getId());
    if (item == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "不在应交名单");
    }
    markStartedIfNeeded(item, platformUserId);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("taskId", String.valueOf(taskId));
    out.put("itemId", String.valueOf(item.getId()));
    out.put("startedAt", item.getStartedAt() == null ? null : item.getStartedAt().toString());
    return out;
  }

  @Transactional
  public FollowTaskResultVO submit(Long platformUserId, Long taskId, FollowTaskSubmitReq req) {
    TrainFollowTaskEntity task = requireTask(taskId);
    if (!STATUS_PUBLISHED.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务未开放提交");
    }
    TrainPeopleListEntity people = requireStudentPeople(platformUserId, task.getClassId());
    TrainFollowTaskItemEntity item = findItem(taskId, people.getId());
    if (item == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "不在应交名单");
    }
    boolean alreadyDone = ITEM_GRADED.equals(item.getStatus()) || ITEM_SUBMITTED.equals(item.getStatus());
    if (alreadyDone && hasAnyAnswerContent(item.getId())) {
      if (TYPE_MOCK.equals(task.getTaskType())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "模拟考不可重考");
      }
      // 作业：正常已交卷直接回看结果
      return buildResult(task, item, true);
    }

    List<TrainFollowTaskQuestionEntity> questions = listQuestions(taskId);
    Map<Long, FollowTaskAnswerReq> answerMap = new HashMap<>();
    for (FollowTaskAnswerReq a : req.getAnswers()) {
      if (a.getQuestionId() != null) {
        answerMap.put(a.getQuestionId(), a);
      }
    }

    // 补交空答卷时原地 UPDATE，避免 uk_tfta_item_question + @TableLogic 软删失效导致重复插入
    Map<Long, TrainFollowTaskAnswerEntity> existingByQuestion = answerMapper.selectList(
            new LambdaQueryWrapper<TrainFollowTaskAnswerEntity>()
                .eq(TrainFollowTaskAnswerEntity::getItemId, item.getId())
                .eq(TrainFollowTaskAnswerEntity::getDeleted, 0))
        .stream()
        .collect(Collectors.toMap(TrainFollowTaskAnswerEntity::getQuestionId, a -> a, (a, b) -> a));

    BigDecimal total = BigDecimal.ZERO;
    LocalDateTime now = LocalDateTime.now();
    for (TrainFollowTaskQuestionEntity q : questions) {
      FollowTaskAnswerReq ans = answerMap.get(q.getId());
      Object userAns = ans == null ? null : ans.getAnswer();
      GradeResult gr = grade(q, userAns);
      String answerJson = toJson(userAns);
      TrainFollowTaskAnswerEntity row = existingByQuestion.get(q.getId());
      if (row == null) {
        row = new TrainFollowTaskAnswerEntity();
        row.setTaskId(taskId);
        row.setItemId(item.getId());
        row.setQuestionId(q.getId());
        row.setPeopleId(people.getId());
        row.setAnswerJson(answerJson);
        row.setIsCorrect(gr.correct ? 1 : 0);
        row.setScore(gr.score);
        row.setTenantId(task.getTenantId());
        row.setCreatedBy(platformUserId);
        row.setUpdatedBy(platformUserId);
        row.setDeleted(0);
        answerMapper.insert(row);
      } else {
        row.setAnswerJson(answerJson);
        row.setIsCorrect(gr.correct ? 1 : 0);
        row.setScore(gr.score);
        row.setUpdatedBy(platformUserId);
        answerMapper.updateById(row);
      }
      total = total.add(gr.score);
    }

    item.setScore(total);
    item.setStatus(ITEM_GRADED);
    item.setSubmittedAt(now);
    item.setGradedAt(now);
    item.setGradedBy(0L);
    item.setPassed(isPassed(task, total) ? 1 : 0);
    if (item.getStartedAt() == null) {
      item.setStartedAt(now);
    }
    long secs = ChronoUnit.SECONDS.between(item.getStartedAt(), now);
    item.setDurationSeconds((int) Math.max(0, Math.min(secs, Integer.MAX_VALUE)));
    item.setUpdatedBy(platformUserId);
    itemMapper.updateById(item);

    // 交卷后始终返回学员作答与正确答案对照（解析文案仍随任务配置）
    return buildResult(task, item, true);
  }

  @Transactional(readOnly = true)
  public FollowTaskResultVO getResult(Long platformUserId, Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    TrainPeopleListEntity people = requireStudentPeople(platformUserId, task.getClassId());
    TrainFollowTaskItemEntity item = findItem(taskId, people.getId());
    if (item == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "不在应交名单");
    }
    if (ITEM_PENDING.equals(item.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "尚未交卷");
    }
    // 已交卷：始终展示作答对照；解析文案在任务开启「交卷后显示解析」或任务关闭后展示
    return buildResult(task, item, true);
  }

  // ---------- 管理端 ----------

  @Transactional(readOnly = true)
  public List<FollowTaskVO> listByClassAdmin(Long classId) {
    return listByClassAdmin(classId, FILTER_ASSIGNMENT);
  }

  @Transactional(readOnly = true)
  public List<FollowTaskVO> listByClassAdmin(Long classId, String taskTypeFilter) {
    requireClass(classId);
    return listByClassInternal(classId, true, taskTypeFilter);
  }

  /** 管理端创建草稿（模拟考配卷等） */
  @Transactional
  public FollowTaskVO createDraftAdmin(FollowTaskSaveReq req) {
    Long classId = req.getClassId();
    if (classId == null || classId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "classId 无效");
    }
    TrainInfoClassEntity clazz = requireClass(classId);
    String taskType = normalizeTaskType(req.getTaskType());
    validateMount(req, taskType);
    Long tenantId = resolveTenantId(classId);
    Long userId = currentAdminUserId();

    TrainFollowTaskEntity task = new TrainFollowTaskEntity();
    fillTaskFromReq(task, req, clazz, taskType, tenantId, userId);
    task.setStatus(STATUS_DRAFT);
    taskMapper.insert(task);
    replaceQuestions(task, resolveQuestions(req, classId, tenantId), userId);
    return toTaskVO(task, true, false);
  }

  /**
   * 跟班添加组卷：调用题库组卷计划 create，再快照为本班 mock 草稿。
   * 同一事务：挂接失败则计划一并回滚。
   */
  @Transactional
  public Map<String, Object> createMockWithPlanAdmin(Long classId, FollowMockWithPlanReq req) {
    requireClass(classId);
    if (req == null || req.getPlan() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写组卷计划参数");
    }
    ExamMakeExamPlanCreateReq planReq = req.getPlan();
    if (planReq.examType() == null || planReq.examType() != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "跟班模拟考仅支持理论组卷");
    }
    assertCourseAllowedForClass(classId, planReq.courseId());
    ExamMakeExamPlanVO plan = makeExamPlanService.create(planReq);
    return attachMockFromMakePlan(
        classId,
        plan.getId(),
        StringUtils.hasText(req.getTitle()) ? req.getTitle().trim() : plan.getTitle(),
        req.getDescription(),
        req.getBizDate(),
        req.getPassScore() != null
            ? req.getPassScore()
            : (plan.getPassPoint() != null ? BigDecimal.valueOf(plan.getPassPoint()) : null),
        req.getShowAnalysisAfterSubmit(),
        !Boolean.FALSE.equals(req.getExcludeOtherMock()));
  }

  /** 选用已有组卷计划，快照为本班 mock 草稿（不新建计划）。 */
  @Transactional
  public Map<String, Object> createMockFromPlanAdmin(Long classId, FollowMockFromPlanReq req) {
    requireClass(classId);
    if (req == null || req.getMakeExamPlanId() == null || req.getMakeExamPlanId() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择组卷计划");
    }
    ExamMakeExamPlanEntity plan = makeExamPlanMapper.selectById(req.getMakeExamPlanId());
    if (plan == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "组卷计划不存在");
    }
    if (plan.getExamType() == null || plan.getExamType() != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "跟班模拟考仅支持理论组卷计划");
    }
    assertCourseAllowedForClass(classId, plan.getCourseId());
    return attachMockFromMakePlan(
        classId,
        plan.getId(),
        StringUtils.hasText(req.getTitle()) ? req.getTitle().trim() : plan.getTitle(),
        req.getDescription(),
        req.getBizDate(),
        req.getPassScore() != null
            ? req.getPassScore()
            : (plan.getPassPoint() != null ? BigDecimal.valueOf(plan.getPassPoint()) : null),
        req.getShowAnalysisAfterSubmit(),
        !Boolean.FALSE.equals(req.getExcludeOtherMock()));
  }

  /** 本班可用的理论组卷计划下拉（科目落在本班练习/推荐范围内）。 */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> listMockPlanOptionsAdmin(Long classId) {
    requireClass(classId);
    Long tenantId = resolveTenantId(classId);
    ScopeFilter scope = loadScopeFilter(classId, tenantId);
    List<Integer> courseIds = scope.courseIds.isEmpty() ? scope.fallbackCourseIds : scope.courseIds;

    LambdaQueryWrapper<ExamMakeExamPlanEntity> qw = new LambdaQueryWrapper<ExamMakeExamPlanEntity>()
        .eq(ExamMakeExamPlanEntity::getExamType, 0)
        .orderByDesc(ExamMakeExamPlanEntity::getId)
        .last("LIMIT 200");
    if (tenantId != null && tenantId > 0) {
      qw.eq(ExamMakeExamPlanEntity::getTenantId, tenantId.intValue());
    }
    if (!courseIds.isEmpty()) {
      qw.in(ExamMakeExamPlanEntity::getCourseId, courseIds);
    }
    List<ExamMakeExamPlanEntity> plans = makeExamPlanMapper.selectList(qw);
    Map<Integer, String> courseTitles = Map.of();
    if (!plans.isEmpty()) {
      Set<Integer> ids = plans.stream()
          .map(ExamMakeExamPlanEntity::getCourseId)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
      if (!ids.isEmpty()) {
        courseTitles = examCourseMapper.selectBatchIds(ids).stream()
            .collect(Collectors.toMap(ExamCourseEntity::getId, ExamCourseEntity::getTitle, (a, b) -> a));
      }
    }
    List<Map<String, Object>> out = new ArrayList<>();
    for (ExamMakeExamPlanEntity p : plans) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", p.getId());
      row.put("title", p.getTitle());
      row.put("courseId", p.getCourseId());
      row.put("courseTitle", courseTitles.getOrDefault(p.getCourseId(), "#" + p.getCourseId()));
      row.put("totalPoint", p.getTotalPoint());
      row.put("passPoint", p.getPassPoint());
      row.put("time", p.getTime());
      row.put("createTime", p.getCreateTime());
      out.add(row);
    }
    return out;
  }

  private Map<String, Object> attachMockFromMakePlan(
      Long classId,
      Integer makeExamPlanId,
      String title,
      String description,
      String bizDate,
      BigDecimal passScore,
      Boolean showAnalysisAfterSubmit,
      boolean excludeOtherMock) {
    ExamMakeExamPlanEntity plan = makeExamPlanMapper.selectById(makeExamPlanId);
    if (plan == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "组卷计划不存在");
    }
    ExamPaperEntity paper = examPaperMapper.selectOne(new LambdaQueryWrapper<ExamPaperEntity>()
        .eq(ExamPaperEntity::getMakeId, makeExamPlanId)
        .orderByDesc(ExamPaperEntity::getId)
        .last("LIMIT 1"));
    if (paper == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该组卷计划尚未生成正式卷，请先在题库完成组卷");
    }
    List<ExamPaperDetailEntity> paperDetails = examPaperDetailMapper.selectList(
        new LambdaQueryWrapper<ExamPaperDetailEntity>()
            .eq(ExamPaperDetailEntity::getPaperId, paper.getId())
            .orderByAsc(ExamPaperDetailEntity::getSortNumber)
            .orderByAsc(ExamPaperDetailEntity::getId));
    if (paperDetails.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "正式卷无题目，无法挂接模拟考。请确认该科目有终审通过试题，或自定义模式已配置题型");
    }

    LinkedHashSet<Integer> bankIds = new LinkedHashSet<>();
    int subjectiveSkipped = 0;
    for (ExamPaperDetailEntity pd : paperDetails) {
      if (pd.getExamId() == null || pd.getExamId() <= 0) {
        continue;
      }
      if (!FollowTaskBankQuestionConverter.isObjective(pd.getType())) {
        subjectiveSkipped++;
        continue;
      }
      bankIds.add(pd.getExamId());
    }
    int paperObjectiveCount = bankIds.size();
    int excludedByOtherMock = 0;
    if (excludeOtherMock) {
      Set<Integer> used = new HashSet<>(listMockUsedExamDetailIds(classId, null));
      LinkedHashSet<Integer> filtered = new LinkedHashSet<>();
      for (Integer id : bankIds) {
        if (used.contains(id)) {
          excludedByOtherMock++;
        } else {
          filtered.add(id);
        }
      }
      bankIds = filtered;
    }
    if (bankIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          excludeOtherMock && excludedByOtherMock > 0
              ? "排除本班其他模拟卷已选题后无可挂客观题，请取消排除或换计划"
              : "正式卷中无可挂接的客观题（单选/多选/判断）");
    }

    Long tenantId = resolveTenantId(classId);
    Long userId = currentAdminUserId();
    TrainInfoClassEntity clazz = requireClass(classId);

    FollowTaskSaveReq saveReq = new FollowTaskSaveReq();
    saveReq.setClassId(classId);
    saveReq.setTaskType(TYPE_MOCK);
    saveReq.setTitle(StringUtils.hasText(title) ? title : plan.getTitle());
    saveReq.setDescription(description);
    saveReq.setBizDate(bizDate);
    saveReq.setPassScore(passScore);
    saveReq.setShowAnalysisAfterSubmit(
        showAnalysisAfterSubmit != null ? showAnalysisAfterSubmit : false);
    saveReq.setSourceExamDetailIds(new ArrayList<>(bankIds));

    TrainFollowTaskEntity task = new TrainFollowTaskEntity();
    fillTaskFromReq(task, saveReq, clazz, TYPE_MOCK, tenantId, userId);
    task.setStatus(STATUS_DRAFT);
    task.setMakeExamPlanId(makeExamPlanId);
    task.setSourcePaperId(paper.getId());
    taskMapper.insert(task);
    replaceQuestions(task, resolveQuestions(saveReq, classId, tenantId), userId);

    FollowTaskVO taskVo = toTaskVO(task, true, false);
    Map<String, Object> importStats = new LinkedHashMap<>();
    importStats.put("paperQuestionCount", paperDetails.size());
    importStats.put("paperObjectiveCount", paperObjectiveCount);
    importStats.put("importedCount", bankIds.size());
    importStats.put("subjectiveSkipped", subjectiveSkipped);
    importStats.put("excludedByOtherMock", excludedByOtherMock);
    importStats.put("excludeOtherMock", excludeOtherMock);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("task", taskVo);
    out.put("makeExamPlanId", makeExamPlanId);
    out.put("paperId", paper.getId());
    out.put("importStats", importStats);
    return out;
  }

  private void assertCourseAllowedForClass(Long classId, Integer courseId) {
    if (courseId == null || courseId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请指定科目");
    }
    Long tenantId = resolveTenantId(classId);
    ScopeFilter scope = loadScopeFilter(classId, tenantId);
    List<Integer> allowed = scope.courseIds.isEmpty() ? scope.fallbackCourseIds : scope.courseIds;
    if (!allowed.isEmpty() && !allowed.contains(courseId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "科目不在本班练习/推荐范围内");
    }
  }

  @Transactional
  public FollowTaskVO updateDraftAdmin(Long taskId, FollowTaskSaveReq req) {
    TrainFollowTaskEntity task = requireTask(taskId);
    if (!STATUS_DRAFT.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可修改");
    }
    String taskType = normalizeTaskType(req.getTaskType());
    validateMount(req, taskType);
    Long userId = currentAdminUserId();
    TrainInfoClassEntity clazz = requireClass(task.getClassId());
    fillTaskFromReq(task, req, clazz, taskType, task.getTenantId(), userId);
    taskMapper.updateById(task);
    replaceQuestions(task, resolveQuestions(req, task.getClassId(), task.getTenantId()), userId);
    return toTaskVO(task, true, false);
  }

  @Transactional
  public FollowTaskVO publishAdmin(Long taskId) {
    return publishInternal(requireTask(taskId), currentAdminUserId());
  }

  @Transactional
  public FollowTaskVO closeAdmin(Long taskId) {
    return closeInternal(requireTask(taskId), currentAdminUserId());
  }

  @Transactional
  public FollowTaskVO reopenAdmin(Long taskId) {
    return reopenInternal(requireTask(taskId), currentAdminUserId());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listMockUsedQuestionIds(Long classId, Long excludeTaskId) {
    requireClass(classId);
    List<Integer> usedIds = listMockUsedExamDetailIds(classId, excludeTaskId);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("excludeTaskId", excludeTaskId);
    out.put("usedIds", usedIds);
    out.put("count", usedIds.size());
    return out;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> bankScopeMetaAdmin(Long classId) {
    requireClass(classId);
    return bankScopeMetaInternal(classId);
  }

  /**
   * 管理端：选定科目后查询可抽客观题题型题数（可选排除本班其他模拟卷已选题）。
   */
  @Transactional(readOnly = true)
  public Map<String, Object> bankTypeCountsAdmin(
      Long classId, Integer courseId, boolean excludeOtherMock, Long excludeTaskId) {
    requireClass(classId);
    if (courseId == null || courseId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请指定科目");
    }
    Long tenantId = resolveTenantId(classId);
    ScopeFilter scope = loadScopeFilter(classId, tenantId);
    List<Integer> scopedCourses = !scope.courseIds.isEmpty()
        ? scope.courseIds
        : scope.fallbackCourseIds;
    List<Integer> filterCourses = resolveFilterCourses(courseId, scope, scopedCourses);
    BankScopeQuery bankScope = resolveBankScopeQuery(filterCourses, scope.knowledgeIds, null);
    List<Integer> exclude = excludeOtherMock
        ? listMockUsedExamDetailIds(classId, excludeTaskId)
        : List.of();
    Map<String, Object> typeCounts = aggregateTypeCounts(tenantId, bankScope, exclude);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("courseId", courseId);
    out.put("excludeOtherMock", excludeOtherMock);
    out.put("excludedCount", exclude.size());
    out.put("typeCounts", typeCounts);
    out.put("hint", excludeOtherMock && !exclude.isEmpty()
        ? "已排除本班其他模拟卷占用题 " + exclude.size() + " 道后的可抽题量"
        : "当前科目在本班练习范围内的可抽客观题量");
    return out;
  }

  @Transactional(readOnly = true)
  public FollowTaskVO getTaskAdmin(Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    return toTaskVO(task, true, true);
  }

  @Transactional(readOnly = true)
  public List<FollowTaskItemVO> listItemsAdmin(Long taskId) {
    requireTask(taskId);
    return listItemsInternal(taskId);
  }

  @Transactional(readOnly = true)
  public FollowTaskResultVO getItemAnswersAdmin(Long taskId, Long itemId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    TrainFollowTaskItemEntity item = itemMapper.selectById(itemId);
    if (item == null || Objects.equals(item.getDeleted(), 1)
        || !Objects.equals(item.getTaskId(), taskId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "提交记录不存在");
    }
    return buildResult(task, item, true);
  }

  @Transactional(readOnly = true)
  public FollowTaskResultVO getItemAnswersForTeacher(Long platformUserId, Long taskId, Long itemId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    assertTeacher(platformUserId, task.getClassId());
    return getItemAnswersAdmin(taskId, itemId);
  }

  @Transactional(readOnly = true)
  public FollowTaskStatsVO getTaskStatsAdmin(Long taskId) {
    requireTask(taskId);
    return buildTaskStats(taskId);
  }

  @Transactional(readOnly = true)
  public FollowTaskStatsVO getTaskStatsForTeacher(Long platformUserId, Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    assertTeacher(platformUserId, task.getClassId());
    return buildTaskStats(taskId);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> exportTaskAnswersExcel(Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    List<FollowTaskItemVO> items = listItemsInternal(taskId);
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet summary = wb.createSheet("学员汇总");
      Row h0 = summary.createRow(0);
      String[] headers0 = {
          "学员", "状态", "得分", "达标", "正确题数", "总题数", "正确率%", "答题时长(秒)", "提交时间"
      };
      for (int c = 0; c < headers0.length; c++) {
        h0.createCell(c).setCellValue(headers0[c]);
      }
      int r = 1;
      for (FollowTaskItemVO it : items) {
        Row row = summary.createRow(r++);
        row.createCell(0).setCellValue(it.userName() == null ? "" : it.userName());
        row.createCell(1).setCellValue(it.statusName() == null ? it.status() : it.statusName());
        if (it.score() != null) {
          row.createCell(2).setCellValue(it.score().doubleValue());
        } else {
          row.createCell(2).setCellValue("");
        }
        row.createCell(3).setCellValue(it.passed() == null ? "" : (it.passed() ? "是" : "否"));
        row.createCell(4).setCellValue(it.correctCount() == null ? 0 : it.correctCount());
        row.createCell(5).setCellValue(it.questionCount() == null ? 0 : it.questionCount());
        row.createCell(6).setCellValue(
            it.accuracyRate() == null ? "" : it.accuracyRate().toPlainString());
        row.createCell(7).setCellValue(
            it.durationSeconds() == null ? "" : String.valueOf(it.durationSeconds()));
        row.createCell(8).setCellValue(
            it.submittedAt() == null ? "" : it.submittedAt().toString().replace('T', ' '));
      }

      Sheet detail = wb.createSheet("逐题明细");
      Row h1 = detail.createRow(0);
      String[] headers1 = {
          "学员", "题号", "题型", "题干", "学员作答", "标准答案", "是否正确", "得分", "满分"
      };
      for (int c = 0; c < headers1.length; c++) {
        h1.createCell(c).setCellValue(headers1[c]);
      }
      int dr = 1;
      for (FollowTaskItemVO it : items) {
        if (ITEM_PENDING.equals(it.status())) {
          continue;
        }
        FollowTaskResultVO result = getItemAnswersAdmin(taskId, Long.parseLong(it.id()));
        for (FollowTaskAnswerResultVO a : result.answers()) {
          Row row = detail.createRow(dr++);
          row.createCell(0).setCellValue(it.userName() == null ? "" : it.userName());
          row.createCell(1).setCellValue(a.sortNumber() == null ? 0 : a.sortNumber());
          row.createCell(2).setCellValue(a.questionType() == null ? "" : a.questionType());
          row.createCell(3).setCellValue(plainText(a.stem()));
          row.createCell(4).setCellValue(a.userAnswerJson() == null ? "" : a.userAnswerJson());
          row.createCell(5).setCellValue(a.correctAnswerJson() == null ? "" : a.correctAnswerJson());
          row.createCell(6).setCellValue(Boolean.TRUE.equals(a.correct()) ? "正确" : "错误");
          row.createCell(7).setCellValue(a.score() == null ? 0 : a.score().doubleValue());
          row.createCell(8).setCellValue(a.fullScore() == null ? 0 : a.fullScore().doubleValue());
        }
      }

      Sheet qStat = wb.createSheet("单题正确率");
      FollowTaskStatsVO stats = buildTaskStats(taskId);
      Row hq = qStat.createRow(0);
      String[] headersQ = {"题号", "题型", "题干", "作答人数", "正确人数", "正确率%"};
      for (int c = 0; c < headersQ.length; c++) {
        hq.createCell(c).setCellValue(headersQ[c]);
      }
      int qr = 1;
      for (FollowTaskQuestionStatVO qs : stats.questionStats()) {
        Row row = qStat.createRow(qr++);
        row.createCell(0).setCellValue(qs.sortNumber() == null ? 0 : qs.sortNumber());
        row.createCell(1).setCellValue(qs.questionType() == null ? "" : qs.questionType());
        row.createCell(2).setCellValue(qs.stemPreview() == null ? "" : qs.stemPreview());
        row.createCell(3).setCellValue(qs.answeredCount() == null ? 0 : qs.answeredCount());
        row.createCell(4).setCellValue(qs.correctCount() == null ? 0 : qs.correctCount());
        row.createCell(5).setCellValue(
            qs.accuracyRate() == null ? "" : qs.accuracyRate().toPlainString());
      }

      wb.write(bos);
      byte[] bytes = bos.toByteArray();
      String safeTitle = (task.getTitle() == null ? "作业" : task.getTitle())
          .replaceAll("[\\\\/:*?\"<>|]", "_");
      String filename = safeTitle + "_答卷明细.xlsx";
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8)
                  .replace("+", "%20"))
          .contentType(MediaType.parseMediaType(
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(bytes);
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出答卷失败：" + ex.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public Map<String, Object> homeworkMetrics(Long classId) {
    return channelMetrics(classId, List.of(TYPE_HOMEWORK, TYPE_PRACTICE), "作业");
  }

  @Transactional(readOnly = true)
  public Map<String, Object> mockMetrics(Long classId) {
    return channelMetrics(classId, List.of(TYPE_MOCK), "模拟考");
  }

  /**
   * 班级跟班通道汇总（作业/模拟考共用）：已发布任务数、交卷、正确率、预警。
   */
  private Map<String, Object> channelMetrics(Long classId, List<String> taskTypes, String label) {
    requireClass(classId);
    List<TrainFollowTaskEntity> tasks = taskMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskEntity>()
        .eq(TrainFollowTaskEntity::getClassId, classId)
        .in(TrainFollowTaskEntity::getStatus, STATUS_PUBLISHED, STATUS_CLOSED)
        .in(TrainFollowTaskEntity::getTaskType, taskTypes)
        .eq(TrainFollowTaskEntity::getDeleted, 0)
        .orderByDesc(TrainFollowTaskEntity::getPublishedAt));
    int taskCount = tasks.size();
    int totalItems = 0;
    int passedItems = 0;
    int gradedItems = 0;
    BigDecimal accuracySum = BigDecimal.ZERO;
    int accuracyN = 0;
    Map<Long, Integer> consecutiveMiss = new HashMap<>();
    Map<Long, String> peopleNames = new HashMap<>();
    Map<Long, List<BigDecimal>> peopleAccuracy = new HashMap<>();

    for (TrainFollowTaskEntity t : tasks) {
      int qCount = listQuestions(t.getId()).size();
      List<TrainFollowTaskItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskItemEntity>()
          .eq(TrainFollowTaskItemEntity::getTaskId, t.getId())
          .eq(TrainFollowTaskItemEntity::getDeleted, 0));
      totalItems += items.size();
      Map<Long, AccuPair> accu = loadItemAccuracy(items, qCount);
      for (TrainFollowTaskItemEntity it : items) {
        if (it.getPeopleId() != null && StringUtils.hasText(it.getUserName())) {
          peopleNames.put(it.getPeopleId(), it.getUserName());
        }
        boolean done = ITEM_GRADED.equals(it.getStatus()) || ITEM_SUBMITTED.equals(it.getStatus());
        boolean empty = done && !hasAnyAnswerContent(it.getId());
        if (done && !empty) {
          gradedItems++;
          if (it.getPassed() != null && it.getPassed() == 1) {
            passedItems++;
          } else if (it.getPassed() == null) {
            passedItems++;
          }
          AccuPair ap = accu.get(it.getId());
          if (ap != null && ap.accuracyRate != null) {
            accuracySum = accuracySum.add(ap.accuracyRate);
            accuracyN++;
            peopleAccuracy.computeIfAbsent(it.getPeopleId(), k -> new ArrayList<>()).add(ap.accuracyRate);
          }
          consecutiveMiss.putIfAbsent(it.getPeopleId(), 0);
        } else {
          Integer cur = consecutiveMiss.get(it.getPeopleId());
          if (cur == null) {
            consecutiveMiss.put(it.getPeopleId(), 1);
          } else if (cur > 0) {
            consecutiveMiss.put(it.getPeopleId(), cur + 1);
          }
        }
      }
    }

    BigDecimal rate = totalItems == 0
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(gradedItems * 100.0 / totalItems).setScale(1, RoundingMode.HALF_UP);
    BigDecimal avgAccuracy = accuracyN == 0
        ? null
        : accuracySum.divide(BigDecimal.valueOf(accuracyN), 1, RoundingMode.HALF_UP);

    List<Map<String, Object>> warnings = new ArrayList<>();
    for (Map.Entry<Long, Integer> e : consecutiveMiss.entrySet()) {
      if (e.getValue() != null && e.getValue() >= 2) {
        Map<String, Object> w = new LinkedHashMap<>();
        w.put("peopleId", e.getKey());
        w.put("userName", peopleNames.getOrDefault(e.getKey(), String.valueOf(e.getKey())));
        w.put("type", "miss");
        w.put("message", "连续未交 " + e.getValue() + " 次" + label);
        w.put("consecutiveMiss", e.getValue());
        warnings.add(w);
      }
    }
    for (Map.Entry<Long, List<BigDecimal>> e : peopleAccuracy.entrySet()) {
      List<BigDecimal> list = e.getValue();
      if (list.isEmpty()) {
        continue;
      }
      BigDecimal sum = list.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal avg = sum.divide(BigDecimal.valueOf(list.size()), 1, RoundingMode.HALF_UP);
      if (avg.compareTo(BigDecimal.valueOf(60)) < 0) {
        Map<String, Object> w = new LinkedHashMap<>();
        w.put("peopleId", e.getKey());
        w.put("userName", peopleNames.getOrDefault(e.getKey(), String.valueOf(e.getKey())));
        w.put("type", "low_accuracy");
        w.put("message", label + "平均正确率 " + avg + "%");
        w.put("avgAccuracyRate", avg);
        warnings.add(w);
      }
    }
    warnings.sort((a, b) -> {
      int missA = a.get("consecutiveMiss") instanceof Number n ? n.intValue() : 0;
      int missB = b.get("consecutiveMiss") instanceof Number n ? n.intValue() : 0;
      if (missA != missB) {
        return Integer.compare(missB, missA);
      }
      return String.valueOf(a.get("userName")).compareTo(String.valueOf(b.get("userName")));
    });
    if (warnings.size() > 30) {
      warnings = warnings.subList(0, 30);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("taskCount", taskCount);
    out.put("totalItems", totalItems);
    out.put("gradedItems", gradedItems);
    out.put("passedItems", passedItems);
    out.put("completionRate", rate);
    out.put("avgAccuracyRate", avgAccuracy);
    out.put("warnings", warnings);
    return out;
  }

  @Transactional
  public Map<String, Object> syncRoster(Long taskId) {
    TrainFollowTaskEntity task = requireTask(taskId);
    if (!STATUS_PUBLISHED.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已发布任务可补发");
    }
    int added = generateItems(task, SecurityContext.currentUser().getUserId());
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("taskId", String.valueOf(taskId));
    out.put("added", added);
    return out;
  }

  /** 插班：新学员补发本班已发布未关闭任务 */
  @Transactional
  public int issuePendingItemsForNewStudent(Long classId, Long peopleId) {
    if (classId == null || peopleId == null) {
      return 0;
    }
    TrainPeopleListEntity people = peopleMapper.selectById(peopleId);
    if (people == null || !"student".equals(people.getType())) {
      return 0;
    }
    List<TrainFollowTaskEntity> tasks = taskMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskEntity>()
        .eq(TrainFollowTaskEntity::getClassId, classId)
        .eq(TrainFollowTaskEntity::getStatus, STATUS_PUBLISHED)
        .eq(TrainFollowTaskEntity::getDeleted, 0));
    int added = 0;
    for (TrainFollowTaskEntity t : tasks) {
      if (findItem(t.getId(), peopleId) != null) {
        continue;
      }
      insertItem(t, people, people.getId());
      added++;
    }
    return added;
  }

  // ---------- 内部 ----------

  private void assertTeacher(Long platformUserId, Long classId) {
    roleService.requireAnyActiveRole(
        platformUserId,
        classId,
        TrainClassRoleApplicationService.LEAD_TEACHER,
        TrainClassRoleApplicationService.ASSISTANT);
  }

  private FollowTaskVO publishInternal(TrainFollowTaskEntity task, Long userId) {
    if (!STATUS_DRAFT.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可发布");
    }
    List<TrainFollowTaskQuestionEntity> qs = listQuestions(task.getId());
    if (qs.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先添加题目");
    }
    task.setStatus(STATUS_PUBLISHED);
    task.setPublishedAt(LocalDateTime.now());
    task.setUpdatedBy(userId);
    taskMapper.updateById(task);
    generateItems(task, userId);
    return toTaskVO(task, false, true);
  }

  private FollowTaskVO closeInternal(TrainFollowTaskEntity task, Long userId) {
    if (!STATUS_PUBLISHED.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已发布任务可关闭");
    }
    task.setStatus(STATUS_CLOSED);
    task.setUpdatedBy(userId);
    taskMapper.updateById(task);
    return toTaskVO(task, false, true);
  }

  /** 已关闭 → 已发布；保留原名单与答卷，未交学员可继续交。 */
  private FollowTaskVO reopenInternal(TrainFollowTaskEntity task, Long userId) {
    if (!STATUS_CLOSED.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已关闭任务可重新打开");
    }
    List<TrainFollowTaskQuestionEntity> qs = listQuestions(task.getId());
    if (qs.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "题目为空，无法重新打开");
    }
    task.setStatus(STATUS_PUBLISHED);
    if (task.getPublishedAt() == null) {
      task.setPublishedAt(LocalDateTime.now());
    }
    task.setUpdatedBy(userId);
    taskMapper.updateById(task);
    return toTaskVO(task, false, true);
  }

  private List<Integer> listMockUsedExamDetailIds(Long classId, Long excludeTaskId) {
    List<TrainFollowTaskEntity> mocks = taskMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskEntity>()
        .eq(TrainFollowTaskEntity::getClassId, classId)
        .eq(TrainFollowTaskEntity::getTaskType, TYPE_MOCK)
        .in(TrainFollowTaskEntity::getStatus, STATUS_DRAFT, STATUS_PUBLISHED)
        .eq(TrainFollowTaskEntity::getDeleted, 0)
        .select(TrainFollowTaskEntity::getId));
    if (mocks.isEmpty()) {
      return List.of();
    }
    List<Long> taskIds = mocks.stream()
        .map(TrainFollowTaskEntity::getId)
        .filter(id -> excludeTaskId == null || !Objects.equals(id, excludeTaskId))
        .toList();
    if (taskIds.isEmpty()) {
      return List.of();
    }
    List<TrainFollowTaskQuestionEntity> qs = questionMapper.selectList(
        new LambdaQueryWrapper<TrainFollowTaskQuestionEntity>()
            .in(TrainFollowTaskQuestionEntity::getTaskId, taskIds)
            .eq(TrainFollowTaskQuestionEntity::getDeleted, 0)
            .isNotNull(TrainFollowTaskQuestionEntity::getSourceExamDetailId)
            .select(TrainFollowTaskQuestionEntity::getSourceExamDetailId));
    LinkedHashSet<Integer> ids = new LinkedHashSet<>();
    for (TrainFollowTaskQuestionEntity q : qs) {
      if (q.getSourceExamDetailId() != null && q.getSourceExamDetailId() > 0) {
        ids.add(q.getSourceExamDetailId().intValue());
      }
    }
    return new ArrayList<>(ids);
  }

  private void applyTaskTypeFilter(LambdaQueryWrapper<TrainFollowTaskEntity> q, String filter) {
    if (!StringUtils.hasText(filter) || "all".equalsIgnoreCase(filter.trim())) {
      return;
    }
    String f = filter.trim();
    if (TYPE_MOCK.equals(f)) {
      q.eq(TrainFollowTaskEntity::getTaskType, TYPE_MOCK);
    } else if (FILTER_ASSIGNMENT.equals(f) || TYPE_HOMEWORK.equals(f) || TYPE_PRACTICE.equals(f)) {
      q.in(TrainFollowTaskEntity::getTaskType, TYPE_HOMEWORK, TYPE_PRACTICE);
    } else {
      q.eq(TrainFollowTaskEntity::getTaskType, f);
    }
  }

  private Long currentAdminUserId() {
    try {
      Long id = SecurityContext.currentUser().getUserId();
      if (id != null && id > 0) {
        return id;
      }
    } catch (Exception ignored) {
    }
    return 0L;
  }

  private List<FollowTaskVO> listByClassInternal(Long classId, boolean withStats, String taskTypeFilter) {
    LambdaQueryWrapper<TrainFollowTaskEntity> q = new LambdaQueryWrapper<TrainFollowTaskEntity>()
        .eq(TrainFollowTaskEntity::getClassId, classId)
        .eq(TrainFollowTaskEntity::getDeleted, 0)
        .orderByDesc(TrainFollowTaskEntity::getCreatedAt);
    applyTaskTypeFilter(q, taskTypeFilter);
    List<TrainFollowTaskEntity> tasks = taskMapper.selectList(q);
    List<FollowTaskVO> out = new ArrayList<>();
    for (TrainFollowTaskEntity t : tasks) {
      out.add(toTaskVO(t, false, withStats));
    }
    return out;
  }

  private List<FollowTaskItemVO> listItemsInternal(Long taskId) {
    List<TrainFollowTaskQuestionEntity> questions = listQuestions(taskId);
    int qCount = questions.size();
    List<TrainFollowTaskItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskItemEntity>()
        .eq(TrainFollowTaskItemEntity::getTaskId, taskId)
        .eq(TrainFollowTaskItemEntity::getDeleted, 0)
        .orderByAsc(TrainFollowTaskItemEntity::getUserName));
    Map<Long, AccuPair> accu = loadItemAccuracy(items, qCount);
    List<FollowTaskItemVO> out = new ArrayList<>();
    for (TrainFollowTaskItemEntity i : items) {
      AccuPair ap = accu.getOrDefault(i.getId(), AccuPair.empty(qCount));
      out.add(toItemVO(i, ap.correctCount, ap.questionCount, ap.accuracyRate));
    }
    return out;
  }

  private FollowTaskStatsVO buildTaskStats(Long taskId) {
    List<TrainFollowTaskQuestionEntity> questions = listQuestions(taskId);
    List<TrainFollowTaskItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskItemEntity>()
        .eq(TrainFollowTaskItemEntity::getTaskId, taskId)
        .eq(TrainFollowTaskItemEntity::getDeleted, 0));
    int totalCount = items.size();
    int submittedCount = 0;
    BigDecimal scoreSum = BigDecimal.ZERO;
    int scoreN = 0;
    int passedN = 0;
    BigDecimal accuracySum = BigDecimal.ZERO;
    int accuracyN = 0;
    long durationSum = 0;
    int durationN = 0;
    Map<Long, AccuPair> accu = loadItemAccuracy(items, questions.size());

    Map<Long, int[]> qCounters = new LinkedHashMap<>();
    for (TrainFollowTaskQuestionEntity q : questions) {
      qCounters.put(q.getId(), new int[]{0, 0}); // answered, correct
    }

    List<Long> submittedItemIds = new ArrayList<>();
    for (TrainFollowTaskItemEntity it : items) {
      boolean done = ITEM_GRADED.equals(it.getStatus()) || ITEM_SUBMITTED.equals(it.getStatus());
      if (!done) {
        continue;
      }
      submittedCount++;
      submittedItemIds.add(it.getId());
      if (it.getScore() != null) {
        scoreSum = scoreSum.add(it.getScore());
        scoreN++;
      }
      if (it.getPassed() != null && it.getPassed() == 1) {
        passedN++;
      } else if (it.getPassed() == null) {
        passedN++;
      }
      AccuPair ap = accu.get(it.getId());
      if (ap != null && ap.accuracyRate != null) {
        accuracySum = accuracySum.add(ap.accuracyRate);
        accuracyN++;
      }
      if (it.getDurationSeconds() != null && it.getDurationSeconds() >= 0) {
        durationSum += it.getDurationSeconds();
        durationN++;
      }
    }

    if (!submittedItemIds.isEmpty()) {
      List<TrainFollowTaskAnswerEntity> answers = answerMapper.selectList(
          new LambdaQueryWrapper<TrainFollowTaskAnswerEntity>()
              .in(TrainFollowTaskAnswerEntity::getItemId, submittedItemIds)
              .eq(TrainFollowTaskAnswerEntity::getDeleted, 0));
      for (TrainFollowTaskAnswerEntity a : answers) {
        int[] c = qCounters.get(a.getQuestionId());
        if (c == null) {
          continue;
        }
        if (!hasAnswerContent(a.getAnswerJson())) {
          continue;
        }
        c[0]++;
        if (a.getIsCorrect() != null && a.getIsCorrect() == 1) {
          c[1]++;
        }
      }
    }

    List<FollowTaskQuestionStatVO> qStats = new ArrayList<>();
    for (TrainFollowTaskQuestionEntity q : questions) {
      int[] c = qCounters.getOrDefault(q.getId(), new int[]{0, 0});
      BigDecimal qRate = c[0] == 0
          ? null
          : BigDecimal.valueOf(c[1] * 100.0 / c[0]).setScale(1, RoundingMode.HALF_UP);
      qStats.add(new FollowTaskQuestionStatVO(
          String.valueOf(q.getId()),
          q.getSortNumber(),
          q.getQuestionType(),
          stemPreview(q.getStem()),
          c[0],
          c[1],
          qRate));
    }

    return new FollowTaskStatsVO(
        String.valueOf(taskId),
        questions.size(),
        totalCount,
        submittedCount,
        scoreN == 0 ? null : scoreSum.divide(BigDecimal.valueOf(scoreN), 1, RoundingMode.HALF_UP),
        submittedCount == 0
            ? null
            : BigDecimal.valueOf(passedN * 100.0 / submittedCount).setScale(1, RoundingMode.HALF_UP),
        accuracyN == 0
            ? null
            : accuracySum.divide(BigDecimal.valueOf(accuracyN), 1, RoundingMode.HALF_UP),
        durationN == 0 ? null : (int) (durationSum / durationN),
        qStats);
  }

  private Map<Long, AccuPair> loadItemAccuracy(List<TrainFollowTaskItemEntity> items, int qCount) {
    Map<Long, AccuPair> out = new HashMap<>();
    if (items == null || items.isEmpty()) {
      return out;
    }
    List<Long> ids = items.stream().map(TrainFollowTaskItemEntity::getId).toList();
    List<TrainFollowTaskAnswerEntity> answers = answerMapper.selectList(
        new LambdaQueryWrapper<TrainFollowTaskAnswerEntity>()
            .in(TrainFollowTaskAnswerEntity::getItemId, ids)
            .eq(TrainFollowTaskAnswerEntity::getDeleted, 0));
    Map<Long, List<TrainFollowTaskAnswerEntity>> byItem = answers.stream()
        .collect(Collectors.groupingBy(TrainFollowTaskAnswerEntity::getItemId));
    for (TrainFollowTaskItemEntity it : items) {
      List<TrainFollowTaskAnswerEntity> rows = byItem.getOrDefault(it.getId(), List.of());
      int correct = 0;
      for (TrainFollowTaskAnswerEntity a : rows) {
        if (a.getIsCorrect() != null && a.getIsCorrect() == 1 && hasAnswerContent(a.getAnswerJson())) {
          correct++;
        }
      }
      BigDecimal rate = qCount <= 0
          ? null
          : BigDecimal.valueOf(correct * 100.0 / qCount).setScale(1, RoundingMode.HALF_UP);
      // 未交卷不展示正确率
      if (ITEM_PENDING.equals(it.getStatus())) {
        out.put(it.getId(), new AccuPair(null, qCount, null));
      } else {
        out.put(it.getId(), new AccuPair(correct, qCount, rate));
      }
    }
    return out;
  }

  private void markStartedIfNeeded(TrainFollowTaskItemEntity item, Long operatorId) {
    if (item.getStartedAt() != null) {
      return;
    }
    // 已交且有内容的不再改开答时间
    if ((ITEM_GRADED.equals(item.getStatus()) || ITEM_SUBMITTED.equals(item.getStatus()))
        && hasAnyAnswerContent(item.getId())) {
      return;
    }
    item.setStartedAt(LocalDateTime.now());
    item.setUpdatedBy(operatorId);
    itemMapper.updateById(item);
  }

  private static boolean hasAnswerContent(String answerJson) {
    return StringUtils.hasText(answerJson) && !"null".equalsIgnoreCase(answerJson.trim());
  }

  private static String plainText(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    return raw.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
  }

  private static String stemPreview(String stem) {
    String t = plainText(stem);
    return t.length() > 48 ? t.substring(0, 48) + "…" : t;
  }

  private record AccuPair(Integer correctCount, Integer questionCount, BigDecimal accuracyRate) {
    static AccuPair empty(int qCount) {
      return new AccuPair(null, qCount, null);
    }
  }

  private void fillTaskFromReq(
      TrainFollowTaskEntity task,
      FollowTaskSaveReq req,
      TrainInfoClassEntity clazz,
      String taskType,
      Long tenantId,
      Long userId) {
    task.setClassId(clazz.getId());
    task.setTrainInfoId(clazz.getTrainInfoId());
    task.setScheduleId(req.getScheduleId());
    task.setBizDate(trimToNull(req.getBizDate()));
    if (req.getScheduleId() != null && req.getScheduleId() > 0) {
      TrainScheduleEntity sch = scheduleMapper.selectById(req.getScheduleId());
      if (sch != null && StringUtils.hasText(sch.getScheduleDate()) && !StringUtils.hasText(task.getBizDate())) {
        task.setBizDate(sch.getScheduleDate());
      }
    }
    task.setTaskType(taskType);
    task.setTitle(req.getTitle().trim());
    task.setDescription(trimToNull(req.getDescription()));
    task.setDeadlineAt(parseDateTime(req.getDeadlineAt()));
    task.setPassScore(req.getPassScore());
    task.setPassRule(StringUtils.hasText(req.getPassRule()) ? req.getPassRule().trim() : "submitted");
    boolean show = req.getShowAnalysisAfterSubmit() != null
        ? req.getShowAnalysisAfterSubmit()
        : TYPE_PRACTICE.equals(taskType);
    task.setShowAnalysisAfterSubmit(show ? 1 : 0);
    task.setTenantId(tenantId);
    task.setCreatedBy(userId);
    task.setUpdatedBy(userId);
    task.setDeleted(0);
  }

  private void replaceQuestions(TrainFollowTaskEntity task, List<FollowTaskQuestionReq> reqs, Long userId) {
    if (reqs == null || reqs.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少添加一道题目（自拟或从题库选择）");
    }
    List<TrainFollowTaskQuestionEntity> old = listQuestions(task.getId());
    for (TrainFollowTaskQuestionEntity q : old) {
      questionMapper.deleteById(q.getId());
    }
    int i = 1;
    for (FollowTaskQuestionReq req : reqs) {
      String qType = normalizeQType(req.getQuestionType());
      if (("single".equals(qType) || "multi".equals(qType) || "judge".equals(qType))
          && req.getAnswer() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写标准答案");
      }
      if (("single".equals(qType) || "multi".equals(qType))
          && (req.getOptions() == null || req.getOptions().isEmpty())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "单选/多选需提供选项");
      }
      TrainFollowTaskQuestionEntity q = new TrainFollowTaskQuestionEntity();
      q.setTaskId(task.getId());
      q.setSortNumber(req.getSortNumber() != null ? req.getSortNumber() : i);
      q.setQuestionType(qType);
      q.setStem(req.getStem().trim());
      q.setOptionsJson(toJson(req.getOptions()));
      q.setAnswerJson(toJson(req.getAnswer()));
      q.setScore(req.getScore() != null ? req.getScore() : BigDecimal.ONE);
      q.setAnalysis(trimToNull(req.getAnalysis()));
      q.setSourceExamDetailId(req.getSourceExamDetailId());
      q.setTenantId(task.getTenantId());
      q.setCreatedBy(userId);
      q.setUpdatedBy(userId);
      q.setDeleted(0);
      questionMapper.insert(q);
      i++;
    }
  }

  private List<FollowTaskQuestionReq> resolveQuestions(
      FollowTaskSaveReq req, Long classId, Long tenantId) {
    List<FollowTaskQuestionReq> merged = new ArrayList<>();
    if (req.getQuestions() != null) {
      for (FollowTaskQuestionReq q : req.getQuestions()) {
        if (q != null && StringUtils.hasText(q.getStem())) {
          merged.add(q);
        }
      }
    }
    List<Integer> bankIds = req.getSourceExamDetailIds();
    if (bankIds != null && !bankIds.isEmpty()) {
      ScopeFilter scope = loadScopeFilter(classId, tenantId);
      LinkedHashSet<Integer> unique = new LinkedHashSet<>();
      for (Integer id : bankIds) {
        if (id != null && id > 0) {
          unique.add(id);
        }
      }
      if (unique.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "题库题目 ID 无效");
      }
      List<ExamExamDetailEntity> details = examDetailMapper.selectBatchIds(unique);
      Map<Integer, ExamExamDetailEntity> map = details.stream()
          .collect(Collectors.toMap(ExamExamDetailEntity::getId, d -> d, (a, b) -> a));
      int sort = merged.size() + 1;
      for (Integer id : unique) {
        ExamExamDetailEntity d = map.get(id);
        if (d == null || Objects.equals(d.getDeleted(), 1)) {
          throw new BusinessException(ErrorCode.NOT_FOUND, "题库题不存在: " + id);
        }
        if (tenantId != null && tenantId > 0 && d.getTenantId() != null
            && !Objects.equals(d.getTenantId(), tenantId.intValue())) {
          throw new BusinessException(ErrorCode.FORBIDDEN, "无权使用题库题: " + id);
        }
        if (!FollowTaskBankQuestionConverter.isObjective(d.getType())) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持客观题，题号: " + id);
        }
        assertDetailInScope(d, scope);
        FollowTaskQuestionReq converted = bankConverter.convert(d, sort++);
        merged.add(converted);
      }
    }
    if (merged.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少添加一道题目（自拟或从题库选择）");
    }
    return merged;
  }

  private void assertDetailInScope(ExamExamDetailEntity d, ScopeFilter scope) {
    if (!scope.knowledgeIds.isEmpty()) {
      if (d.getKnowledgeId() == null || !scope.knowledgeIds.contains(d.getKnowledgeId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "题目不在本班知识点范围内: " + d.getId());
      }
      return;
    }
    if (scope.courseIds.isEmpty()) {
      return;
    }
    if (d.getExamId() != null && d.getExamId() > 0 && scope.courseIds.contains(d.getExamId())) {
      return;
    }
    if (d.getKnowledgeId() != null) {
      ExamKnowledgeStructureEntity node = knowledgeStructureMapper.selectById(d.getKnowledgeId());
      if (node != null && node.getCourseId() != null && scope.courseIds.contains(node.getCourseId())) {
        return;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "题目不在本班科目范围内: " + d.getId());
  }

  /**
   * 解析选题范围：科目 → 知识点树；白名单与科目求交。
   * 不依赖 exam_exam_detail.exam_id（存量常为 0）。
   */
  private BankScopeQuery resolveBankScopeQuery(
      List<Integer> filterCourses,
      List<Integer> scopeKnowledgeIds,
      Integer knowledgeId) {
    if (knowledgeId != null && knowledgeId > 0) {
      List<Integer> expanded = expandKnowledgeFilter(knowledgeId, filterCourses, scopeKnowledgeIds);
      if (expanded.isEmpty()) {
        return BankScopeQuery.impossible("该知识点分类下暂无可用客观题");
      }
      return BankScopeQuery.byKnowledgeIds(expanded, null);
    }

    List<Integer> courseKnowledges = listKnowledgeIdsByCourses(filterCourses);
    if (!scopeKnowledgeIds.isEmpty()) {
      if (filterCourses.isEmpty() || courseKnowledges.isEmpty()) {
        return BankScopeQuery.byKnowledgeIds(scopeKnowledgeIds, null);
      }
      Set<Integer> courseSet = new HashSet<>(courseKnowledges);
      List<Integer> intersected = scopeKnowledgeIds.stream().filter(courseSet::contains).distinct().toList();
      if (intersected.isEmpty()) {
        return BankScopeQuery.impossible("所选科目与本班知识点白名单无交集，暂无试题");
      }
      return BankScopeQuery.byKnowledgeIds(intersected, null);
    }

    if (!filterCourses.isEmpty()) {
      if (!courseKnowledges.isEmpty()) {
        // 知识点树 + 兼容已正确回填 exam_id 的题目
        return BankScopeQuery.byKnowledgeOrExam(courseKnowledges, filterCourses, null);
      }
      return BankScopeQuery.byExamIds(filterCourses, "该科目知识点树为空，且题库未挂 exam_id，暂无试题");
    }
    return BankScopeQuery.unrestricted();
  }

  /**
   * 选中知识点（可为父节点）：展开为自身+子孙；有白名单则与白名单求交。
   */
  private List<Integer> expandKnowledgeFilter(
      Integer knowledgeId,
      List<Integer> filterCourses,
      List<Integer> scopeKnowledgeIds) {
    ExamKnowledgeStructureEntity node = knowledgeStructureMapper.selectById(knowledgeId);
    if (node == null || (node.getDeleted() != null && node.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "知识点不存在");
    }
    if (!filterCourses.isEmpty()
        && node.getCourseId() != null
        && !filterCourses.contains(node.getCourseId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "知识点不属于所选科目");
    }

    List<Integer> courseIds = filterCourses != null && !filterCourses.isEmpty()
        ? filterCourses
        : (node.getCourseId() != null ? List.of(node.getCourseId()) : List.of());
    List<ExamKnowledgeStructureEntity> all = courseIds.isEmpty()
        ? List.of(node)
        : knowledgeStructureMapper.selectList(
            new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
                .in(ExamKnowledgeStructureEntity::getCourseId, courseIds)
                .eq(ExamKnowledgeStructureEntity::getDeleted, 0));

    Map<Integer, List<Integer>> children = new HashMap<>();
    for (ExamKnowledgeStructureEntity n : all) {
      int pid = n.getParentId() == null ? 0 : n.getParentId();
      children.computeIfAbsent(pid, k -> new ArrayList<>()).add(n.getId());
    }

    LinkedHashSet<Integer> collected = new LinkedHashSet<>();
    java.util.ArrayDeque<Integer> dq = new java.util.ArrayDeque<>();
    dq.add(knowledgeId);
    while (!dq.isEmpty()) {
      Integer id = dq.poll();
      if (id == null || !collected.add(id)) {
        continue;
      }
      List<Integer> kids = children.getOrDefault(id, List.of());
      for (Integer c : kids) {
        dq.add(c);
      }
    }

    if (!scopeKnowledgeIds.isEmpty()) {
      Set<Integer> allow = new HashSet<>(scopeKnowledgeIds);
      List<Integer> hit = collected.stream().filter(allow::contains).toList();
      if (!hit.isEmpty()) {
        return hit;
      }
      if (allow.contains(knowledgeId)) {
        return List.of(knowledgeId);
      }
      throw new BusinessException(ErrorCode.BAD_REQUEST, "知识点不在本班练习范围内");
    }
    return new ArrayList<>(collected);
  }

  private Set<Integer> resolveKnowledgeTreeKeepIds(
      List<ExamKnowledgeStructureEntity> all,
      Set<Integer> whitelist) {
    if (whitelist == null || whitelist.isEmpty()) {
      return all.stream().map(ExamKnowledgeStructureEntity::getId).filter(Objects::nonNull)
          .collect(Collectors.toCollection(HashSet::new));
    }
    Map<Integer, ExamKnowledgeStructureEntity> byId = all.stream()
        .filter(n -> n.getId() != null)
        .collect(Collectors.toMap(ExamKnowledgeStructureEntity::getId, n -> n, (a, b) -> a));
    Set<Integer> keep = new HashSet<>();
    for (Integer wid : whitelist) {
      Integer cur = wid;
      while (cur != null && cur > 0 && keep.add(cur)) {
        ExamKnowledgeStructureEntity n = byId.get(cur);
        if (n == null) {
          break;
        }
        cur = n.getParentId();
      }
    }
    return keep;
  }

  private List<Map<String, Object>> buildBankKnowledgeNodes(
      Map<Integer, List<ExamKnowledgeStructureEntity>> byParent,
      int parentId,
      Set<Integer> keepIds,
      Set<Integer> whitelist) {
    List<ExamKnowledgeStructureEntity> children = byParent.getOrDefault(parentId, List.of());
    List<Map<String, Object>> out = new ArrayList<>();
    for (ExamKnowledgeStructureEntity n : children) {
      if (n.getId() == null || !keepIds.contains(n.getId())) {
        continue;
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", n.getId());
      row.put("title", n.getTitle());
      row.put("courseId", n.getCourseId());
      row.put("parentId", n.getParentId() == null ? 0 : n.getParentId());
      boolean inWhitelist = whitelist == null || whitelist.isEmpty() || whitelist.contains(n.getId());
      row.put("inWhitelist", inWhitelist);
      List<Map<String, Object>> kids = buildBankKnowledgeNodes(byParent, n.getId(), keepIds, whitelist);
      if (!kids.isEmpty()) {
        row.put("children", kids);
      }
      out.add(row);
    }
    return out;
  }

  private void applyBankScopeToWrapper(
      LambdaQueryWrapper<ExamExamDetailEntity> q,
      BankScopeQuery bankScope) {
    if (bankScope.knowledgeIds() != null && !bankScope.knowledgeIds().isEmpty()
        && bankScope.examIds() != null && !bankScope.examIds().isEmpty()) {
      List<Integer> kids = bankScope.knowledgeIds();
      List<Integer> eids = bankScope.examIds();
      q.and(w -> w.in(ExamExamDetailEntity::getKnowledgeId, kids)
          .or()
          .in(ExamExamDetailEntity::getExamId, eids));
      return;
    }
    if (bankScope.knowledgeIds() != null && !bankScope.knowledgeIds().isEmpty()) {
      q.in(ExamExamDetailEntity::getKnowledgeId, bankScope.knowledgeIds());
      return;
    }
    if (bankScope.examIds() != null && !bankScope.examIds().isEmpty()) {
      q.in(ExamExamDetailEntity::getExamId, bankScope.examIds());
    }
  }

  private void applyBankScopeToQueryWrapper(
      QueryWrapper<ExamExamDetailEntity> q,
      BankScopeQuery bankScope) {
    if (bankScope.knowledgeIds() != null && !bankScope.knowledgeIds().isEmpty()
        && bankScope.examIds() != null && !bankScope.examIds().isEmpty()) {
      List<Integer> kids = bankScope.knowledgeIds();
      List<Integer> eids = bankScope.examIds();
      q.and(w -> w.in("knowledge_id", kids).or().in("exam_id", eids));
      return;
    }
    if (bankScope.knowledgeIds() != null && !bankScope.knowledgeIds().isEmpty()) {
      q.in("knowledge_id", bankScope.knowledgeIds());
      return;
    }
    if (bankScope.examIds() != null && !bankScope.examIds().isEmpty()) {
      q.in("exam_id", bankScope.examIds());
    }
  }

  private Map<String, Object> emptyTypeCounts() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("single", 0L);
    m.put("multi", 0L);
    m.put("judge", 0L);
    m.put("total", 0L);
    return m;
  }

  /** 当前范围内各客观题型题量（不受题型 chip / 关键词影响）。 */
  private Map<String, Object> aggregateTypeCounts(Long tenantId, BankScopeQuery bankScope) {
    return aggregateTypeCounts(tenantId, bankScope, null);
  }

  private Map<String, Object> aggregateTypeCounts(
      Long tenantId, BankScopeQuery bankScope, Collection<Integer> excludeIds) {
    Map<String, Object> out = emptyTypeCounts();
    if (bankScope == null || bankScope.impossible()) {
      return out;
    }
    QueryWrapper<ExamExamDetailEntity> qw = baseObjectiveAggWrapper(tenantId);
    qw.select("type", "COUNT(*) AS cnt");
    applyBankScopeToQueryWrapper(qw, bankScope);
    if (excludeIds != null && !excludeIds.isEmpty()) {
      qw.notIn("id", excludeIds);
    }
    qw.groupBy("type");
    long total = 0L;
    for (Map<String, Object> row : examDetailMapper.selectMaps(qw)) {
      String raw = row.get("type") == null ? "" : String.valueOf(row.get("type"));
      String nt = FollowTaskBankQuestionConverter.normalizeType(raw);
      if (!out.containsKey(nt) || "total".equals(nt)) {
        continue;
      }
      long c = toLong(row.get("cnt"));
      out.put(nt, ((Number) out.get(nt)).longValue() + c);
      total += c;
    }
    out.put("total", total);
    return out;
  }

  /** 知识点节点直接挂载的客观题数量（不含子孙，由 fillKnowledgeQuestionCounts 向上汇总）。 */
  private Map<Integer, Integer> aggregateDirectKnowledgeCounts(Long tenantId, BankScopeQuery bankScope) {
    Map<Integer, Integer> out = new HashMap<>();
    if (bankScope == null || bankScope.impossible()) {
      return out;
    }
    QueryWrapper<ExamExamDetailEntity> qw = baseObjectiveAggWrapper(tenantId);
    qw.select("knowledge_id AS knowledgeId", "COUNT(*) AS cnt");
    applyBankScopeToQueryWrapper(qw, bankScope);
    qw.isNotNull("knowledge_id");
    qw.groupBy("knowledge_id");
    for (Map<String, Object> row : examDetailMapper.selectMaps(qw)) {
      Object kid = row.get("knowledgeId");
      if (kid == null) {
        kid = row.get("knowledge_id");
      }
      if (kid == null) {
        continue;
      }
      out.put(((Number) kid).intValue(), (int) toLong(row.get("cnt")));
    }
    return out;
  }

  private QueryWrapper<ExamExamDetailEntity> baseObjectiveAggWrapper(Long tenantId) {
    QueryWrapper<ExamExamDetailEntity> qw = new QueryWrapper<>();
    qw.eq("deleted", 0)
        .eq("status", 1)
        .in("type", List.of("single", "multi", "judge", "danxuan", "duoxuan", "pandan"));
    if (tenantId != null && tenantId > 0) {
      qw.eq("tenant_id", tenantId.intValue());
    }
    return qw;
  }

  @SuppressWarnings("unchecked")
  private int fillKnowledgeQuestionCounts(List<Map<String, Object>> nodes, Map<Integer, Integer> direct) {
    if (nodes == null || nodes.isEmpty()) {
      return 0;
    }
    int sum = 0;
    for (Map<String, Object> node : nodes) {
      Integer id = node.get("id") instanceof Number n ? n.intValue() : null;
      int self = id == null ? 0 : direct.getOrDefault(id, 0);
      Object childrenObj = node.get("children");
      int childSum = 0;
      if (childrenObj instanceof List<?> list && !list.isEmpty()) {
        childSum = fillKnowledgeQuestionCounts((List<Map<String, Object>>) list, direct);
      }
      int total = self + childSum;
      node.put("questionCount", total);
      sum += total;
    }
    return sum;
  }

  private static long toLong(Object v) {
    if (v instanceof Number n) {
      return n.longValue();
    }
    if (v == null) {
      return 0L;
    }
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (Exception e) {
      return 0L;
    }
  }

  private List<Integer> listKnowledgeIdsByCourses(Collection<Integer> courseIds) {
    if (courseIds == null || courseIds.isEmpty()) {
      return List.of();
    }
    return knowledgeStructureMapper.selectList(
            new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
                .select(ExamKnowledgeStructureEntity::getId)
                .in(ExamKnowledgeStructureEntity::getCourseId, courseIds)
                .eq(ExamKnowledgeStructureEntity::getDeleted, 0))
        .stream()
        .map(ExamKnowledgeStructureEntity::getId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private Map<Integer, Integer> loadKnowledgeCourseMap(Collection<Integer> knowledgeIds) {
    if (knowledgeIds == null || knowledgeIds.isEmpty()) {
      return Map.of();
    }
    List<Integer> ids = knowledgeIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return knowledgeStructureMapper.selectBatchIds(ids).stream()
        .filter(n -> n.getId() != null && n.getCourseId() != null)
        .collect(Collectors.toMap(ExamKnowledgeStructureEntity::getId, ExamKnowledgeStructureEntity::getCourseId, (a, b) -> a));
  }

  private Integer resolveQuestionCourseId(
      ExamExamDetailEntity d,
      List<Integer> filterCourses,
      Map<Integer, Integer> knowledgeCourseMap) {
    if (d.getExamId() != null && d.getExamId() > 0) {
      return d.getExamId();
    }
    if (d.getKnowledgeId() != null && knowledgeCourseMap.containsKey(d.getKnowledgeId())) {
      return knowledgeCourseMap.get(d.getKnowledgeId());
    }
    if (filterCourses != null && filterCourses.size() == 1) {
      return filterCourses.get(0);
    }
    return d.getExamId();
  }

  private String buildBankEmptyHint(
      ScopeFilter scope,
      List<Integer> filterCourses,
      String keyword,
      String questionType) {
    if (StringUtils.hasText(keyword) || StringUtils.hasText(questionType)) {
      return "当前筛选条件下暂无客观题，可切换科目、题型或清空关键词";
    }
    if (scope.configured) {
      if (filterCourses != null && filterCourses.size() == 1) {
        return "该科目在练习范围内暂无可用客观题。可换科目，或改用「自拟题目」";
      }
      return "本班练习范围内暂无可用客观题。请检查后台科目/知识点配置，或改用「自拟题目」";
    }
    if (filterCourses == null || filterCourses.isEmpty()) {
      return "未配置练习范围且无推荐科目，暂无试题。请先在后台配置班级练习范围";
    }
    return "推荐科目下暂无客观题，可切换科目或改用「自拟题目」";
  }

  private record BankScopeQuery(
      List<Integer> knowledgeIds,
      List<Integer> examIds,
      boolean impossible,
      String emptyHint) {
    static BankScopeQuery unrestricted() {
      return new BankScopeQuery(null, null, false, null);
    }

    static BankScopeQuery byKnowledgeIds(List<Integer> kids, String hint) {
      return new BankScopeQuery(kids, null, false, hint);
    }

    static BankScopeQuery byExamIds(List<Integer> eids, String hint) {
      return new BankScopeQuery(null, eids, false, hint);
    }

    static BankScopeQuery byKnowledgeOrExam(List<Integer> kids, List<Integer> eids, String hint) {
      return new BankScopeQuery(kids, eids, false, hint);
    }

    static BankScopeQuery impossible(String hint) {
      return new BankScopeQuery(List.of(), List.of(), true, hint);
    }
  }

  private ScopeFilter loadScopeFilter(Long classId, Long tenantId) {
    List<Integer> courseIds = practiceCourseMapper.selectList(
            new LambdaQueryWrapper<TrainClassPracticeCourseEntity>()
                .eq(TrainClassPracticeCourseEntity::getClassId, classId)
                .eq(tenantId != null && tenantId > 0, TrainClassPracticeCourseEntity::getTenantId, tenantId)
                .eq(TrainClassPracticeCourseEntity::getDeleted, 0))
        .stream()
        .map(TrainClassPracticeCourseEntity::getExamCourseId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    List<Integer> knowledgeIds = practiceKnowledgeMapper.selectList(
            new LambdaQueryWrapper<TrainClassPracticeKnowledgeEntity>()
                .eq(TrainClassPracticeKnowledgeEntity::getClassId, classId)
                .eq(tenantId != null && tenantId > 0, TrainClassPracticeKnowledgeEntity::getTenantId, tenantId)
                .eq(TrainClassPracticeKnowledgeEntity::getDeleted, 0))
        .stream()
        .map(TrainClassPracticeKnowledgeEntity::getKnowledgeId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    boolean configured = !courseIds.isEmpty() || !knowledgeIds.isEmpty();
    List<Integer> fallback = List.of();
    if (!configured) {
      TrainInfoClassEntity clazz = classMapper.selectById(classId);
      if (clazz != null && clazz.getWorkTypeId() != null && clazz.getWorkTypeId() > 0) {
        LambdaQueryWrapper<ExamCourseEntity> cq = new LambdaQueryWrapper<ExamCourseEntity>()
            .eq(ExamCourseEntity::getWorkTypeId, clazz.getWorkTypeId().intValue())
            .orderByAsc(ExamCourseEntity::getId)
            .last("LIMIT 20");
        if (clazz.getWorkTypeLevel() != null && clazz.getWorkTypeLevel() > 0) {
          cq.eq(ExamCourseEntity::getLevel, clazz.getWorkTypeLevel());
        }
        fallback = examCourseMapper.selectList(cq).stream().map(ExamCourseEntity::getId).toList();
        if (fallback.isEmpty() && clazz.getWorkTypeLevel() != null) {
          fallback = examCourseMapper.selectList(new LambdaQueryWrapper<ExamCourseEntity>()
                  .eq(ExamCourseEntity::getWorkTypeId, clazz.getWorkTypeId().intValue())
                  .orderByAsc(ExamCourseEntity::getId)
                  .last("LIMIT 20"))
              .stream().map(ExamCourseEntity::getId).toList();
        }
      }
    }
    return new ScopeFilter(configured, courseIds, knowledgeIds, fallback);
  }

  private static List<String> typeAliases(String normalized) {
    return switch (normalized) {
      case "single" -> List.of("single", "danxuan");
      case "multi" -> List.of("multi", "duoxuan");
      case "judge" -> List.of("judge", "pandan");
      default -> List.of(normalized);
    };
  }

  private record ScopeFilter(
      boolean configured,
      List<Integer> courseIds,
      List<Integer> knowledgeIds,
      List<Integer> fallbackCourseIds) {}


  private int generateItems(TrainFollowTaskEntity task, Long operatorId) {
    List<TrainPeopleListEntity> students = peopleMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, task.getClassId())
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0));
    int added = 0;
    for (TrainPeopleListEntity s : students) {
      if (findItem(task.getId(), s.getId()) != null) {
        continue;
      }
      insertItem(task, s, operatorId);
      added++;
    }
    return added;
  }

  private void insertItem(TrainFollowTaskEntity task, TrainPeopleListEntity people, Long operatorId) {
    TrainFollowTaskItemEntity item = new TrainFollowTaskItemEntity();
    item.setTaskId(task.getId());
    item.setPeopleId(people.getId());
    item.setUserName(people.getRealname());
    item.setStatus(ITEM_PENDING);
    item.setTenantId(task.getTenantId());
    item.setCreatedBy(operatorId);
    item.setUpdatedBy(operatorId);
    item.setDeleted(0);
    itemMapper.insert(item);
  }

  private GradeResult grade(TrainFollowTaskQuestionEntity q, Object userAns) {
    BigDecimal full = q.getScore() != null ? q.getScore() : BigDecimal.ONE;
    Object correct = parseJson(q.getAnswerJson());
    boolean ok = answersEqual(q.getQuestionType(), correct, userAns);
    return new GradeResult(ok, ok ? full : BigDecimal.ZERO);
  }

  private boolean answersEqual(String type, Object correct, Object user) {
    if (correct == null && user == null) {
      return true;
    }
    if (correct == null || user == null) {
      return false;
    }
    if ("multi".equals(type)) {
      Set<String> a = toNormSet(correct);
      Set<String> b = toNormSet(user);
      return a.equals(b);
    }
    return normalizeAnswer(correct).equals(normalizeAnswer(user));
  }

  private Set<String> toNormSet(Object v) {
    Set<String> set = new HashSet<>();
    if (v instanceof Collection<?> c) {
      for (Object o : c) {
        set.add(normalizeAnswer(o));
      }
    } else if (v instanceof String s && s.contains(",")) {
      for (String p : s.split(",")) {
        set.add(normalizeAnswer(p));
      }
    } else {
      set.add(normalizeAnswer(v));
    }
    return set;
  }

  private String normalizeAnswer(Object v) {
    if (v == null) {
      return "";
    }
    return v.toString().trim().toUpperCase(Locale.ROOT);
  }

  private boolean isPassed(TrainFollowTaskEntity task, BigDecimal score) {
    if (task.getPassScore() != null) {
      return score.compareTo(task.getPassScore()) >= 0;
    }
    return true;
  }

  private FollowTaskResultVO buildResult(
      TrainFollowTaskEntity task, TrainFollowTaskItemEntity item, boolean showCorrectAnswers) {
    List<TrainFollowTaskQuestionEntity> qs = listQuestions(task.getId());
    Map<Long, TrainFollowTaskAnswerEntity> ansMap = answerMapper.selectList(
            new LambdaQueryWrapper<TrainFollowTaskAnswerEntity>()
                .eq(TrainFollowTaskAnswerEntity::getItemId, item.getId())
                .eq(TrainFollowTaskAnswerEntity::getDeleted, 0))
        .stream()
        .collect(Collectors.toMap(TrainFollowTaskAnswerEntity::getQuestionId, a -> a, (a, b) -> a));

    boolean showAnalysisText = Objects.equals(task.getShowAnalysisAfterSubmit(), 1)
        || STATUS_CLOSED.equals(task.getStatus());

    List<FollowTaskAnswerResultVO> rows = new ArrayList<>();
    int correctCount = 0;
    for (TrainFollowTaskQuestionEntity q : qs) {
      TrainFollowTaskAnswerEntity a = ansMap.get(q.getId());
      boolean ok = a != null && a.getIsCorrect() != null && a.getIsCorrect() == 1
          && hasAnswerContent(a.getAnswerJson());
      if (ok) {
        correctCount++;
      }
      rows.add(new FollowTaskAnswerResultVO(
          String.valueOf(q.getId()),
          q.getSortNumber(),
          q.getQuestionType(),
          q.getStem(),
          q.getOptionsJson(),
          a != null ? a.getAnswerJson() : null,
          showCorrectAnswers ? q.getAnswerJson() : null,
          showAnalysisText ? q.getAnalysis() : null,
          a != null && a.getIsCorrect() != null && a.getIsCorrect() == 1,
          a != null ? a.getScore() : null,
          q.getScore()));
    }
    int qCount = qs.size();
    BigDecimal accuracy = qCount <= 0
        ? null
        : BigDecimal.valueOf(correctCount * 100.0 / qCount).setScale(1, RoundingMode.HALF_UP);
    return new FollowTaskResultVO(
        String.valueOf(task.getId()),
        String.valueOf(item.getId()),
        item.getStatus(),
        item.getScore(),
        item.getPassed() != null && item.getPassed() == 1,
        showCorrectAnswers,
        correctCount,
        qCount,
        accuracy,
        item.getDurationSeconds(),
        rows);
  }

  private FollowTaskVO toTaskVO(TrainFollowTaskEntity t, boolean withQuestions, boolean withStats) {
    List<FollowTaskQuestionVO> qs = null;
    int qCount = 0;
    if (withQuestions) {
      List<TrainFollowTaskQuestionEntity> list = listQuestions(t.getId());
      qCount = list.size();
      qs = list.stream().map(q -> toQuestionVO(q, true)).toList();
    } else {
      qCount = Math.toIntExact(questionMapper.selectCount(new LambdaQueryWrapper<TrainFollowTaskQuestionEntity>()
          .eq(TrainFollowTaskQuestionEntity::getTaskId, t.getId())
          .eq(TrainFollowTaskQuestionEntity::getDeleted, 0)));
    }
    Integer total = null;
    Integer submitted = null;
    Integer graded = null;
    BigDecimal rate = null;
    if (withStats && !STATUS_DRAFT.equals(t.getStatus())) {
      List<TrainFollowTaskItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskItemEntity>()
          .eq(TrainFollowTaskItemEntity::getTaskId, t.getId())
          .eq(TrainFollowTaskItemEntity::getDeleted, 0));
      total = items.size();
      submitted = (int) items.stream()
          .filter(i -> !ITEM_PENDING.equals(i.getStatus()))
          .count();
      graded = (int) items.stream()
          .filter(i -> ITEM_GRADED.equals(i.getStatus()))
          .count();
      int passed = (int) items.stream()
          .filter(i -> i.getPassed() != null && i.getPassed() == 1)
          .count();
      rate = total == 0
          ? BigDecimal.ZERO
          : BigDecimal.valueOf(passed * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }
    return new FollowTaskVO(
        String.valueOf(t.getId()),
        t.getClassId(),
        t.getTrainInfoId(),
        t.getScheduleId(),
        t.getBizDate(),
        t.getTaskType(),
        TASK_TYPE_NAMES.getOrDefault(t.getTaskType(), t.getTaskType()),
        t.getTitle(),
        t.getDescription(),
        t.getDeadlineAt(),
        t.getStatus(),
        STATUS_NAMES.getOrDefault(t.getStatus(), t.getStatus()),
        t.getPassScore(),
        t.getPassRule(),
        Objects.equals(t.getShowAnalysisAfterSubmit(), 1),
        t.getPublishedAt(),
        t.getCreatedAt(),
        qCount,
        total,
        submitted,
        graded,
        rate,
        qs,
        t.getMakeExamPlanId(),
        t.getSourcePaperId(),
        resolveMakeExamPlanTitle(t.getMakeExamPlanId()));
  }

  private String resolveMakeExamPlanTitle(Integer makeExamPlanId) {
    if (makeExamPlanId == null || makeExamPlanId <= 0) {
      return null;
    }
    ExamMakeExamPlanEntity plan = makeExamPlanMapper.selectById(makeExamPlanId);
    return plan != null ? plan.getTitle() : null;
  }

  private FollowTaskQuestionVO toQuestionVO(TrainFollowTaskQuestionEntity q, boolean includeAnswer) {
    return new FollowTaskQuestionVO(
        String.valueOf(q.getId()),
        q.getSortNumber(),
        q.getQuestionType(),
        q.getStem(),
        q.getOptionsJson(),
        q.getScore(),
        includeAnswer ? q.getAnswerJson() : null,
        includeAnswer ? q.getAnalysis() : null);
  }

  private FollowTaskItemVO toItemVO(
      TrainFollowTaskItemEntity i,
      Integer correctCount,
      Integer questionCount,
      BigDecimal accuracyRate) {
    return new FollowTaskItemVO(
        String.valueOf(i.getId()),
        String.valueOf(i.getTaskId()),
        i.getPeopleId(),
        i.getUserName(),
        i.getStatus(),
        ITEM_STATUS_NAMES.getOrDefault(i.getStatus(), i.getStatus()),
        i.getScore(),
        i.getPassed() != null && i.getPassed() == 1,
        i.getSubmittedAt(),
        i.getGradedAt(),
        i.getComment(),
        correctCount,
        questionCount,
        accuracyRate,
        i.getStartedAt(),
        i.getDurationSeconds());
  }

  private List<TrainFollowTaskQuestionEntity> listQuestions(Long taskId) {
    return questionMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskQuestionEntity>()
            .eq(TrainFollowTaskQuestionEntity::getTaskId, taskId)
            .eq(TrainFollowTaskQuestionEntity::getDeleted, 0)
            .orderByAsc(TrainFollowTaskQuestionEntity::getSortNumber))
        .stream()
        .sorted(Comparator.comparing(q -> q.getSortNumber() == null ? 0 : q.getSortNumber()))
        .toList();
  }

  private TrainFollowTaskItemEntity findItem(Long taskId, Long peopleId) {
    return itemMapper.selectOne(new LambdaQueryWrapper<TrainFollowTaskItemEntity>()
        .eq(TrainFollowTaskItemEntity::getTaskId, taskId)
        .eq(TrainFollowTaskItemEntity::getPeopleId, peopleId)
        .eq(TrainFollowTaskItemEntity::getDeleted, 0)
        .last("LIMIT 1"));
  }

  /** 是否存在至少一条非空作答（排除历史雪花 ID 精度丢失导致的空答卷） */
  private boolean hasAnyAnswerContent(Long itemId) {
    if (itemId == null) {
      return false;
    }
    List<TrainFollowTaskAnswerEntity> rows = answerMapper.selectList(
        new LambdaQueryWrapper<TrainFollowTaskAnswerEntity>()
            .eq(TrainFollowTaskAnswerEntity::getItemId, itemId)
            .eq(TrainFollowTaskAnswerEntity::getDeleted, 0));
    for (TrainFollowTaskAnswerEntity row : rows) {
      if (StringUtils.hasText(row.getAnswerJson()) && !"null".equalsIgnoreCase(row.getAnswerJson().trim())) {
        return true;
      }
    }
    return false;
  }

  private TrainPeopleListEntity requireStudentPeople(Long platformUserId, Long classId) {
    TrainPeopleListEntity people = peopleMapper.selectOne(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getPlatformUserId, platformUserId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .last("LIMIT 1"));
    if (people == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "非本班学员");
    }
    return people;
  }

  private TrainFollowTaskEntity requireTask(Long taskId) {
    if (taskId == null || taskId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "taskId 无效");
    }
    TrainFollowTaskEntity task = taskMapper.selectById(taskId);
    if (task == null || Objects.equals(task.getDeleted(), 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
    }
    return task;
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    if (clazz == null || Objects.equals(clazz.getDeleted(), 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    return clazz;
  }

  private void validateMount(FollowTaskSaveReq req, String taskType) {
    if (TYPE_MOCK.equals(taskType)) {
      // 模拟考挂载可选：有课次/业务日则展示，无则仅按班级
      return;
    }
    boolean hasSchedule = req.getScheduleId() != null && req.getScheduleId() > 0;
    boolean hasDate = StringUtils.hasText(req.getBizDate());
    if (!hasSchedule && !hasDate) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择课次或填写业务日");
    }
  }

  private String normalizeTaskType(String type) {
    if (!StringUtils.hasText(type) || !TASK_TYPES.contains(type.trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "taskType 须为 homework、practice 或 mock");
    }
    return type.trim();
  }

  private String normalizeQType(String type) {
    if (!StringUtils.hasText(type) || !Q_TYPES.contains(type.trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "题型仅支持 single/multi/judge");
    }
    return type.trim();
  }

  private Long resolveTenantId(Long classId) {
    Long ctx = TenantContextHolder.getTenantId();
    if (ctx != null && ctx > 0) {
      return ctx;
    }
    try {
      Long tid = SecurityContext.currentUser().getTenantId();
      if (tid != null && tid > 0) {
        return tid;
      }
    } catch (Exception ignored) {
    }
    TrainScheduleEntity one = scheduleMapper.selectOne(new LambdaQueryWrapper<TrainScheduleEntity>()
        .eq(TrainScheduleEntity::getClassId, classId)
        .eq(TrainScheduleEntity::getDeleted, 0)
        .last("LIMIT 1"));
    if (one != null && one.getTenantId() != null && one.getTenantId() > 0) {
      return one.getTenantId();
    }
    return 1L;
  }

  private LocalDateTime parseDateTime(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String s = raw.trim().replace("T", " ");
    try {
      if (s.length() == 10) {
        return LocalDateTime.parse(s + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      }
      if (s.length() == 16) {
        return LocalDateTime.parse(s + ":00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      }
      return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "截止时间格式无效");
    }
  }

  private String toJson(Object v) {
    if (v == null) {
      return null;
    }
    try {
      if (v instanceof String s) {
        String t = s.trim();
        if (t.startsWith("[") || t.startsWith("{") || t.startsWith("\"")) {
          return t;
        }
        return objectMapper.writeValueAsString(t);
      }
      return objectMapper.writeValueAsString(v);
    } catch (JsonProcessingException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "JSON 序列化失败");
    }
  }

  private Object parseJson(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    try {
      return objectMapper.readValue(raw, new TypeReference<Object>() {});
    } catch (Exception e) {
      return raw;
    }
  }

  private static String trimToNull(String s) {
    if (!StringUtils.hasText(s)) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private record GradeResult(boolean correct, BigDecimal score) {}
}
