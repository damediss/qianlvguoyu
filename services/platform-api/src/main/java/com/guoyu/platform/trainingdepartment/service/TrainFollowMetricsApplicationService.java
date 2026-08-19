package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.trainingdepartment.entity.TrainAttendanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowIssueEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowPhotoPackEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowSurveyResponseEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyPackSlotEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidySlotFileEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainAttendanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowIssueMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowPhotoPackMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowSurveyResponseMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidySlotFileMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainFollowMetricsApplicationService {

  private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final Set<String> OPEN_ISSUE_STATUSES = Set.of("open", "doing");
  private static final Set<String> CHECKLIST_READY_STATUSES = Set.of("ready", "ok", "done", "true");

  private final TrainFollowTaskApplicationService taskService;
  private final TrainFollowEvidenceApplicationService evidenceService;
  private final TrainSubsidyPackProfileApplicationService subsidyProfileService;
  private final TrainAttendanceMapper attendanceMapper;
  private final TrainExamListMapper examListMapper;
  private final TrainExamPeopleListMapper examPeopleMapper;
  private final TrainSubsidyMapper subsidyMapper;
  private final TrainSubsidySlotFileMapper subsidySlotFileMapper;
  private final TrainFollowPhotoPackMapper photoPackMapper;
  private final TrainFollowSurveyResponseMapper surveyMapper;
  private final TrainFollowIssueMapper issueMapper;
  private final TrainInfoClassMapper classMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final ObjectMapper objectMapper;

  public TrainFollowMetricsApplicationService(
      TrainFollowTaskApplicationService taskService,
      TrainFollowEvidenceApplicationService evidenceService,
      TrainSubsidyPackProfileApplicationService subsidyProfileService,
      TrainAttendanceMapper attendanceMapper,
      TrainExamListMapper examListMapper,
      TrainExamPeopleListMapper examPeopleMapper,
      TrainSubsidyMapper subsidyMapper,
      TrainSubsidySlotFileMapper subsidySlotFileMapper,
      TrainFollowPhotoPackMapper photoPackMapper,
      TrainFollowSurveyResponseMapper surveyMapper,
      TrainFollowIssueMapper issueMapper,
      TrainInfoClassMapper classMapper,
      TrainPeopleListMapper peopleListMapper,
      ObjectMapper objectMapper) {
    this.taskService = taskService;
    this.evidenceService = evidenceService;
    this.subsidyProfileService = subsidyProfileService;
    this.attendanceMapper = attendanceMapper;
    this.examListMapper = examListMapper;
    this.examPeopleMapper = examPeopleMapper;
    this.subsidyMapper = subsidyMapper;
    this.subsidySlotFileMapper = subsidySlotFileMapper;
    this.photoPackMapper = photoPackMapper;
    this.surveyMapper = surveyMapper;
    this.issueMapper = issueMapper;
    this.classMapper = classMapper;
    this.peopleListMapper = peopleListMapper;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> classQualityMetrics(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = tenantId();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);

    BigDecimal attendanceDayRate = computeAttendanceDayRate(classId, tenantId);
    BigDecimal attendanceCumulativeRate = computeAttendanceCumulativeRate(classId, tenantId);
    result.put("attendanceDayRate", attendanceDayRate);
    result.put("attendanceCumulativeRate", attendanceCumulativeRate);

    Map<String, Object> homework = taskService.homeworkMetrics(classId);
    BigDecimal homeworkCompletionRate = toBigDecimal(homework.get("completionRate"));
    result.put("homeworkCompletionRate", homeworkCompletionRate);

    Map<String, Object> mock = taskService.mockMetrics(classId);
    result.put("mockPassRate", computeMockPassRate(mock));

    result.put("examPassRate", computeExamPassRate(clazz, tenantId));

    List<TrainFollowSurveyResponseEntity> surveys = surveyMapper.selectList(
        new LambdaQueryWrapper<TrainFollowSurveyResponseEntity>()
            .eq(TrainFollowSurveyResponseEntity::getTenantId, tenantId)
            .eq(TrainFollowSurveyResponseEntity::getClassId, classId)
            .eq(TrainFollowSurveyResponseEntity::getDeleted, 0));
    result.put("satisfactionCount", surveys.size());
    result.put("satisfactionAvg", computeSurveyAvg(surveys));

    Map<String, Object> coverage = evidenceService.coverage(classId);
    result.put("evidenceReady", coverage.get("ready"));
    result.put("evidenceOkSessions", coverage.get("okSessions"));
    result.put("evidenceRequiredSessions", coverage.get("requiredSessions"));
    result.put("evidencePhotoCount", coverage.get("photoApprovedCount"));
    result.put("evidenceMode", coverage.get("mode"));

    long openIssueCount = issueMapper.selectCount(openIssueQuery(classId, tenantId, null));
    long criticalIssueCount = issueMapper.selectCount(openIssueQuery(classId, tenantId, "critical"));
    result.put("openIssueCount", openIssueCount);
    result.put("criticalIssueCount", criticalIssueCount);

    appendSubsidyPack(result, classId, tenantId);
    result.put("alerts", buildAlerts(
        attendanceCumulativeRate,
        homeworkCompletionRate,
        homework,
        surveys,
        coverage,
        criticalIssueCount,
        result));

    return result;
  }

  private void appendSubsidyPack(Map<String, Object> result, Long classId, Long tenantId) {
    TrainSubsidyEntity subsidy = subsidyMapper.selectOne(
        new LambdaQueryWrapper<TrainSubsidyEntity>()
            .eq(TrainSubsidyEntity::getTenantId, tenantId)
            .eq(TrainSubsidyEntity::getClassId, classId)
            .eq(TrainSubsidyEntity::getDeleted, 0)
            .orderByDesc(TrainSubsidyEntity::getUpdateTime)
            .orderByDesc(TrainSubsidyEntity::getId)
            .last("LIMIT 1"));
    if (subsidy == null) {
      result.put("subsidyId", null);
      result.put("subsidyReadySlots", null);
      result.put("subsidyTotalSlots", null);
      result.put("subsidyReady", null);
      result.put("subsidyMaterialStatus", null);
      return;
    }
    int[] counts = computeSubsidyReadyLive(subsidy, tenantId);
    if (counts[1] == 0) {
      counts = countSubsidyChecklist(subsidy.getChecklistJson());
    }
    int ready = counts[0];
    int total = counts[1];
    boolean readyFlag = total > 0 && ready == total;
    if (subsidy.getMaterialStatus() != null && subsidy.getMaterialStatus() == 1) {
      readyFlag = true;
    }
    result.put("subsidyId", subsidy.getId());
    result.put("subsidyReadySlots", ready);
    result.put("subsidyTotalSlots", total);
    result.put("subsidyReady", readyFlag);
    result.put("subsidyMaterialStatus", subsidy.getMaterialStatus());
  }

  /**
   * 补贴齐套口径：配方必填槽位均已有材料（跟班照片包或手工上传）。
   * 与 {@link TrainSubsidyMaterialPackService#generate} 判定一致。
   */
  private int[] computeSubsidyReadyLive(TrainSubsidyEntity subsidy, Long tenantId) {
    if (subsidy.getProfileId() == null) {
      return new int[] {0, 0};
    }
    List<TrainSubsidyPackSlotEntity> slots = subsidyProfileService.listSlots(subsidy.getProfileId());
    if (slots.isEmpty()) {
      return new int[] {0, 0};
    }
    Map<String, List<TrainSubsidySlotFileEntity>> filesBySlot = new LinkedHashMap<>();
    subsidySlotFileMapper.selectList(new LambdaQueryWrapper<TrainSubsidySlotFileEntity>()
            .eq(TrainSubsidySlotFileEntity::getSubsidyId, subsidy.getId())
            .eq(TrainSubsidySlotFileEntity::getDeleted, 0))
        .forEach(f -> filesBySlot.computeIfAbsent(f.getSlotCode(), k -> new ArrayList<>()).add(f));

    TrainFollowPhotoPackEntity followPack = null;
    if (subsidy.getClassId() != null) {
      followPack = photoPackMapper.selectOne(new LambdaQueryWrapper<TrainFollowPhotoPackEntity>()
          .eq(TrainFollowPhotoPackEntity::getClassId, subsidy.getClassId())
          .eq(TrainFollowPhotoPackEntity::getTenantId, tenantId)
          .eq(TrainFollowPhotoPackEntity::getDeleted, 0)
          .orderByDesc(TrainFollowPhotoPackEntity::getGeneratedAt)
          .last("LIMIT 1"));
    }

    int total = 0;
    int ready = 0;
    for (TrainSubsidyPackSlotEntity slot : slots) {
      boolean required = slot.getRequiredFlag() != null && slot.getRequiredFlag() == 1;
      if (!required) {
        continue;
      }
      total++;
      boolean ok = false;
      if ("follow_photo".equals(slot.getRenderer())) {
        if (followPack != null && followPack.getFileId() != null) {
          ok = true;
        } else if (filesBySlot.containsKey(slot.getSlotCode())
            && !filesBySlot.get(slot.getSlotCode()).isEmpty()) {
          ok = true;
        }
      } else {
        ok = filesBySlot.containsKey(slot.getSlotCode())
            && !filesBySlot.get(slot.getSlotCode()).isEmpty();
      }
      if (ok) {
        ready++;
      }
    }
    return new int[] {ready, total};
  }

  private List<Map<String, Object>> buildAlerts(
      BigDecimal attendanceCumulativeRate,
      BigDecimal homeworkCompletionRate,
      Map<String, Object> homework,
      List<TrainFollowSurveyResponseEntity> surveys,
      Map<String, Object> coverage,
      long criticalIssueCount,
      Map<String, Object> result) {
    List<Map<String, Object>> alerts = new ArrayList<>();

    if (attendanceCumulativeRate != null
        && attendanceCumulativeRate.compareTo(BigDecimal.valueOf(80)) < 0) {
      alerts.add(alert("warn", "attendance_low",
          "累计出勤率 " + attendanceCumulativeRate + "%，低于 80%"));
    }
    if (homeworkCompletionRate != null
        && toInt(homework.get("taskCount")) > 0
        && homeworkCompletionRate.compareTo(BigDecimal.valueOf(80)) < 0) {
      alerts.add(alert("warn", "homework_low",
          "作业完成率 " + homeworkCompletionRate + "%，低于 80%"));
    }

    Object warningsObj = homework.get("warnings");
    if (warningsObj instanceof List<?> warnings) {
      int added = 0;
      for (Object w : warnings) {
        if (!(w instanceof Map<?, ?> map)) {
          continue;
        }
        Object type = map.get("type");
        if (!"miss".equals(type) && !"low_accuracy".equals(type)) {
          continue;
        }
        String message = map.get("message") != null ? String.valueOf(map.get("message")) : "作业预警";
        alerts.add(alert("warn", "homework_" + type, message));
        added++;
        if (added >= 5) {
          break;
        }
      }
    }

    BigDecimal satisfactionAvg = computeSurveyAvg(surveys);
    if (!surveys.isEmpty()
        && satisfactionAvg != null
        && satisfactionAvg.compareTo(BigDecimal.valueOf(3.5)) < 0) {
      alerts.add(alert("warn", "satisfaction_low",
          "平均满意度 " + satisfactionAvg + "，低于 3.5"));
    }

    Object evidenceReady = coverage.get("ready");
    boolean ready = evidenceReady instanceof Boolean b && b;
    if (!ready) {
      Object photoCount = coverage.get("photoApprovedCount");
      alerts.add(alert("warn", "evidence_incomplete",
          photoCount != null
              ? "课堂影像尚未添加（当前已通过照片 " + photoCount + " 张）"
              : "课堂影像留痕未齐套，请在「留痕」中手动添加照片"));
    }

    if (criticalIssueCount > 0) {
      alerts.add(alert("critical", "critical_issue",
          "存在 " + criticalIssueCount + " 条未关闭的严重整改项"));
    }

    Object subsidyReady = result.get("subsidyReady");
    Object subsidyId = result.get("subsidyId");
    if (subsidyId != null && Boolean.FALSE.equals(subsidyReady)) {
      Object readySlots = result.get("subsidyReadySlots");
      Object totalSlots = result.get("subsidyTotalSlots");
      alerts.add(alert("warn", "subsidy_not_ready",
          "补贴材料包未齐套（" + readySlots + "/" + totalSlots + "）"));
    }

    return alerts;
  }

  private static Map<String, Object> alert(String level, String code, String message) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("level", level);
    row.put("code", code);
    row.put("message", message);
    return row;
  }

  private BigDecimal computeAttendanceDayRate(Long classId, Long tenantId) {
    String today = LocalDate.now().format(DAY);
    List<TrainAttendanceEntity> rows = attendanceMapper.selectList(
        new LambdaQueryWrapper<TrainAttendanceEntity>()
            .eq(TrainAttendanceEntity::getTenantId, tenantId)
            .eq(TrainAttendanceEntity::getClassId, classId)
            .eq(TrainAttendanceEntity::getAttendanceDate, today)
            .eq(TrainAttendanceEntity::getDeleted, 0));
    if (rows.isEmpty()) {
      return null;
    }
    // 按学员折叠：优先按日记录；否则同日多课次「任一有效出勤即出勤」
    return ratePresentByUser(collapseToUserDayStatus(rows));
  }

  private BigDecimal computeAttendanceCumulativeRate(Long classId, Long tenantId) {
    List<TrainAttendanceEntity> rows = attendanceMapper.selectList(
        new LambdaQueryWrapper<TrainAttendanceEntity>()
            .eq(TrainAttendanceEntity::getTenantId, tenantId)
            .eq(TrainAttendanceEntity::getClassId, classId)
            .eq(TrainAttendanceEntity::getDeleted, 0));
    if (rows.isEmpty()) {
      return null;
    }
    // 课次级：每条 (学员×日×课次/按日) 计一次，避免同日多课次被错误摊薄或放大
    return ratePresent(rows);
  }

  private static BigDecimal ratePresent(List<TrainAttendanceEntity> rows) {
    int total = rows.size();
    int present = 0;
    for (TrainAttendanceEntity row : rows) {
      if (isPresent(row.getStatus())) {
        present++;
      }
    }
    return BigDecimal.valueOf(present * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
  }

  private static BigDecimal ratePresentByUser(Map<Long, Integer> statusByUser) {
    if (statusByUser.isEmpty()) {
      return null;
    }
    int present = 0;
    for (Integer status : statusByUser.values()) {
      if (isPresent(status)) {
        present++;
      }
    }
    return BigDecimal.valueOf(present * 100.0 / statusByUser.size()).setScale(1, RoundingMode.HALF_UP);
  }

  /**
   * 同日多记录折叠为学员级状态：有 schedule_id 为空的按日记录则用之；
   * 否则多课次合并为「有一次有效出勤即出勤，否则取最严重缺勤态」。
   */
  private static Map<Long, Integer> collapseToUserDayStatus(List<TrainAttendanceEntity> rows) {
    Map<Long, Integer> dayLevel = new HashMap<>();
    Map<Long, Integer> sessionMerged = new HashMap<>();
    for (TrainAttendanceEntity row : rows) {
      Long uid = row.getUserId();
      if (uid == null) {
        continue;
      }
      if (row.getScheduleId() == null) {
        dayLevel.put(uid, row.getStatus());
      } else {
        sessionMerged.put(uid, mergeSessionStatus(sessionMerged.get(uid), row.getStatus()));
      }
    }
    Map<Long, Integer> result = new HashMap<>(sessionMerged);
    result.putAll(dayLevel);
    return result;
  }

  /** 课次合并：有效出勤优先；否则保留缺勤(0)。 */
  private static Integer mergeSessionStatus(Integer current, Integer incoming) {
    if (isPresent(incoming)) {
      return incoming;
    }
    if (isPresent(current)) {
      return current;
    }
    if (incoming != null) {
      return incoming;
    }
    return current;
  }

  private static boolean isPresent(Integer status) {
    return status != null && (status == 1 || status == 2 || status == 3);
  }

  private BigDecimal computeMockPassRate(Map<String, Object> mock) {
    int gradedItems = toInt(mock.get("gradedItems"));
    if (gradedItems == 0) {
      return null;
    }
    int passedItems = toInt(mock.get("passedItems"));
    return BigDecimal.valueOf(passedItems * 100.0 / gradedItems).setScale(1, RoundingMode.HALF_UP);
  }

  private BigDecimal computeExamPassRate(TrainInfoClassEntity clazz, Long tenantId) {
    if (clazz.getTrainInfoId() != null) {
      List<TrainExamListEntity> exams = examListMapper.selectList(
          new LambdaQueryWrapper<TrainExamListEntity>()
              .eq(TrainExamListEntity::getTrainId, clazz.getTrainInfoId())
              .eq(TrainExamListEntity::getDeleted, 0));
      if (!exams.isEmpty()) {
        List<Long> examIds = exams.stream().map(TrainExamListEntity::getId).toList();
        List<TrainExamPeopleListEntity> people = examPeopleMapper.selectList(
            new LambdaQueryWrapper<TrainExamPeopleListEntity>()
                .in(TrainExamPeopleListEntity::getExamId, examIds)
                .isNotNull(TrainExamPeopleListEntity::getScore)
                .eq(TrainExamPeopleListEntity::getDeleted, 0));
        if (!people.isEmpty()) {
          int passed = 0;
          for (TrainExamPeopleListEntity person : people) {
            if (isExamPass(person)) {
              passed++;
            }
          }
          return BigDecimal.valueOf(passed * 100.0 / people.size()).setScale(1, RoundingMode.HALF_UP);
        }
      }
    }
    return computeRosterExamPassRate(clazz.getId(), tenantId);
  }

  private BigDecimal computeRosterExamPassRate(Long classId, Long tenantId) {
    List<TrainPeopleListEntity> students = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0)
            .isNotNull(TrainPeopleListEntity::getExamScore));
    if (tenantId != null) {
      students = students.stream()
          .filter(s -> s.getTenantId() == null || tenantId.equals(s.getTenantId()))
          .toList();
    }
    if (students.isEmpty()) {
      return null;
    }
    int passed = 0;
    for (TrainPeopleListEntity student : students) {
      if (student.getExamScore() != null
          && student.getExamScore().compareTo(BigDecimal.valueOf(60)) >= 0) {
        passed++;
      }
    }
    return BigDecimal.valueOf(passed * 100.0 / students.size()).setScale(1, RoundingMode.HALF_UP);
  }

  private static boolean isExamPass(TrainExamPeopleListEntity person) {
    BigDecimal score = person.getScore();
    if (score == null) {
      return false;
    }
    BigDecimal totalScore = person.getTotalScore();
    if (totalScore != null && totalScore.compareTo(BigDecimal.ZERO) > 0) {
      return score.divide(totalScore, 4, RoundingMode.HALF_UP)
          .compareTo(BigDecimal.valueOf(0.6)) >= 0;
    }
    return score.compareTo(BigDecimal.valueOf(60)) >= 0;
  }

  private static BigDecimal computeSurveyAvg(List<TrainFollowSurveyResponseEntity> surveys) {
    if (surveys.isEmpty()) {
      return null;
    }
    BigDecimal sum = BigDecimal.ZERO;
    for (TrainFollowSurveyResponseEntity survey : surveys) {
      if (survey.getScore() != null) {
        sum = sum.add(survey.getScore());
      }
    }
    return sum.divide(BigDecimal.valueOf(surveys.size()), 2, RoundingMode.HALF_UP);
  }

  private int[] countSubsidyChecklist(String checklistJson) {
    if (!StringUtils.hasText(checklistJson)) {
      return new int[] {0, 0};
    }
    try {
      List<Map<String, Object>> items = objectMapper.readValue(
          checklistJson, new TypeReference<List<Map<String, Object>>>() {});
      boolean hasRequiredFlag = items.stream().anyMatch(i -> i.containsKey("required"));
      int total = 0;
      int ready = 0;
      for (Map<String, Object> item : items) {
        if (hasRequiredFlag && !isRequiredSlot(item)) {
          continue;
        }
        total++;
        if (isChecklistItemReady(item)) {
          ready++;
        }
      }
      return new int[] {ready, total};
    } catch (Exception ex) {
      return new int[] {0, 0};
    }
  }

  private static boolean isRequiredSlot(Map<String, Object> item) {
    Object required = item.get("required");
    if (required instanceof Boolean b) {
      return b;
    }
    if (required instanceof Number n) {
      return n.intValue() != 0;
    }
    return true;
  }

  private static boolean isChecklistItemReady(Map<String, Object> item) {
    if (item == null || item.isEmpty()) {
      return false;
    }
    Object ok = item.get("ok");
    if (ok instanceof Boolean b && b) {
      return true;
    }
    if (ok instanceof Number n && n.intValue() != 0) {
      return true;
    }
    Object filled = item.get("filled");
    if (filled instanceof Boolean b && b) {
      return true;
    }
    if (filled instanceof Number n && n.intValue() != 0) {
      return true;
    }
    Object status = item.get("status");
    if (status != null) {
      return CHECKLIST_READY_STATUSES.contains(String.valueOf(status).trim().toLowerCase(Locale.ROOT));
    }
    return false;
  }

  private LambdaQueryWrapper<TrainFollowIssueEntity> openIssueQuery(
      Long classId, Long tenantId, String severity) {
    LambdaQueryWrapper<TrainFollowIssueEntity> q = new LambdaQueryWrapper<TrainFollowIssueEntity>()
        .eq(TrainFollowIssueEntity::getTenantId, tenantId)
        .eq(TrainFollowIssueEntity::getClassId, classId)
        .in(TrainFollowIssueEntity::getStatus, OPEN_ISSUE_STATUSES)
        .eq(TrainFollowIssueEntity::getDeleted, 0);
    if (StringUtils.hasText(severity)) {
      q.eq(TrainFollowIssueEntity::getSeverity, severity);
    }
    return q;
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity clazz = classMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    return clazz;
  }

  private Long tenantId() {
    Long t = TenantContextHolder.getTenantId();
    if (t == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少租户");
    }
    return t;
  }

  private static BigDecimal toBigDecimal(Object value) {
    if (value instanceof BigDecimal bd) {
      return bd;
    }
    if (value instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    return null;
  }

  private static int toInt(Object value) {
    if (value instanceof Number n) {
      return n.intValue();
    }
    return 0;
  }
}
