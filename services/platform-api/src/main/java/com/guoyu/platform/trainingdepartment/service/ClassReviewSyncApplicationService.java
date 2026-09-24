package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewGrayWhitelistEntity;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewGrayWhitelistMapper;
import com.guoyu.platform.exam.reviewsync.service.ReviewPlanApplicationService;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import com.guoyu.platform.trainingdepartment.dto.ClassPracticeScopeSaveReq;
import com.guoyu.platform.trainingdepartment.dto.ClassReviewSyncSaveReq;
import com.guoyu.platform.trainingdepartment.entity.TrainClassReviewSyncEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassReviewSyncMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 班级同步学配置（通道 C 绑定层）：class → exam，开通名单/灰度。
 * <p>不改 /appreviewsync 引擎；仅做绑定与开通编排。
 */
@Service
public class ClassReviewSyncApplicationService {

  private static final Logger log = LoggerFactory.getLogger(ClassReviewSyncApplicationService.class);

  private final TrainInfoClassMapper classMapper;
  private final TrainClassReviewSyncMapper reviewSyncMapper;
  private final TrainExamListMapper examListMapper;
  private final TrainExamWorkTypeListMapper workTypeListMapper;
  private final TrainPeopleListMapper peopleMapper;
  private final TrainExamPeopleListMapper examPeopleMapper;
  private final AiReviewGrayWhitelistMapper grayWhitelistMapper;
  private final DictMapper dictMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final TrainExamListApplicationService examListApplicationService;
  private final ReviewPlanApplicationService reviewPlanApplicationService;
  private final JdbcTemplate jdbcTemplate;
  private final ConcurrentHashMap<Long, Object> classLocks = new ConcurrentHashMap<>();

  public ClassReviewSyncApplicationService(
      TrainInfoClassMapper classMapper,
      TrainClassReviewSyncMapper reviewSyncMapper,
      TrainExamListMapper examListMapper,
      TrainExamWorkTypeListMapper workTypeListMapper,
      TrainPeopleListMapper peopleMapper,
      TrainExamPeopleListMapper examPeopleMapper,
      AiReviewGrayWhitelistMapper grayWhitelistMapper,
      DictMapper dictMapper,
      TrainingOperatorScopeService operatorScopeService,
      TrainExamListApplicationService examListApplicationService,
      ReviewPlanApplicationService reviewPlanApplicationService,
      JdbcTemplate jdbcTemplate) {
    this.classMapper = classMapper;
    this.reviewSyncMapper = reviewSyncMapper;
    this.examListMapper = examListMapper;
    this.workTypeListMapper = workTypeListMapper;
    this.peopleMapper = peopleMapper;
    this.examPeopleMapper = examPeopleMapper;
    this.grayWhitelistMapper = grayWhitelistMapper;
    this.dictMapper = dictMapper;
    this.operatorScopeService = operatorScopeService;
    this.examListApplicationService = examListApplicationService;
    this.reviewPlanApplicationService = reviewPlanApplicationService;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getConfig(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = resolveTenantId();
    TrainClassReviewSyncEntity cfg = findByClass(tenantId, classId);
    return buildConfigView(clazz, cfg, tenantId);
  }

  @Transactional
  public Map<String, Object> saveConfig(Long classId, ClassReviewSyncSaveReq req) {
    operatorScopeService.requireOrgOperator();
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = resolveTenantId();
    if (req == null || req.getExamId() == null || req.getExamId() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择同步学考试计划");
    }
    if (req.getSyncStartTime() != null && req.getSyncEndTime() != null
        && req.getSyncStartTime().isAfter(req.getSyncEndTime())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "开练开始日期不能晚于截止日期");
    }
    TrainExamListEntity exam = requireExam(req.getExamId());
    warnIfWorkTypeMismatch(clazz, exam);

    LocalDateTime now = LocalDateTime.now();
    TrainClassReviewSyncEntity existing = findByClass(tenantId, classId);
    Long previousExamId = existing != null && existing.getExamId() != null && existing.getExamId() > 0
        ? existing.getExamId() : null;
    if (existing == null) {
      // 软删行仍占 uk_class_review_sync(tenant_id, class_id)，插入前物理清幽灵
      reviewSyncMapper.physicalDeleteByClass(tenantId, classId);
      TrainClassReviewSyncEntity row = new TrainClassReviewSyncEntity();
      row.setTenantId(tenantId);
      row.setClassId(classId);
      applySaveFields(row, req, now);
      row.setCreateTime(now);
      row.setDeleted(0);
      reviewSyncMapper.insert(row);
    } else {
      applySaveFields(existing, req, now);
      reviewSyncMapper.updateById(existing);
    }
    // 改绑后从旧计划移出本班学员，避免 App「考试任务」同时出现错绑计划
    if (previousExamId != null && !previousExamId.equals(req.getExamId())) {
      int removed = unenrollClassRosterFromExam(classId, previousExamId);
      if (removed > 0) {
        log.info("[班级同步学] 改绑 exam {}→{}，已从旧计划移出本班考生 {} 人 classId={}",
            previousExamId, req.getExamId(), removed, classId);
      }
    }
    Map<String, Object> view = buildConfigView(clazz, findByClass(tenantId, classId), tenantId);
    int enabled = req.getEnabled() != null && req.getEnabled() == 0 ? 0 : 1;
    if (enabled == 1) {
      Map<String, Object> opened = tryOpenEnrollmentAndGray(classId);
      view.put("enroll", opened.get("enroll"));
      view.put("gray", opened.get("gray"));
      view.put("enrollError", opened.get("enrollError"));
      view.put("grayError", opened.get("grayError"));
      view.put("autoSynced", true);
      TrainClassReviewSyncEntity saved = findByClass(tenantId, classId);
      if (saved != null && (saved.getAutoEnroll() == null || saved.getAutoEnroll() != 1)) {
        saved.setAutoEnroll(1);
        saved.setUpdateTime(LocalDateTime.now());
        reviewSyncMapper.updateById(saved);
      }
    }
    return view;
  }

  /**
   * 跟班「同步学」Tab：已绑定则同步花名册；未绑定则匹配/新建考试计划后开通。
   */
  @Transactional
  public Map<String, Object> ensureBoundAndSyncRoster(Long classId) {
    operatorScopeService.requireOrgOperator();
    synchronized (lockFor(classId)) {
      TrainInfoClassEntity clazz = requireClass(classId);
      Long tenantId = resolveTenantId();
      TrainClassReviewSyncEntity existing = findByClass(tenantId, classId);
      if (existing != null && existing.getExamId() != null && existing.getExamId() > 0) {
        TrainExamListEntity exam = examListMapper.selectOne(new LambdaQueryWrapper<TrainExamListEntity>()
            .eq(TrainExamListEntity::getId, existing.getExamId())
            .eq(TrainExamListEntity::getDeleted, 0)
            .last("LIMIT 1"));
        if (exam != null) {
          String classLevel = clazz.getWorkTypeLevel() != null ? String.valueOf(clazz.getWorkTypeLevel()) : null;
          String expectedSyncTitle = expectedClassSyncExamTitle(clazz);
          String examTitle = exam.getTitle() == null ? null : exam.getTitle().trim();
          boolean titleOwnsClass = expectedSyncTitle != null && expectedSyncTitle.equals(examTitle);
          boolean otherClassSync = isClassScopedSyncExamTitle(examTitle) && !titleOwnsClass;
          List<TrainExamWorkTypeListEntity> bindings = workTypeListMapper.selectList(
              new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
                  .eq(TrainExamWorkTypeListEntity::getExamId, exam.getId())
                  .eq(TrainExamWorkTypeListEntity::getDeleted, 0));
          int boundScore = computeMatchScore(bindings, clazz.getWorkTypeId(), classLevel);
          if (boundScore == 1 && StringUtils.hasText(classLevel) && StringUtils.hasText(exam.getLevel())
              && classLevel.equals(exam.getLevel().trim())) {
            boundScore = 2;
          }
          // 他班「*-同步学」或等级不匹配 → 重选/新建
          if (otherClassSync || (StringUtils.hasText(classLevel) && boundScore < 2 && !titleOwnsClass)) {
            log.warn("[班级同步学] 跟班同步 Tab 发现错绑 examId={}「{}」score={} otherClassSync={}，将重新匹配 classId={}",
                exam.getId(), exam.getTitle(), boundScore, otherClassSync, classId);
          } else {
            boolean enabledNow = existing.getEnabled() != null && existing.getEnabled() == 1;
            Map<String, Object> opened = tryOpenEnrollmentAndGray(classId, enabledNow);
            Map<String, Object> cfg = buildConfigView(clazz, existing, tenantId);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("action", "synced");
            out.put("examSource", "REUSE_BINDING");
            out.put("examId", String.valueOf(existing.getExamId()));
            out.put("config", cfg);
            out.put("enroll", opened.get("enroll"));
            out.put("gray", opened.get("gray"));
            out.put("enrollError", opened.get("enrollError"));
            out.put("grayError", opened.get("grayError"));
            @SuppressWarnings("unchecked")
            Map<String, Object> enroll = opened.get("enroll") instanceof Map
                ? (Map<String, Object>) opened.get("enroll") : null;
            @SuppressWarnings("unchecked")
            Map<String, Object> gray = opened.get("gray") instanceof Map
                ? (Map<String, Object>) opened.get("gray") : null;
            out.put("message", buildAutoProvisionMessage(
                "REUSE_BINDING", cfg, enroll,
                (String) opened.get("enrollError"), gray, (String) opened.get("grayError")));
            return out;
          }
        }
      }
      ClassPracticeScopeSaveReq req = new ClassPracticeScopeSaveReq();
      req.setEnabled(1);
      return autoProvisionFromPracticeScope(classId, req);
    }
  }

  /**
   * 花名册变更后：若本班已绑定同步学则补名单（幂等，未配置则跳过）。
   */
  public Map<String, Object> syncRosterIfBound(Long classId) {
    if (classId == null || classId <= 0) {
      return Map.of("ok", true, "action", "skipped");
    }
    try {
      Long tenantId = resolveTenantId();
      TrainClassReviewSyncEntity cfg = findByClass(tenantId, classId);
      if (cfg == null || cfg.getExamId() == null) {
        return Map.of("ok", true, "action", "skipped");
      }
      return enrollRoster(classId);
    } catch (Exception e) {
      log.warn("[班级同步学] 花名册变更后自动同步名单失败 classId={}: {}", classId, e.getMessage());
      Map<String, Object> failed = new LinkedHashMap<>();
      failed.put("ok", false);
      failed.put("action", "failed");
      failed.put("message", e.getMessage());
      return failed;
    }
  }

  /**
   * 试题配置保存后自动开通同步学：绑定 exam、同步名单、写入灰度。
   * 独立事务：失败不回滚外层刷题范围保存（由调用方 fail-soft）。
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<String, Object> autoProvisionFromPracticeScope(Long classId, ClassPracticeScopeSaveReq practiceReq) {
    operatorScopeService.requireOrgOperator();
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = resolveTenantId();
    int enabled = practiceReq != null && practiceReq.getEnabled() != null && practiceReq.getEnabled() == 0 ? 0 : 1;
    TrainClassReviewSyncEntity existing = findByClass(tenantId, classId);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("ok", true);

    if (enabled == 0) {
      if (existing == null || existing.getExamId() == null) {
        out.put("action", "skipped");
        out.put("message", "试题配置已停用，本班尚未绑定同步学");
        return out;
      }
      ClassReviewSyncSaveReq disableReq = new ClassReviewSyncSaveReq();
      disableReq.setExamId(existing.getExamId());
      disableReq.setEnabled(0);
      disableReq.setSyncStartTime(practiceReq == null ? null : practiceReq.getPracticeStartTime());
      disableReq.setSyncEndTime(practiceReq == null ? null : practiceReq.getPracticeEndTime());
      disableReq.setRemark(mergeAutoRemark(existing.getRemark(), practiceReq == null ? null : practiceReq.getRemark()));
      Map<String, Object> cfg = saveConfig(classId, disableReq);
      out.put("action", "disabled");
      out.put("message", "试题配置已停用，同步学入口已同步关闭");
      out.put("config", cfg);
      return out;
    }

    Long examId;
    String examSource;
    try {
      ExamPick pick = resolveExamForAutoProvision(clazz, existing, practiceReq);
      examId = pick.examId();
      examSource = pick.source();
    } catch (BusinessException e) {
      out.put("ok", false);
      out.put("action", "failed");
      out.put("message", e.getMessage());
      return out;
    }

    // 刷题科目写回考试计划（复用/匹配计划常缺 courseId 或 source 不一致 →「未绑定有效题库」）
    List<Integer> practiceCourses = practiceReq == null ? List.of() : practiceReq.getCourseIds();
    if (practiceCourses != null && !practiceCourses.isEmpty()) {
      try {
        examListApplicationService.syncCoursesForReviewSync(examId, practiceCourses);
      } catch (Exception e) {
        log.warn("[班级同步学] 同步试题配置科目到考试计划失败 examId={}: {}", examId, e.getMessage());
      }
    }

    ClassReviewSyncSaveReq saveReq = new ClassReviewSyncSaveReq();
    saveReq.setExamId(examId);
    saveReq.setEnabled(1);
    saveReq.setSyncStartTime(practiceReq == null ? null : practiceReq.getPracticeStartTime());
    saveReq.setSyncEndTime(practiceReq == null ? null : practiceReq.getPracticeEndTime());
    saveReq.setRemark(mergeAutoRemark(existing == null ? null : existing.getRemark(),
        practiceReq == null ? null : practiceReq.getRemark()));
    Map<String, Object> cfg = saveConfig(classId, saveReq);
    @SuppressWarnings("unchecked")
    Map<String, Object> enroll = cfg.get("enroll") instanceof Map ? (Map<String, Object>) cfg.get("enroll") : null;
    @SuppressWarnings("unchecked")
    Map<String, Object> gray = cfg.get("gray") instanceof Map ? (Map<String, Object>) cfg.get("gray") : null;
    String enrollError = cfg.get("enrollError") instanceof String ? (String) cfg.get("enrollError") : null;
    String grayError = cfg.get("grayError") instanceof String ? (String) cfg.get("grayError") : null;

    out.put("action", "CREATED".equals(examSource) ? "created" : "bound");
    out.put("examSource", examSource);
    out.put("examId", String.valueOf(examId));
    out.put("config", cfg);
    out.put("enroll", enroll);
    out.put("gray", gray);
    out.put("message", buildAutoProvisionMessage(examSource, cfg, enroll, enrollError, gray, grayError));
    if (enrollError != null) {
      out.put("enrollError", enrollError);
    }
    if (grayError != null) {
      out.put("grayError", grayError);
    }
    return out;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listExamOptions(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    List<TrainExamListEntity> exams = List.of();
    if (clazz.getTrainInfoId() != null && clazz.getTrainInfoId() > 0) {
      exams = examListMapper.selectList(new LambdaQueryWrapper<TrainExamListEntity>()
          .eq(TrainExamListEntity::getTrainId, clazz.getTrainInfoId())
          .eq(TrainExamListEntity::getDeleted, 0)
          .orderByDesc(TrainExamListEntity::getId)
          .last("LIMIT 200"));
    }

    Set<Long> examIds = exams.stream().map(TrainExamListEntity::getId).collect(Collectors.toSet());
    Map<Long, List<TrainExamWorkTypeListEntity>> wtMap = batchLoadWorkTypes(examIds);

    Long classWt = clazz.getWorkTypeId();
    String classLevel = clazz.getWorkTypeLevel() != null ? String.valueOf(clazz.getWorkTypeLevel()) : null;
    String expectedSyncTitle = expectedClassSyncExamTitle(clazz);

    List<Map<String, Object>> items = new ArrayList<>();
    for (TrainExamListEntity exam : exams) {
      List<TrainExamWorkTypeListEntity> bindings = wtMap.getOrDefault(exam.getId(), List.of());
      int matchScore = computeMatchScore(bindings, classWt, classLevel);
      if (matchScore == 1 && StringUtils.hasText(classLevel) && StringUtils.hasText(exam.getLevel())
          && classLevel.equals(exam.getLevel().trim())) {
        matchScore = 2;
      }
      String title = exam.getTitle() == null ? null : exam.getTitle().trim();
      boolean titleOwnsClass = expectedSyncTitle != null && expectedSyncTitle.equals(title);
      // 他班「*-同步学」在下拉里不算推荐匹配
      if (isClassScopedSyncExamTitle(title) && !titleOwnsClass) {
        matchScore = 0;
      } else if (titleOwnsClass) {
        matchScore = Math.max(matchScore, 2);
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("examId", String.valueOf(exam.getId()));
      row.put("title", exam.getTitle());
      row.put("level", exam.getLevel());
      row.put("matched", matchScore >= 2);
      row.put("workTypeMatched", matchScore >= 1);
      row.put("matchScore", matchScore);
      row.put("workTypeBindings", bindings.stream().map(b -> {
        Map<String, Object> bRow = new LinkedHashMap<>();
        bRow.put("workTypeId", b.getWorkTypeId());
        bRow.put("level", b.getLevel());
        bRow.put("courseId", b.getCourseId());
        return bRow;
      }).toList());
      items.add(row);
    }
    items.sort(Comparator
        .comparingInt((Map<String, Object> m) -> (Integer) m.get("matchScore")).reversed()
        .thenComparing(m -> String.valueOf(m.get("examId")), Comparator.reverseOrder()));

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", String.valueOf(classId));
    out.put("workTypeId", classWt);
    out.put("workTypeLevel", clazz.getWorkTypeLevel());
    out.put("trainInfoId", clazz.getTrainInfoId() == null ? null : String.valueOf(clazz.getTrainInfoId()));
    out.put("items", items);
    return out;
  }

  /** 花名册 → 考生名单（有 platform_user_id 的学员） */
  @Transactional
  public Map<String, Object> enrollRoster(Long classId) {
    operatorScopeService.requireOrgOperator();
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = resolveTenantId();
    TrainClassReviewSyncEntity cfg = requireConfigured(tenantId, classId);
    Long examId = cfg.getExamId();
    requireExam(examId);
    // App「复习任务」依赖 train_exam_review；同步学入口也仅在复习任务(result_type=1)展示
    boolean reviewEnsured = ensureExamReviewConfig(examId);

    List<TrainPeopleListEntity> roster = peopleMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0));

    List<TrainExamPeopleListEntity> existingPeople = examPeopleMapper.selectList(
        new LambdaQueryWrapper<TrainExamPeopleListEntity>()
            .eq(TrainExamPeopleListEntity::getExamId, examId)
            .eq(TrainExamPeopleListEntity::getDeleted, 0));
    Map<Long, TrainExamPeopleListEntity> byUserId = new HashMap<>();
    Map<String, TrainExamPeopleListEntity> byIdCard = new HashMap<>();
    for (TrainExamPeopleListEntity p : existingPeople) {
      if (p.getPlatformUserId() != null && p.getPlatformUserId() > 0) {
        byUserId.put(p.getPlatformUserId(), p);
      }
      if (StringUtils.hasText(p.getIdCardNum())) {
        byIdCard.put(p.getIdCardNum().trim().toUpperCase(), p);
      }
    }

    int inserted = 0;
    int existed = 0;
    int skippedNoAccount = 0;
    LocalDateTime now = LocalDateTime.now();
    Set<Long> seenUsers = new HashSet<>();

    for (TrainPeopleListEntity student : roster) {
      Long uid = student.getPlatformUserId();
      if (uid == null || uid <= 0) {
        skippedNoAccount++;
        continue;
      }
      if (!seenUsers.add(uid)) {
        continue;
      }
      TrainExamPeopleListEntity hit = byUserId.get(uid);
      if (hit == null && StringUtils.hasText(student.getIdCard())) {
        hit = byIdCard.get(student.getIdCard().trim().toUpperCase());
      }
      if (hit != null) {
        boolean dirty = false;
        if (hit.getPlatformUserId() == null || hit.getPlatformUserId() <= 0) {
          hit.setPlatformUserId(uid);
          dirty = true;
        }
        if (!StringUtils.hasText(hit.getRealname()) && StringUtils.hasText(student.getRealname())) {
          hit.setRealname(student.getRealname());
          dirty = true;
        }
        if (!StringUtils.hasText(hit.getPhone()) && StringUtils.hasText(student.getPhone())) {
          hit.setPhone(student.getPhone());
          dirty = true;
        }
        if (!StringUtils.hasText(hit.getIdCardNum()) && StringUtils.hasText(student.getIdCard())) {
          hit.setIdCardNum(student.getIdCard().trim());
          dirty = true;
        }
        if (dirty) {
          hit.setUpdateTime(now);
          examPeopleMapper.updateById(hit);
        }
        existed++;
        continue;
      }
      TrainExamPeopleListEntity row = new TrainExamPeopleListEntity();
      row.setExamId(examId);
      row.setPlatformUserId(uid);
      row.setRealname(student.getRealname());
      row.setPhone(student.getPhone());
      row.setIdCardNum(StringUtils.hasText(student.getIdCard()) ? student.getIdCard().trim() : null);
      row.setWorkTypeId(clazz.getWorkTypeId());
      if (clazz.getWorkTypeLevel() != null) {
        row.setWorkTypeLevel(String.valueOf(clazz.getWorkTypeLevel()));
      }
      row.setTenantId(tenantId);
      row.setSortNumber(1);
      row.setLearnTime(0);
      row.setLearnUseTime(0);
      row.setCreateTime(now);
      row.setUpdateTime(now);
      row.setDeleted(0);
      examPeopleMapper.insert(row);
      byUserId.put(uid, row);
      inserted++;
    }

    // 清掉本班学员在其它「*-同步学」计划上的残留（错绑改绑后 App 仍见双计划）
    int removedForeign = unenrollClassRosterFromOtherSyncExams(clazz, examId);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", String.valueOf(classId));
    out.put("examId", String.valueOf(examId));
    out.put("inserted", inserted);
    out.put("existed", existed);
    out.put("skippedNoAccount", skippedNoAccount);
    out.put("removedForeign", removedForeign);
    out.put("reviewEnsured", reviewEnsured);
    out.put("roster", buildRosterGap(classId, examId));
    return out;
  }

  /** 写入 exam 级灰度白名单（幂等） */
  @Transactional
  public Map<String, Object> ensureGray(Long classId) {
    operatorScopeService.requireOrgOperator();
    requireClass(classId);
    Long tenantId = resolveTenantId();
    TrainClassReviewSyncEntity cfg = requireConfigured(tenantId, classId);
    Long examId = cfg.getExamId();
    requireExam(examId);

    boolean grayEnabled = isGraySwitchOn(tenantId);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", String.valueOf(classId));
    out.put("examId", String.valueOf(examId));
    out.put("grayEnabled", grayEnabled);

    if (!grayEnabled) {
      out.put("examInGray", true);
      out.put("message", "已全量开放，无需白名单");
      out.put("inserted", false);
      return out;
    }

    Long count = grayWhitelistMapper.selectCount(new LambdaQueryWrapper<AiReviewGrayWhitelistEntity>()
        .eq(AiReviewGrayWhitelistEntity::getScopeType, "exam")
        .eq(AiReviewGrayWhitelistEntity::getScopeId, examId));
    if (count != null && count > 0) {
      out.put("examInGray", true);
      out.put("message", "该考试计划已在灰度白名单中");
      out.put("inserted", false);
      return out;
    }

    AiReviewGrayWhitelistEntity row = new AiReviewGrayWhitelistEntity();
    row.setTenantId(tenantId);
    row.setScopeType("exam");
    row.setScopeId(examId);
    row.setRemark("跟班班级同步学开通 classId=" + classId);
    grayWhitelistMapper.insert(row);

    out.put("examInGray", true);
    out.put("message", "已加入灰度白名单");
    out.put("inserted", true);
    return out;
  }

  /** 删班时清理同步学配置（物理删，避免唯一键被软删行占住） */
  @Transactional
  public int cleanupByClassId(Long classId) {
    if (classId == null) {
      return 0;
    }
    Long tenantId = resolveTenantId();
    return reviewSyncMapper.physicalDeleteByClass(tenantId, classId);
  }

  /**
   * 解析班级绑定的同步学 exam（供 App/助教复用）。
   * 管理端也可用于展示当前生效状态。
   */
  @Transactional(readOnly = true)
  public Map<String, Object> resolve(Long classId, LocalDateTime now) {
    if (classId == null || classId <= 0) {
      return resolveNull("NOT_CONFIGURED");
    }
    Long tenantId = resolveTenantId();
    TrainClassReviewSyncEntity cfg = findByClass(tenantId, classId);
    if (cfg == null) {
      return resolveNull("NOT_CONFIGURED");
    }
    if (cfg.getEnabled() == null || cfg.getEnabled() != 1) {
      return resolveNull("DISABLED");
    }
    LocalDateTime at = now != null ? now : LocalDateTime.now();
    if (cfg.getSyncStartTime() != null && at.isBefore(cfg.getSyncStartTime())) {
      return resolveNull("WINDOW");
    }
    if (cfg.getSyncEndTime() != null && at.isAfter(cfg.getSyncEndTime())) {
      return resolveNull("WINDOW");
    }
    TrainExamListEntity exam = examListMapper.selectOne(new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getId, cfg.getExamId())
        .eq(TrainExamListEntity::getDeleted, 0));
    if (exam == null) {
      return resolveNull("EXAM_GONE");
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("configured", true);
    out.put("examId", String.valueOf(exam.getId()));
    out.put("examTitle", exam.getTitle());
    out.put("enabled", true);
    out.put("reasonCode", null);
    out.put("syncStartTime", cfg.getSyncStartTime() == null ? null : cfg.getSyncStartTime().toLocalDate().toString());
    out.put("syncEndTime", cfg.getSyncEndTime() == null ? null : cfg.getSyncEndTime().toLocalDate().toString());
    return out;
  }

  /**
   * App 班成员：解析本班同步学入口（配置 + 灰度 + 考生名单）。
   * 不走管理端组织权限；租户取 {@link TenantContextHolder}（App principal.tenantId 常为 0）。
   */
  @Transactional(readOnly = true)
  public Map<String, Object> resolveForAppMember(Long classId, Long platformUserId) {
    Map<String, Object> out = new LinkedHashMap<>();
    Map<String, Object> resolved = resolve(classId, LocalDateTime.now());
    boolean configured = Boolean.TRUE.equals(resolved.get("configured"));
    out.put("configured", configured);
    out.put("examId", resolved.get("examId"));
    out.put("examTitle", resolved.get("examTitle"));
    out.put("enabled", resolved.get("enabled"));
    out.put("syncStartTime", resolved.get("syncStartTime"));
    out.put("syncEndTime", resolved.get("syncEndTime"));

    if (!configured) {
      String reason = resolved.get("reasonCode") == null
          ? "NOT_CONFIGURED"
          : String.valueOf(resolved.get("reasonCode"));
      out.put("entryVisible", false);
      out.put("reasonCode", reason);
      out.put("reasonMessage", appReasonMessage(reason));
      out.put("deepLink", null);
      out.put("grayAllowed", false);
      out.put("inRoster", false);
      return out;
    }

    Long examId = Long.parseLong(String.valueOf(resolved.get("examId")));
    Long tenantId = resolveTenantId();
    boolean grayAllowed = true;
    try {
      Map<String, Object> gray = reviewPlanApplicationService.grayAvailability(
          platformUserId == null ? 0L : platformUserId, examId, tenantId);
      Object allowed = gray == null ? null : gray.get("allowed");
      grayAllowed = Boolean.TRUE.equals(allowed) || "true".equalsIgnoreCase(String.valueOf(allowed));
    } catch (Exception e) {
      log.warn("[班级同步学] App 灰度预检失败 classId={} examId={}: {}", classId, examId, e.getMessage());
      grayAllowed = false;
    }

    boolean inRoster = false;
    if (platformUserId != null && platformUserId > 0) {
      Long cnt = examPeopleMapper.selectCount(new LambdaQueryWrapper<TrainExamPeopleListEntity>()
          .eq(TrainExamPeopleListEntity::getExamId, examId)
          .eq(TrainExamPeopleListEntity::getPlatformUserId, platformUserId)
          .eq(TrainExamPeopleListEntity::getDeleted, 0));
      inRoster = cnt != null && cnt > 0;
    }

    String reasonCode = null;
    if (!grayAllowed) {
      reasonCode = "GRAY_BLOCKED";
    } else if (!inRoster) {
      reasonCode = "NOT_IN_ROSTER";
    }

    boolean entryVisible = grayAllowed && inRoster;
    out.put("entryVisible", entryVisible);
    out.put("reasonCode", reasonCode);
    out.put("reasonMessage", reasonCode == null ? null : appReasonMessage(reasonCode));
    out.put("grayAllowed", grayAllowed);
    out.put("inRoster", inRoster);

    Map<String, Object> deepLink = new LinkedHashMap<>();
    deepLink.put("path", "/pages/exam/syncHome");
    Map<String, Object> query = new LinkedHashMap<>();
    query.put("exam_id", String.valueOf(examId));
    deepLink.put("query", query);
    out.put("deepLink", deepLink);
    return out;
  }

  private static String appReasonMessage(String reasonCode) {
    if (reasonCode == null) {
      return null;
    }
    return switch (reasonCode) {
      case "NOT_CONFIGURED" -> "本班尚未开通同步学，请联系教务配置";
      case "DISABLED" -> "本班同步学已停用";
      case "WINDOW" -> "当前不在同步学开练时间窗口内";
      case "EXAM_GONE" -> "同步学考试计划已失效，请联系教务";
      case "GRAY_BLOCKED" -> "同步练习正在小范围试点中，敬请期待";
      case "NOT_IN_ROSTER" -> "您尚未加入本班同步学名单，请联系教务开通";
      default -> "暂无法进入同步学";
    };
  }

  // ---------- internal ----------

  private Object lockFor(Long classId) {
    return classLocks.computeIfAbsent(classId, k -> new Object());
  }

  Map<String, Object> tryOpenEnrollmentAndGray(Long classId) {
    return tryOpenEnrollmentAndGray(classId, true);
  }

  Map<String, Object> tryOpenEnrollmentAndGray(Long classId, boolean includeGray) {
    Map<String, Object> out = new LinkedHashMap<>();
    try {
      out.put("enroll", enrollRoster(classId));
    } catch (Exception e) {
      log.warn("[班级同步学] 自动同步名单失败 classId={}: {}", classId, e.getMessage());
      out.put("enrollError", e.getMessage());
    }
    if (includeGray) {
      try {
        out.put("gray", ensureGray(classId));
      } catch (Exception e) {
        log.warn("[班级同步学] 自动加入灰度失败 classId={}: {}", classId, e.getMessage());
        out.put("grayError", e.getMessage());
      }
    }
    return out;
  }

  /**
   * 确保考试计划有复习配置行，使 App 归入「复习任务」并可出现「同步练习」入口。
   * paper_id 与 Apppromote 列表硬编码口径对齐（1）；已存在则幂等跳过。
   * <p>用 JdbcTemplate 直写，兼容合并库 train_exam_review 无 deleted 列。
   *
   * @return true 表示本次新写入
   */
  boolean ensureExamReviewConfig(Long examId) {
    if (examId == null || examId <= 0) {
      return false;
    }
    Long existingId = jdbcTemplate.query(
        "SELECT id FROM train_exam_review WHERE exam_id = ? LIMIT 1",
        (rs, rowNum) -> rs.getLong(1),
        examId).stream().findFirst().orElse(null);
    if (existingId != null && existingId > 0) {
      return false;
    }
    Long tenantId = resolveTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    int rows = jdbcTemplate.update(
        "INSERT INTO train_exam_review (exam_id, paper_id, num, user_id, tenant_id) VALUES (?, ?, ?, ?, ?)",
        examId, 1L, 0, userId, tenantId);
    if (rows > 0) {
      log.info("[班级同步学] 已为 examId={} 写入复习配置，App 将归入复习任务", examId);
      return true;
    }
    return false;
  }

  /**
   * 从指定考试计划中软删本班花名册对应考生（仅本班学员，不影响他班合法名单）。
   */
  int unenrollClassRosterFromExam(Long classId, Long examId) {
    if (classId == null || examId == null || examId <= 0) {
      return 0;
    }
    List<TrainPeopleListEntity> roster = peopleMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0));
    if (roster.isEmpty()) {
      return 0;
    }
    Set<Long> userIds = new HashSet<>();
    Set<String> idCards = new HashSet<>();
    for (TrainPeopleListEntity s : roster) {
      if (s.getPlatformUserId() != null && s.getPlatformUserId() > 0) {
        userIds.add(s.getPlatformUserId());
      }
      if (StringUtils.hasText(s.getIdCard())) {
        String card = s.getIdCard().trim();
        idCards.add(card);
        idCards.add(card.toUpperCase());
      }
    }
    if (userIds.isEmpty() && idCards.isEmpty()) {
      return 0;
    }
    LambdaQueryWrapper<TrainExamPeopleListEntity> q = new LambdaQueryWrapper<TrainExamPeopleListEntity>()
        .eq(TrainExamPeopleListEntity::getExamId, examId)
        .eq(TrainExamPeopleListEntity::getDeleted, 0);
    q.and(w -> {
      boolean first = true;
      if (!userIds.isEmpty()) {
        w.in(TrainExamPeopleListEntity::getPlatformUserId, userIds);
        first = false;
      }
      if (!idCards.isEmpty()) {
        if (!first) {
          w.or();
        }
        w.in(TrainExamPeopleListEntity::getIdCardNum, idCards);
      }
    });
    List<TrainExamPeopleListEntity> hits = examPeopleMapper.selectList(q);
    if (hits.isEmpty()) {
      return 0;
    }
    int removed = 0;
    for (TrainExamPeopleListEntity hit : hits) {
      // TableLogic：deleteById 软删
      removed += examPeopleMapper.deleteById(hit.getId());
    }
    return removed;
  }

  /**
   * 学员移出班级后，从本班已绑定的同步学/复习试卷名单中移除。
   * 同一试卷仍被其他班级的在册学员使用时不移除。
   */
  public int unenrollRemovedStudents(Long classId, List<TrainPeopleListEntity> removed) {
    if (classId == null || classId <= 0 || removed == null || removed.isEmpty()) {
      return 0;
    }
    Long tenantId = resolveTenantId();
    TrainClassReviewSyncEntity cfg = findByClass(tenantId, classId);
    if (cfg == null || cfg.getExamId() == null || cfg.getExamId() <= 0) {
      return 0;
    }
    Long examId = cfg.getExamId();
    List<Long> siblingClassIds = reviewSyncMapper.selectList(new LambdaQueryWrapper<TrainClassReviewSyncEntity>()
            .eq(TrainClassReviewSyncEntity::getExamId, examId)
            .ne(TrainClassReviewSyncEntity::getClassId, classId))
        .stream()
        .map(TrainClassReviewSyncEntity::getClassId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();

    int removedCount = 0;
    for (TrainPeopleListEntity student : removed) {
      if (student == null || (student.getType() != null && !"student".equals(student.getType()))) {
        continue;
      }
      if (stillActiveOnClasses(siblingClassIds, student)) {
        continue;
      }
      removedCount += deleteExamPeopleForStudent(examId, student);
    }
    if (removedCount > 0) {
      log.info("[班级同步学] 学员移出班级后已从复习名单移除 {} 人 classId={} examId={}",
          removedCount, classId, examId);
    }
    return removedCount;
  }

  private boolean stillActiveOnClasses(List<Long> classIds, TrainPeopleListEntity student) {
    if (classIds == null || classIds.isEmpty() || student == null) {
      return false;
    }
    Long platformUserId = student.getPlatformUserId();
    String idCard = StringUtils.hasText(student.getIdCard()) ? student.getIdCard().trim() : null;
    if ((platformUserId == null || platformUserId <= 0) && !StringUtils.hasText(idCard)) {
      return false;
    }
    LambdaQueryWrapper<TrainPeopleListEntity> q = new LambdaQueryWrapper<TrainPeopleListEntity>()
        .in(TrainPeopleListEntity::getTrainClassId, classIds)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0);
    q.and(w -> {
      boolean first = true;
      if (platformUserId != null && platformUserId > 0) {
        w.eq(TrainPeopleListEntity::getPlatformUserId, platformUserId);
        first = false;
      }
      if (StringUtils.hasText(idCard)) {
        if (!first) {
          w.or();
        }
        w.eq(TrainPeopleListEntity::getIdCard, idCard);
      }
    });
    Long cnt = peopleMapper.selectCount(q);
    return cnt != null && cnt > 0;
  }

  private int deleteExamPeopleForStudent(Long examId, TrainPeopleListEntity student) {
    Set<Long> userIds = new HashSet<>();
    Set<String> idCards = new HashSet<>();
    if (student.getPlatformUserId() != null && student.getPlatformUserId() > 0) {
      userIds.add(student.getPlatformUserId());
    }
    if (StringUtils.hasText(student.getIdCard())) {
      String card = student.getIdCard().trim();
      idCards.add(card);
      idCards.add(card.toUpperCase());
    }
    if (userIds.isEmpty() && idCards.isEmpty()) {
      return 0;
    }
    LambdaQueryWrapper<TrainExamPeopleListEntity> q = new LambdaQueryWrapper<TrainExamPeopleListEntity>()
        .eq(TrainExamPeopleListEntity::getExamId, examId)
        .eq(TrainExamPeopleListEntity::getDeleted, 0);
    q.and(w -> {
      boolean first = true;
      if (!userIds.isEmpty()) {
        w.in(TrainExamPeopleListEntity::getPlatformUserId, userIds);
        first = false;
      }
      if (!idCards.isEmpty()) {
        if (!first) {
          w.or();
        }
        w.in(TrainExamPeopleListEntity::getIdCardNum, idCards);
      }
    });
    List<TrainExamPeopleListEntity> hits = examPeopleMapper.selectList(q);
    int removed = 0;
    for (TrainExamPeopleListEntity hit : hits) {
      removed += examPeopleMapper.deleteById(hit.getId());
    }
    return removed;
  }

  /**
   * 本班已绑到正确计划后，清理学员在同培训其它「*-同步学」计划上的错绑残留。
   */
  int unenrollClassRosterFromOtherSyncExams(TrainInfoClassEntity clazz, Long keepExamId) {
    if (clazz == null || clazz.getTrainInfoId() == null || clazz.getTrainInfoId() <= 0
        || keepExamId == null || keepExamId <= 0) {
      return 0;
    }
    List<TrainExamListEntity> syncExams = examListMapper.selectList(new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getTrainId, clazz.getTrainInfoId())
        .eq(TrainExamListEntity::getDeleted, 0)
        .likeLeft(TrainExamListEntity::getTitle, "同步学")
        .ne(TrainExamListEntity::getId, keepExamId)
        .last("LIMIT 200"));
    if (syncExams.isEmpty()) {
      return 0;
    }
    int total = 0;
    for (TrainExamListEntity other : syncExams) {
      total += unenrollClassRosterFromExam(clazz.getId(), other.getId());
    }
    if (total > 0) {
      log.info("[班级同步学] 已从同培训其它同步学计划移出本班考生 {} 人 classId={} keepExamId={}",
          total, clazz.getId(), keepExamId);
    }
    return total;
  }

  private record ExamPick(Long examId, String source) {}

  /**
   * 已绑定且计划仍有效则复用；否则取本培训可匹配计划；再没有则新建。
   * <p>标题以「-同步学」结尾的计划视为班级专属，禁止同工种同等级跨班复用
   * （否则中级1班会绑到中级2班-同步学，学情串班）。
   */
  ExamPick resolveExamForAutoProvision(
      TrainInfoClassEntity clazz,
      TrainClassReviewSyncEntity existing,
      ClassPracticeScopeSaveReq practiceReq) {
    String classLevel = clazz.getWorkTypeLevel() != null ? String.valueOf(clazz.getWorkTypeLevel()) : null;
    String expectedSyncTitle = expectedClassSyncExamTitle(clazz);
    if (existing != null && existing.getExamId() != null && existing.getExamId() > 0) {
      TrainExamListEntity bound = examListMapper.selectOne(new LambdaQueryWrapper<TrainExamListEntity>()
          .eq(TrainExamListEntity::getId, existing.getExamId())
          .eq(TrainExamListEntity::getDeleted, 0)
          .last("LIMIT 1"));
      if (bound != null) {
        String boundTitle = bound.getTitle() == null ? null : bound.getTitle().trim();
        boolean titleOwnsClass = expectedSyncTitle != null && expectedSyncTitle.equals(boundTitle);
        // 他班「*-同步学」即使工种等级相符也视为错绑
        if (isClassScopedSyncExamTitle(boundTitle) && !titleOwnsClass) {
          log.warn("[班级同步学] 本班已绑他班同步学计划 examId={}「{}」，将重新匹配 classId={} expect={}",
              bound.getId(), boundTitle, clazz.getId(), expectedSyncTitle);
        } else {
          List<TrainExamWorkTypeListEntity> bindings = workTypeListMapper.selectList(
              new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
                  .eq(TrainExamWorkTypeListEntity::getExamId, bound.getId())
                  .eq(TrainExamWorkTypeListEntity::getDeleted, 0));
          int boundScore = computeMatchScore(bindings, clazz.getWorkTypeId(), classLevel);
          if (boundScore == 1 && StringUtils.hasText(classLevel) && StringUtils.hasText(bound.getLevel())
              && classLevel.equals(bound.getLevel().trim())) {
            boundScore = 2;
          }
          // 本班专属标题、或非专属计划且工种+等级匹配、或班级无等级
          if (titleOwnsClass || !StringUtils.hasText(classLevel) || boundScore >= 2) {
            return new ExamPick(bound.getId(), "REUSE_BINDING");
          }
          log.warn("[班级同步学] 本班已绑 examId={}「{}」与班级等级不匹配(score={})，将重新匹配或新建 classId={} level={}",
              bound.getId(), bound.getTitle(), boundScore, clazz.getId(), classLevel);
        }
      }
    }
    Long matched = pickBestMatchingExamId(clazz);
    if (matched != null) {
      return new ExamPick(matched, "MATCHED_TRAIN");
    }
    List<Integer> courseIds = practiceReq == null ? List.of() : practiceReq.getCourseIds();
    Long created = examListApplicationService.createForClassReviewSync(clazz, courseIds);
    return new ExamPick(created, "CREATED");
  }

  /** 自动开通使用的规范标题：{班级名}-同步学 */
  static String expectedClassSyncExamTitle(TrainInfoClassEntity clazz) {
    if (clazz == null || !StringUtils.hasText(clazz.getTitle())) {
      return null;
    }
    String title = clazz.getTitle().trim() + "-同步学";
    return title.length() > 255 ? title.substring(0, 255) : title;
  }

  /** 班级通道自动生成的专属同步学计划（禁止跨班按工种复用） */
  static boolean isClassScopedSyncExamTitle(String title) {
    return StringUtils.hasText(title) && title.trim().endsWith("-同步学");
  }

  /**
   * 为本班挑选可复用的同步学考试计划。
   * <ol>
   *   <li>优先标题精确匹配「{班级名}-同步学」</li>
   *   <li>其它「*-同步学」计划一律跳过（防止中级1/2班串班）</li>
   *   <li>非专属计划才按工种+等级匹配；绑定缺等级时可用计划头 level 补齐</li>
   * </ol>
   */
  Long pickBestMatchingExamId(TrainInfoClassEntity clazz) {
    if (clazz == null || clazz.getTrainInfoId() == null || clazz.getTrainInfoId() <= 0) {
      return null;
    }
    List<TrainExamListEntity> exams = examListMapper.selectList(new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getTrainId, clazz.getTrainInfoId())
        .eq(TrainExamListEntity::getDeleted, 0)
        .orderByDesc(TrainExamListEntity::getId)
        .last("LIMIT 200"));
    if (exams.isEmpty()) {
      return null;
    }

    String expectedSyncTitle = expectedClassSyncExamTitle(clazz);
    if (expectedSyncTitle != null) {
      // 同名多条时取最早创建的（通常带历史学情），避免再绑到后来误建的空计划
      TrainExamListEntity titleHit = null;
      for (TrainExamListEntity exam : exams) {
        String title = exam.getTitle() == null ? null : exam.getTitle().trim();
        if (expectedSyncTitle.equals(title)) {
          if (titleHit == null || exam.getId() < titleHit.getId()) {
            titleHit = exam;
          }
        }
      }
      if (titleHit != null) {
        return titleHit.getId();
      }
    }

    Set<Long> examIds = exams.stream().map(TrainExamListEntity::getId).collect(Collectors.toSet());
    Map<Long, List<TrainExamWorkTypeListEntity>> wtMap = batchLoadWorkTypes(examIds);
    Long classWt = clazz.getWorkTypeId();
    String classLevel = clazz.getWorkTypeLevel() != null ? String.valueOf(clazz.getWorkTypeLevel()) : null;
    TrainExamListEntity best = null;
    int bestScore = 0;
    for (TrainExamListEntity exam : exams) {
      String title = exam.getTitle() == null ? null : exam.getTitle().trim();
      // 他班/任意班级专属「*-同步学」不得靠工种等级捞走
      if (isClassScopedSyncExamTitle(title)) {
        continue;
      }
      int score = computeMatchScore(wtMap.getOrDefault(exam.getId(), List.of()), classWt, classLevel);
      // 绑定行只有工种、等级写在计划头上时，补成双匹配
      if (score == 1 && StringUtils.hasText(classLevel) && StringUtils.hasText(exam.getLevel())
          && classLevel.equals(exam.getLevel().trim())) {
        score = 2;
      }
      if (score < 2) {
        continue;
      }
      if (best == null || score > bestScore || (score == bestScore && exam.getId() > best.getId())) {
        best = exam;
        bestScore = score;
      }
    }
    return best == null ? null : best.getId();
  }

  private static String mergeAutoRemark(String existingRemark, String practiceRemark) {
    if (StringUtils.hasText(practiceRemark)) {
      return practiceRemark.trim();
    }
    if (StringUtils.hasText(existingRemark)) {
      return existingRemark.trim();
    }
    return "试题配置自动开通";
  }

  private static String buildAutoProvisionMessage(
      String examSource,
      Map<String, Object> cfg,
      Map<String, Object> enroll,
      String enrollError,
      Map<String, Object> gray,
      String grayError) {
    String examTitle = cfg != null && cfg.get("examTitle") != null
        ? String.valueOf(cfg.get("examTitle"))
        : ("计划#" + (cfg == null ? "" : cfg.get("examId")));
    String bind = switch (examSource == null ? "" : examSource) {
      case "CREATED" -> "已新建同步学计划「" + examTitle + "」并绑定本班";
      case "MATCHED_TRAIN" -> "已绑定本培训同工种同等级计划「" + examTitle + "」";
      default -> "已沿用本班同步学计划「" + examTitle + "」";
    };
    StringBuilder sb = new StringBuilder(bind);
    if (enroll != null) {
      Object inserted = enroll.get("inserted");
      Object existed = enroll.get("existed");
      Object skipped = enroll.get("skippedNoAccount");
      sb.append("；名单新增 ").append(inserted).append("、已在册 ").append(existed)
          .append("、无账号跳过 ").append(skipped);
    } else if (enrollError != null) {
      sb.append("；名单同步失败：").append(enrollError);
    }
    if (gray != null && gray.get("message") != null) {
      sb.append("；").append(gray.get("message"));
    } else if (grayError != null) {
      sb.append("；灰度开通失败：").append(grayError);
    }
    return sb.toString();
  }

  private Map<String, Object> buildConfigView(
      TrainInfoClassEntity clazz, TrainClassReviewSyncEntity cfg, Long tenantId) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", String.valueOf(clazz.getId()));
    out.put("classTitle", clazz.getTitle());
    out.put("workTypeId", clazz.getWorkTypeId());
    out.put("workTypeLevel", clazz.getWorkTypeLevel());
    out.put("configured", cfg != null);

    if (cfg == null) {
      out.put("examId", null);
      out.put("examTitle", null);
      out.put("enabled", false);
      out.put("syncStartTime", null);
      out.put("syncEndTime", null);
      out.put("remark", null);
      out.put("grayEnabled", isGraySwitchOn(tenantId));
      out.put("examInGray", false);
      out.put("roster", emptyRoster());
      out.put("statsPath", null);
      out.put("resolve", resolveNull("NOT_CONFIGURED"));
      return out;
    }

    TrainExamListEntity exam = examListMapper.selectById(cfg.getExamId());
    boolean examAlive = exam != null && (exam.getDeleted() == null || exam.getDeleted() == 0);
    out.put("examId", String.valueOf(cfg.getExamId()));
    out.put("examTitle", examAlive ? exam.getTitle() : null);
    out.put("examGone", !examAlive);
    out.put("enabled", cfg.getEnabled() != null && cfg.getEnabled() == 1);
    out.put("syncStartTime", cfg.getSyncStartTime() == null ? null : cfg.getSyncStartTime().toLocalDate().toString());
    out.put("syncEndTime", cfg.getSyncEndTime() == null ? null : cfg.getSyncEndTime().toLocalDate().toString());
    out.put("remark", cfg.getRemark());

    boolean grayEnabled = isGraySwitchOn(tenantId);
    boolean examInGray = !grayEnabled || isExamInGray(cfg.getExamId());
    out.put("grayEnabled", grayEnabled);
    out.put("examInGray", examInGray);
    out.put("roster", buildRosterGap(clazz.getId(), cfg.getExamId()));
    out.put("statsPath", "/training/exam-lists/" + cfg.getExamId() + "/review-sync-stats");
    out.put("resolve", resolve(clazz.getId(), LocalDateTime.now()));
    return out;
  }

  private Map<String, Object> buildRosterGap(Long classId, Long examId) {
    List<TrainPeopleListEntity> roster = peopleMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0));
    int total = roster.size();
    int withPlatformUser = 0;
    int missingAccount = 0;
    Set<Long> userIds = new HashSet<>();
    for (TrainPeopleListEntity p : roster) {
      if (p.getPlatformUserId() != null && p.getPlatformUserId() > 0) {
        withPlatformUser++;
        userIds.add(p.getPlatformUserId());
      } else {
        missingAccount++;
      }
    }

    int inExamPeople = 0;
    if (!userIds.isEmpty()) {
      Long cnt = examPeopleMapper.selectCount(new LambdaQueryWrapper<TrainExamPeopleListEntity>()
          .eq(TrainExamPeopleListEntity::getExamId, examId)
          .eq(TrainExamPeopleListEntity::getDeleted, 0)
          .in(TrainExamPeopleListEntity::getPlatformUserId, userIds));
      inExamPeople = cnt != null ? cnt.intValue() : 0;
    }
    int missingEnroll = Math.max(0, withPlatformUser - inExamPeople);

    Map<String, Object> gap = new LinkedHashMap<>();
    gap.put("total", total);
    gap.put("withPlatformUser", withPlatformUser);
    gap.put("inExamPeople", inExamPeople);
    gap.put("missingAccount", missingAccount);
    gap.put("missingEnroll", missingEnroll);
    return gap;
  }

  private Map<String, Object> emptyRoster() {
    Map<String, Object> roster = new LinkedHashMap<>();
    roster.put("total", 0);
    roster.put("withPlatformUser", 0);
    roster.put("inExamPeople", 0);
    roster.put("missingAccount", 0);
    roster.put("missingEnroll", 0);
    return roster;
  }

  private Map<String, Object> resolveNull(String reasonCode) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("configured", false);
    out.put("examId", null);
    out.put("examTitle", null);
    out.put("enabled", false);
    out.put("reasonCode", reasonCode);
    return out;
  }

  private void applySaveFields(TrainClassReviewSyncEntity row, ClassReviewSyncSaveReq req, LocalDateTime now) {
    row.setExamId(req.getExamId());
    row.setEnabled(req.getEnabled() != null && req.getEnabled() == 0 ? 0 : 1);
    row.setSyncStartTime(toDayStart(req.getSyncStartTime()));
    row.setSyncEndTime(toDayEnd(req.getSyncEndTime()));
    row.setRemark(StringUtils.hasText(req.getRemark()) ? req.getRemark().trim() : null);
    if (row.getAutoEnroll() == null) {
      row.setAutoEnroll(0);
    }
    row.setUpdateTime(now);
  }

  private LocalDateTime toDayStart(LocalDate d) {
    return d == null ? null : d.atStartOfDay();
  }

  private LocalDateTime toDayEnd(LocalDate d) {
    return d == null ? null : d.atTime(LocalTime.of(23, 59, 59));
  }

  private TrainClassReviewSyncEntity findByClass(Long tenantId, Long classId) {
    return reviewSyncMapper.selectOne(new LambdaQueryWrapper<TrainClassReviewSyncEntity>()
        .eq(TrainClassReviewSyncEntity::getTenantId, tenantId)
        .eq(TrainClassReviewSyncEntity::getClassId, classId)
        .last("LIMIT 1"));
  }

  private TrainClassReviewSyncEntity requireConfigured(Long tenantId, Long classId) {
    TrainClassReviewSyncEntity cfg = findByClass(tenantId, classId);
    if (cfg == null || cfg.getExamId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先保存同步学考试计划绑定");
    }
    return cfg;
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

  private TrainExamListEntity requireExam(Long examId) {
    TrainExamListEntity exam = examListMapper.selectOne(new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getId, examId)
        .eq(TrainExamListEntity::getDeleted, 0));
    if (exam == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考试计划不存在或已删除");
    }
    return exam;
  }

  private void warnIfWorkTypeMismatch(TrainInfoClassEntity clazz, TrainExamListEntity exam) {
    if (clazz.getWorkTypeId() == null || clazz.getWorkTypeId() <= 0) {
      return;
    }
    List<TrainExamWorkTypeListEntity> bindings = workTypeListMapper.selectList(
        new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
            .eq(TrainExamWorkTypeListEntity::getExamId, exam.getId())
            .eq(TrainExamWorkTypeListEntity::getDeleted, 0));
    if (bindings.isEmpty()) {
      log.warn("[班级同步学] exam={} 无工种绑定，班级 classId={} workTypeId={}",
          exam.getId(), clazz.getId(), clazz.getWorkTypeId());
      return;
    }
    int score = computeMatchScore(bindings, clazz.getWorkTypeId(),
        clazz.getWorkTypeLevel() != null ? String.valueOf(clazz.getWorkTypeLevel()) : null);
    if (score < 1) {
      log.warn("[班级同步学] exam={} 与班级 classId={} 工种不匹配（仍允许保存）",
          exam.getId(), clazz.getId());
    }
  }

  private int computeMatchScore(
      List<TrainExamWorkTypeListEntity> bindings, Long classWt, String classLevel) {
    if (classWt == null || classWt <= 0 || bindings == null || bindings.isEmpty()) {
      return 0;
    }
    int best = 0;
    for (TrainExamWorkTypeListEntity b : bindings) {
      if (!Objects.equals(b.getWorkTypeId(), classWt)) {
        continue;
      }
      best = Math.max(best, 1);
      if (StringUtils.hasText(classLevel) && StringUtils.hasText(b.getLevel())
          && classLevel.equals(b.getLevel().trim())) {
        best = Math.max(best, 2);
      }
    }
    return best;
  }

  private Map<Long, List<TrainExamWorkTypeListEntity>> batchLoadWorkTypes(Set<Long> examIds) {
    if (examIds == null || examIds.isEmpty()) {
      return Map.of();
    }
    List<TrainExamWorkTypeListEntity> rows = workTypeListMapper.selectList(
        new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
            .in(TrainExamWorkTypeListEntity::getExamId, examIds)
            .eq(TrainExamWorkTypeListEntity::getDeleted, 0));
    return rows.stream().collect(Collectors.groupingBy(TrainExamWorkTypeListEntity::getExamId));
  }

  private boolean isGraySwitchOn(Long tenantId) {
    try {
      List<DictEntity> dicts = dictMapper.selectList(new LambdaQueryWrapper<DictEntity>()
          .eq(DictEntity::getCode, "ai_review_gray_enabled")
          .eq(DictEntity::getDeleted, 0)
          .last("LIMIT 1"));
      if (!dicts.isEmpty() && dicts.get(0).getValue() != null) {
        String v = dicts.get(0).getValue().trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v);
      }
    } catch (Exception e) {
      log.warn("[班级同步学] 读取灰度开关失败：{}", e.getMessage());
    }
    return false;
  }

  private boolean isExamInGray(Long examId) {
    Long count = grayWhitelistMapper.selectCount(new LambdaQueryWrapper<AiReviewGrayWhitelistEntity>()
        .eq(AiReviewGrayWhitelistEntity::getScopeType, "exam")
        .eq(AiReviewGrayWhitelistEntity::getScopeId, examId));
    return count != null && count > 0;
  }

  private Long resolveTenantId() {
    try {
      Long fromSecurity = SecurityContext.currentUser().getTenantId();
      if (fromSecurity != null && fromSecurity > 0) {
        return fromSecurity;
      }
    } catch (Exception ignored) {
      // App principal 可能无 sys tenant
    }
    Long fromHolder = TenantContextHolder.getTenantId();
    if (fromHolder != null && fromHolder > 0) {
      return fromHolder;
    }
    return TenantContextHolder.DEFAULT_TENANT_ID;
  }
}
