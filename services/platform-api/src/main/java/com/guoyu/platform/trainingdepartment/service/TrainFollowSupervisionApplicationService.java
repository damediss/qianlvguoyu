package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.FollowIssueCreateReq;
import com.guoyu.platform.trainingdepartment.dto.FollowIssueTransitionReq;
import com.guoyu.platform.trainingdepartment.dto.FollowSupervisionCreateReq;
import com.guoyu.platform.trainingdepartment.dto.FollowSupervisionItemPatchReq;
import com.guoyu.platform.trainingdepartment.dto.FollowSupervisionSubmitReq;
import com.guoyu.platform.trainingdepartment.dto.FollowSurveyCreateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowIssueEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowSupervisionEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowSupervisionItemEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowSurveyResponseEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.follow.FollowOpEventCode;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogCommand;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogWriter;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowIssueMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowSupervisionItemMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowSupervisionMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowSurveyResponseMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.vo.FollowIssueVO;
import com.guoyu.platform.trainingdepartment.vo.FollowSupervisionItemVO;
import com.guoyu.platform.trainingdepartment.vo.FollowSupervisionVO;
import com.guoyu.platform.trainingdepartment.vo.FollowSurveyResponseVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class TrainFollowSupervisionApplicationService {

  private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

  private static final String STATUS_DRAFT = "draft";
  private static final String STATUS_SUBMITTED = "submitted";
  private static final String STATUS_CLOSED = "closed";

  private static final Set<String> ITEM_RESULTS = Set.of("pass", "fail", "na");
  private static final Set<String> CONCLUSIONS = Set.of("pass", "fail", "conditional");
  private static final Set<String> ISSUE_STATUSES = Set.of("open", "doing", "done", "closed");
  private static final Set<String> ISSUE_SEVERITIES = Set.of("info", "warn", "critical");
  private static final Set<String> OPEN_ISSUE_STATUSES = Set.of("open", "doing");

  private static final Map<String, Set<String>> ISSUE_TRANSITIONS = Map.of(
      "open", Set.of("doing", "closed"),
      "doing", Set.of("done"),
      "done", Set.of("closed"),
      "closed", Set.of());

  private static final List<String[]> DEFAULT_CHECKLIST = List.of(
      new String[] {"equipment", "设备齐全可用"},
      new String[] {"discipline", "课堂纪律良好"},
      new String[] {"attendance", "到课人数达标"},
      new String[] {"evidence", "影像留痕齐套"},
      new String[] {"attendance_done", "考勤已落实"},
      new String[] {"homework", "作业布置与督促"});

  private final TrainFollowSupervisionMapper supervisionMapper;
  private final TrainFollowSupervisionItemMapper itemMapper;
  private final TrainFollowIssueMapper issueMapper;
  private final TrainFollowSurveyResponseMapper surveyMapper;
  private final TrainInfoClassMapper classMapper;
  private final FollowOpLogWriter opLogWriter;

  public TrainFollowSupervisionApplicationService(
      TrainFollowSupervisionMapper supervisionMapper,
      TrainFollowSupervisionItemMapper itemMapper,
      TrainFollowIssueMapper issueMapper,
      TrainFollowSurveyResponseMapper surveyMapper,
      TrainInfoClassMapper classMapper,
      FollowOpLogWriter opLogWriter) {
    this.supervisionMapper = supervisionMapper;
    this.itemMapper = itemMapper;
    this.issueMapper = issueMapper;
    this.surveyMapper = surveyMapper;
    this.classMapper = classMapper;
    this.opLogWriter = opLogWriter;
  }

  @Transactional(readOnly = true)
  public List<FollowSupervisionVO> listSupervisions(Long classId, String bizDate) {
    requireClass(classId);
    LambdaQueryWrapper<TrainFollowSupervisionEntity> q = supervisionQuery()
        .eq(TrainFollowSupervisionEntity::getClassId, classId)
        .orderByDesc(TrainFollowSupervisionEntity::getBizDate)
        .orderByDesc(TrainFollowSupervisionEntity::getId);
    if (StringUtils.hasText(bizDate)) {
      q.eq(TrainFollowSupervisionEntity::getBizDate, bizDate.trim());
    }
    return supervisionMapper.selectList(q).stream()
        .map(e -> toSupervisionVo(e, false))
        .toList();
  }

  @Transactional(readOnly = true)
  public FollowSupervisionVO getSupervision(Long id) {
    TrainFollowSupervisionEntity entity = requireSupervision(id);
    return toSupervisionVo(entity, true);
  }

  @Transactional
  public FollowSupervisionVO create(Long classId, FollowSupervisionCreateReq req) {
    requireClass(classId);
    PlatformUserPrincipal user = SecurityContext.currentUser();
    Long userId = user.getUserId();

    TrainFollowSupervisionEntity entity = new TrainFollowSupervisionEntity();
    entity.setTenantId(tenantId());
    entity.setClassId(classId);
    entity.setBizDate(req.getBizDate().trim());
    entity.setScheduleId(req.getScheduleId());
    entity.setInspectorUserId(userId);
    entity.setInspectorName(resolveInspectorName(req.getInspectorName(), user));
    entity.setStatus(STATUS_DRAFT);
    entity.setRemark(trimToNull(req.getRemark()));
    entity.setCreatedBy(userId);
    entity.setUpdatedBy(userId);
    entity.setDeleted(0);
    supervisionMapper.insert(entity);

    int sort = 1;
    for (String[] row : DEFAULT_CHECKLIST) {
      TrainFollowSupervisionItemEntity item = new TrainFollowSupervisionItemEntity();
      item.setTenantId(entity.getTenantId());
      item.setSupervisionId(entity.getId());
      item.setItemCode(row[0]);
      item.setItemTitle(row[1]);
      item.setSortNo(sort++);
      item.setCreatedBy(userId);
      item.setUpdatedBy(userId);
      item.setDeleted(0);
      itemMapper.insert(item);
    }

    return getSupervision(entity.getId());
  }

  @Transactional
  public FollowSupervisionItemVO patchItem(Long itemId, FollowSupervisionItemPatchReq req) {
    TrainFollowSupervisionItemEntity item = requireItem(itemId);
    TrainFollowSupervisionEntity supervision = requireSupervision(item.getSupervisionId());
    assertDraft(supervision);

    PlatformUserPrincipal user = SecurityContext.currentUser();
    if (req.getResult() != null) {
      item.setResult(normalizeItemResult(req.getResult()));
    }
    if (req.getRemark() != null) {
      item.setRemark(trimToNull(req.getRemark()));
    }
    item.setUpdatedBy(user.getUserId());
    itemMapper.updateById(item);
    return toItemVo(item);
  }

  @Transactional
  public FollowSupervisionVO submit(Long id, FollowSupervisionSubmitReq req) {
    TrainFollowSupervisionEntity entity = requireSupervision(id);
    assertDraft(entity);
    PlatformUserPrincipal user = SecurityContext.currentUser();

    List<TrainFollowSupervisionItemEntity> items = loadItems(entity.getId());
    long unset = items.stream().filter(i -> !StringUtils.hasText(i.getResult())).count();
    if (unset > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先完成全部检查项再提交（尚有 " + unset + " 项未填）");
    }

    entity.setStatus(STATUS_SUBMITTED);
    entity.setConclusion(normalizeConclusion(req.getConclusion()));
    if (req.getRemark() != null) {
      entity.setRemark(trimToNull(req.getRemark()));
    }
    entity.setSubmittedAt(LocalDateTime.now());
    entity.setUpdatedBy(user.getUserId());
    supervisionMapper.updateById(entity);

    for (TrainFollowSupervisionItemEntity item : items) {
      if (!"fail".equals(item.getResult())) {
        continue;
      }
      FollowIssueCreateReq issueReq = new FollowIssueCreateReq();
      issueReq.setTitle("督导不合格：" + item.getItemTitle());
      issueReq.setDescription(StringUtils.hasText(item.getRemark())
          ? item.getRemark()
          : "督导检查项「" + item.getItemTitle() + "」未通过");
      issueReq.setSeverity("warn");
      issueReq.setSupervisionId(entity.getId());
      createIssueInternal(entity.getClassId(), issueReq, entity.getId(), item.getId(), user, false);
    }

    emitSupervisionOp(entity.getClassId(), FollowOpEventCode.SUPERVISION_SUBMIT, entity,
        "提交督导检查：" + entity.getBizDate() + " · " + conclusionLabel(entity.getConclusion()), "info");
    return getSupervision(id);
  }

  @Transactional
  public FollowSupervisionVO close(Long id) {
    TrainFollowSupervisionEntity entity = requireSupervision(id);
    if (STATUS_CLOSED.equals(entity.getStatus())) {
      return getSupervision(id);
    }
    if (!STATUS_SUBMITTED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已提交的检查单可关闭");
    }
    PlatformUserPrincipal user = SecurityContext.currentUser();
    entity.setStatus(STATUS_CLOSED);
    entity.setUpdatedBy(user.getUserId());
    supervisionMapper.updateById(entity);

    emitSupervisionOp(entity.getClassId(), FollowOpEventCode.SUPERVISION_CLOSE, entity,
        "关闭督导检查：" + entity.getBizDate(), "info");
    return getSupervision(id);
  }

  @Transactional(readOnly = true)
  public List<FollowIssueVO> listIssues(Long classId, String status) {
    requireClass(classId);
    LambdaQueryWrapper<TrainFollowIssueEntity> q = issueQuery()
        .eq(TrainFollowIssueEntity::getClassId, classId)
        .orderByDesc(TrainFollowIssueEntity::getOpenedAt)
        .orderByDesc(TrainFollowIssueEntity::getId);
    if (StringUtils.hasText(status)) {
      q.eq(TrainFollowIssueEntity::getStatus, normalizeIssueStatus(status));
    }
    return issueMapper.selectList(q).stream().map(this::toIssueVo).toList();
  }

  @Transactional
  public FollowIssueVO createIssue(Long classId, FollowIssueCreateReq req) {
    requireClass(classId);
    PlatformUserPrincipal user = SecurityContext.currentUser();
    return createIssueInternal(classId, req, req.getSupervisionId(), null, user, true);
  }

  @Transactional
  public FollowIssueVO transitionIssue(Long id, FollowIssueTransitionReq req) {
    TrainFollowIssueEntity entity = requireIssue(id);
    String from = entity.getStatus();
    String to = normalizeIssueStatus(req.getStatus());
    if (from.equals(to)) {
      return toIssueVo(entity);
    }
    Set<String> allowed = ISSUE_TRANSITIONS.getOrDefault(from, Set.of());
    if (!allowed.contains(to)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "不允许从 " + from + " 变更为 " + to);
    }

    PlatformUserPrincipal user = SecurityContext.currentUser();
    entity.setStatus(to);
    if (req.getResolveNote() != null) {
      entity.setResolveNote(trimToNull(req.getResolveNote()));
    }
    if (req.getAssigneeName() != null) {
      entity.setAssigneeName(trimToNull(req.getAssigneeName()));
    }
    if ("closed".equals(to)) {
      entity.setClosedAt(LocalDateTime.now());
    }
    entity.setUpdatedBy(user.getUserId());
    issueMapper.updateById(entity);

    emitIssueOp(entity.getClassId(), FollowOpEventCode.ISSUE_TRANSITION, entity,
        "整改状态：" + from + " → " + to + " · " + entity.getTitle(),
        "critical".equals(entity.getSeverity()) ? "critical" : "warn");
    return toIssueVo(entity);
  }

  @Transactional(readOnly = true)
  public List<FollowSurveyResponseVO> listSurveys(Long classId) {
    requireClass(classId);
    return surveyMapper.selectList(surveyQuery()
            .eq(TrainFollowSurveyResponseEntity::getClassId, classId)
            .orderByDesc(TrainFollowSurveyResponseEntity::getCreatedAt)
            .orderByDesc(TrainFollowSurveyResponseEntity::getId))
        .stream()
        .map(this::toSurveyVo)
        .toList();
  }

  @Transactional
  public FollowSurveyResponseVO createSurvey(Long classId, FollowSurveyCreateReq req) {
    requireClass(classId);
    BigDecimal score = validateSurveyScore(req.getScore());
    PlatformUserPrincipal user = SecurityContext.currentUser();

    TrainFollowSurveyResponseEntity entity = new TrainFollowSurveyResponseEntity();
    entity.setTenantId(tenantId());
    entity.setClassId(classId);
    entity.setBizDate(StringUtils.hasText(req.getBizDate())
        ? req.getBizDate().trim()
        : LocalDate.now().format(DAY));
    entity.setRespondentName(trimToNull(req.getRespondentName()));
    entity.setScore(score);
    entity.setCommentText(trimToNull(req.getCommentText()));
    entity.setSource("admin");
    entity.setCreatedBy(user.getUserId());
    entity.setUpdatedBy(user.getUserId());
    entity.setDeleted(0);
    surveyMapper.insert(entity);

    emitSurveyOp(classId, entity);
    return toSurveyVo(entity);
  }

  @Transactional(readOnly = true)
  public BigDecimal surveyAvg(Long classId) {
    requireClass(classId);
    List<TrainFollowSurveyResponseEntity> rows = surveyMapper.selectList(
        surveyQuery().eq(TrainFollowSurveyResponseEntity::getClassId, classId));
    if (rows.isEmpty()) {
      return null;
    }
    BigDecimal sum = BigDecimal.ZERO;
    for (TrainFollowSurveyResponseEntity row : rows) {
      if (row.getScore() != null) {
        sum = sum.add(row.getScore());
      }
    }
    return sum.divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);
  }

  private FollowIssueVO createIssueInternal(
      Long classId,
      FollowIssueCreateReq req,
      Long supervisionId,
      Long itemId,
      PlatformUserPrincipal user,
      boolean emitOpen) {
    TrainFollowIssueEntity entity = new TrainFollowIssueEntity();
    entity.setTenantId(tenantId());
    entity.setClassId(classId);
    entity.setSupervisionId(supervisionId);
    entity.setItemId(itemId);
    entity.setTitle(req.getTitle().trim());
    entity.setDescription(trimToNull(req.getDescription()));
    entity.setSeverity(normalizeSeverity(req.getSeverity()));
    entity.setStatus("open");
    entity.setAssigneeName(trimToNull(req.getAssigneeName()));
    entity.setDueDate(trimToNull(req.getDueDate()));
    entity.setOpenedBy(user.getUserId());
    entity.setOpenedAt(LocalDateTime.now());
    entity.setCreatedBy(user.getUserId());
    entity.setUpdatedBy(user.getUserId());
    entity.setDeleted(0);
    issueMapper.insert(entity);

    if (emitOpen) {
      emitIssueOp(classId, FollowOpEventCode.ISSUE_OPEN, entity,
          "开立整改单：" + entity.getTitle(),
          "critical".equals(entity.getSeverity()) ? "critical" : "warn");
    } else {
      emitIssueOp(classId, FollowOpEventCode.ISSUE_OPEN, entity,
          "督导自动生成整改：" + entity.getTitle(), "warn");
    }
    return toIssueVo(entity);
  }

  private FollowSupervisionVO toSupervisionVo(TrainFollowSupervisionEntity entity, boolean withItems) {
    List<FollowSupervisionItemVO> items = withItems
        ? loadItems(entity.getId()).stream().map(this::toItemVo).toList()
        : List.of();
    Long openIssueCount = withItems ? countOpenIssues(entity.getId()) : null;
    return new FollowSupervisionVO(
        entity.getId(),
        entity.getClassId(),
        entity.getBizDate(),
        entity.getScheduleId(),
        entity.getInspectorUserId(),
        entity.getInspectorName(),
        entity.getStatus(),
        entity.getConclusion(),
        entity.getRemark(),
        entity.getSubmittedAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        items,
        openIssueCount);
  }

  private FollowSupervisionItemVO toItemVo(TrainFollowSupervisionItemEntity e) {
    return new FollowSupervisionItemVO(
        e.getId(),
        e.getSupervisionId(),
        e.getItemCode(),
        e.getItemTitle(),
        e.getSortNo(),
        e.getResult(),
        e.getRemark(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private FollowIssueVO toIssueVo(TrainFollowIssueEntity e) {
    return new FollowIssueVO(
        e.getId(),
        e.getClassId(),
        e.getSupervisionId(),
        e.getItemId(),
        e.getTitle(),
        e.getDescription(),
        e.getSeverity(),
        e.getStatus(),
        e.getAssigneeName(),
        e.getDueDate(),
        e.getResolveNote(),
        e.getOpenedBy(),
        e.getOpenedAt(),
        e.getClosedAt(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private FollowSurveyResponseVO toSurveyVo(TrainFollowSurveyResponseEntity e) {
    return new FollowSurveyResponseVO(
        e.getId(),
        e.getClassId(),
        e.getBizDate(),
        e.getRespondentName(),
        e.getScore(),
        e.getCommentText(),
        e.getSource(),
        e.getCreatedAt());
  }

  private List<TrainFollowSupervisionItemEntity> loadItems(Long supervisionId) {
    return itemMapper.selectList(new LambdaQueryWrapper<TrainFollowSupervisionItemEntity>()
        .eq(TrainFollowSupervisionItemEntity::getSupervisionId, supervisionId)
        .eq(TrainFollowSupervisionItemEntity::getTenantId, tenantId())
        .eq(TrainFollowSupervisionItemEntity::getDeleted, 0)
        .orderByAsc(TrainFollowSupervisionItemEntity::getSortNo)
        .orderByAsc(TrainFollowSupervisionItemEntity::getId));
  }

  private long countOpenIssues(Long supervisionId) {
    return issueMapper.selectCount(issueQuery()
        .eq(TrainFollowIssueEntity::getSupervisionId, supervisionId)
        .in(TrainFollowIssueEntity::getStatus, OPEN_ISSUE_STATUSES));
  }

  private TrainFollowSupervisionEntity requireSupervision(Long id) {
    TrainFollowSupervisionEntity entity = supervisionMapper.selectOne(
        supervisionQuery().eq(TrainFollowSupervisionEntity::getId, id));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "督导检查单不存在");
    }
    return entity;
  }

  private TrainFollowSupervisionItemEntity requireItem(Long itemId) {
    TrainFollowSupervisionItemEntity item = itemMapper.selectOne(
        new LambdaQueryWrapper<TrainFollowSupervisionItemEntity>()
            .eq(TrainFollowSupervisionItemEntity::getId, itemId)
            .eq(TrainFollowSupervisionItemEntity::getTenantId, tenantId())
            .eq(TrainFollowSupervisionItemEntity::getDeleted, 0));
    if (item == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "检查项不存在");
    }
    return item;
  }

  private TrainFollowIssueEntity requireIssue(Long id) {
    TrainFollowIssueEntity entity = issueMapper.selectOne(issueQuery().eq(TrainFollowIssueEntity::getId, id));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "整改单不存在");
    }
    return entity;
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

  private LambdaQueryWrapper<TrainFollowSupervisionEntity> supervisionQuery() {
    return new LambdaQueryWrapper<TrainFollowSupervisionEntity>()
        .eq(TrainFollowSupervisionEntity::getTenantId, tenantId())
        .eq(TrainFollowSupervisionEntity::getDeleted, 0);
  }

  private LambdaQueryWrapper<TrainFollowIssueEntity> issueQuery() {
    return new LambdaQueryWrapper<TrainFollowIssueEntity>()
        .eq(TrainFollowIssueEntity::getTenantId, tenantId())
        .eq(TrainFollowIssueEntity::getDeleted, 0);
  }

  private LambdaQueryWrapper<TrainFollowSurveyResponseEntity> surveyQuery() {
    return new LambdaQueryWrapper<TrainFollowSurveyResponseEntity>()
        .eq(TrainFollowSurveyResponseEntity::getTenantId, tenantId())
        .eq(TrainFollowSurveyResponseEntity::getDeleted, 0);
  }

  private void assertDraft(TrainFollowSupervisionEntity entity) {
    if (!STATUS_DRAFT.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿状态可编辑");
    }
  }

  private static String normalizeItemResult(String result) {
    if (!StringUtils.hasText(result) || !ITEM_RESULTS.contains(result.trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "result 须为 pass / fail / na");
    }
    return result.trim();
  }

  private static String normalizeConclusion(String conclusion) {
    if (!StringUtils.hasText(conclusion) || !CONCLUSIONS.contains(conclusion.trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "conclusion 须为 pass / fail / conditional");
    }
    return conclusion.trim();
  }

  private static String normalizeIssueStatus(String status) {
    if (!StringUtils.hasText(status) || !ISSUE_STATUSES.contains(status.trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "status 须为 open / doing / done / closed");
    }
    return status.trim();
  }

  private static String normalizeSeverity(String severity) {
    if (!StringUtils.hasText(severity)) {
      return "info";
    }
    String s = severity.trim();
    if (!ISSUE_SEVERITIES.contains(s)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "severity 须为 info / warn / critical");
    }
    return s;
  }

  private static BigDecimal validateSurveyScore(BigDecimal score) {
    if (score == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "score 不能为空");
    }
    if (score.compareTo(BigDecimal.ONE) < 0 || score.compareTo(BigDecimal.valueOf(5)) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "score 须在 1–5 之间");
    }
    return score.setScale(1, RoundingMode.HALF_UP);
  }

  private Long tenantId() {
    Long t = TenantContextHolder.getTenantId();
    if (t == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少租户");
    }
    return t;
  }

  private static String resolveInspectorName(String override, PlatformUserPrincipal user) {
    if (StringUtils.hasText(override)) {
      return override.trim();
    }
    if (StringUtils.hasText(user.getDisplayName())) {
      return user.getDisplayName();
    }
    return user.getUsername();
  }

  private static String conclusionLabel(String conclusion) {
    if (!StringUtils.hasText(conclusion)) {
      return "—";
    }
    return switch (conclusion) {
      case "pass" -> "通过";
      case "fail" -> "不通过";
      case "conditional" -> "有条件通过";
      default -> conclusion;
    };
  }

  private void emitSupervisionOp(
      Long classId,
      FollowOpEventCode event,
      TrainFollowSupervisionEntity entity,
      String summary,
      String severity) {
    try {
      Map<String, Object> detail = new LinkedHashMap<>();
      detail.put("supervisionId", entity.getId());
      detail.put("bizDate", entity.getBizDate());
      detail.put("status", entity.getStatus());
      detail.put("conclusion", entity.getConclusion());
      opLogWriter.write(FollowOpLogCommand.of(classId, event)
          .tenantId(entity.getTenantId())
          .bizDate(entity.getBizDate())
          .scheduleId(entity.getScheduleId())
          .summary(summary)
          .target("supervision", entity.getId())
          .severity(severity)
          .detail(detail));
    } catch (Exception ex) {
      log.warn("[督导检查] 写过程日志失败 classId={} supervisionId={} err={}",
          classId, entity.getId(), ex.getMessage());
    }
  }

  private void emitIssueOp(
      Long classId,
      FollowOpEventCode event,
      TrainFollowIssueEntity entity,
      String summary,
      String severity) {
    try {
      Map<String, Object> detail = new LinkedHashMap<>();
      detail.put("issueId", entity.getId());
      detail.put("supervisionId", entity.getSupervisionId());
      detail.put("itemId", entity.getItemId());
      detail.put("severity", entity.getSeverity());
      detail.put("status", entity.getStatus());
      opLogWriter.write(FollowOpLogCommand.of(classId, event)
          .tenantId(entity.getTenantId())
          .summary(summary)
          .target("issue", entity.getId())
          .severity(severity)
          .detail(detail));
    } catch (Exception ex) {
      log.warn("[整改单] 写过程日志失败 classId={} issueId={} err={}",
          classId, entity.getId(), ex.getMessage());
    }
  }

  private void emitSurveyOp(Long classId, TrainFollowSurveyResponseEntity entity) {
    try {
      Map<String, Object> detail = new LinkedHashMap<>();
      detail.put("surveyId", entity.getId());
      detail.put("score", entity.getScore());
      detail.put("respondentName", entity.getRespondentName());
      opLogWriter.write(FollowOpLogCommand.of(classId, FollowOpEventCode.SURVEY_SUBMIT)
          .tenantId(entity.getTenantId())
          .bizDate(entity.getBizDate())
          .summary("满意度 " + entity.getScore() + " 分"
              + (StringUtils.hasText(entity.getRespondentName())
              ? " · " + entity.getRespondentName()
              : ""))
          .target("survey", entity.getId())
          .detail(detail));
    } catch (Exception ex) {
      log.warn("[满意度] 写过程日志失败 classId={} surveyId={} err={}",
          classId, entity.getId(), ex.getMessage());
    }
  }

  private static String trimToNull(String s) {
    return StringUtils.hasText(s) ? s.trim() : null;
  }
}
