package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.ScheduleChangeRejectReq;
import com.guoyu.platform.trainingdepartment.dto.ScheduleChangeSubmitReq;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleChangeRequestEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleChangeRequestMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainScheduleChangeRequestVO;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 课表调课申请（App 提交 → 管理端审批）—— 《App课表与调课审批实现方案》S2。
 */
@Service
@Slf4j
public class TrainScheduleChangeRequestApplicationService {

  public static final String STATUS_PENDING = "pending";
  public static final String STATUS_APPROVED = "approved";
  public static final String STATUS_REJECTED = "rejected";
  public static final String STATUS_CANCELLED = "cancelled";

  public static final String TYPE_RESCHEDULE = "reschedule";
  public static final String TYPE_REPLACE_TEACHER = "replace_teacher";
  public static final String TYPE_CANCEL = "cancel";
  public static final String TYPE_ADD = "add";

  private static final Set<String> SUPPORTED_TYPES =
      Set.of(TYPE_RESCHEDULE, TYPE_REPLACE_TEACHER, TYPE_CANCEL, TYPE_ADD);

  private static final Map<String, String> TYPE_NAMES = Map.of(
      TYPE_RESCHEDULE, "改时间/地点",
      TYPE_REPLACE_TEACHER, "换老师",
      TYPE_CANCEL, "停课",
      TYPE_ADD, "加课");

  private static final Map<String, String> STATUS_NAMES = Map.of(
      STATUS_PENDING, "待审批",
      STATUS_APPROVED, "已通过",
      STATUS_REJECTED, "已驳回",
      STATUS_CANCELLED, "已撤销");

  private final TrainScheduleChangeRequestMapper requestMapper;
  private final TrainScheduleMapper scheduleMapper;
  private final TrainInfoClassMapper classMapper;
  private final TrainClassRoleApplicationService roleService;
  private final TrainScheduleApplicationService scheduleService;
  private final ObjectMapper objectMapper;

  public TrainScheduleChangeRequestApplicationService(
      TrainScheduleChangeRequestMapper requestMapper,
      TrainScheduleMapper scheduleMapper,
      TrainInfoClassMapper classMapper,
      TrainClassRoleApplicationService roleService,
      TrainScheduleApplicationService scheduleService,
      ObjectMapper objectMapper) {
    this.requestMapper = requestMapper;
    this.scheduleMapper = scheduleMapper;
    this.classMapper = classMapper;
    this.roleService = roleService;
    this.scheduleService = scheduleService;
    this.objectMapper = objectMapper;
  }

  /** App：提交调课申请 */
  @Transactional
  public TrainScheduleChangeRequestVO submit(Long platformUserId, Long classId, ScheduleChangeSubmitReq req) {
    if (classId == null || classId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "classId 无效");
    }
    roleService.assertClassMember(platformUserId, classId);
    String applicantRole = roleService.requireAnyActiveRole(
        platformUserId,
        classId,
        TrainClassRoleApplicationService.LEAD_TEACHER,
        TrainClassRoleApplicationService.ASSISTANT,
        TrainClassRoleApplicationService.HEAD_TEACHER);

    String changeType = normalizeType(req.getChangeType());
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = resolveTenantId(clazz);

    TrainScheduleEntity schedule = null;
    if (!TYPE_ADD.equals(changeType)) {
      if (req.getScheduleId() == null || req.getScheduleId() <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要调整的课次");
      }
      schedule = requireScheduleOfClass(req.getScheduleId(), classId, tenantId);
      assertNoPendingForSchedule(schedule.getId());
    }

    Map<String, Object> after = buildAfterPayload(changeType, req, schedule);
    validateAfterPayload(changeType, after);

    TrainScheduleChangeRequestEntity entity = new TrainScheduleChangeRequestEntity();
    entity.setClassId(classId);
    entity.setScheduleId(schedule != null ? schedule.getId() : null);
    entity.setChangeType(changeType);
    entity.setBeforeSnapshot(schedule != null ? toJson(snapshotOf(schedule)) : null);
    entity.setAfterPayload(toJson(after));
    entity.setReason(trimToNull(req.getReason()));
    entity.setStatus(STATUS_PENDING);
    entity.setApplicantPlatformUserId(platformUserId);
    entity.setApplicantRoleCode(applicantRole);
    entity.setTenantId(tenantId);
    entity.setCreatedBy(platformUserId);
    entity.setUpdatedBy(platformUserId);
    entity.setDeleted(0);
    requestMapper.insert(entity);
    return toVO(entity);
  }

  /** App：我的申请列表 */
  @Transactional(readOnly = true)
  public List<TrainScheduleChangeRequestVO> listMine(Long platformUserId, Long classId) {
    roleService.assertClassMember(platformUserId, classId);
    return requestMapper.selectList(new LambdaQueryWrapper<TrainScheduleChangeRequestEntity>()
            .eq(TrainScheduleChangeRequestEntity::getClassId, classId)
            .eq(TrainScheduleChangeRequestEntity::getApplicantPlatformUserId, platformUserId)
            .eq(TrainScheduleChangeRequestEntity::getDeleted, 0)
            .orderByDesc(TrainScheduleChangeRequestEntity::getCreatedAt))
        .stream()
        .map(this::toVO)
        .toList();
  }

  /** App：撤销 pending */
  @Transactional
  public TrainScheduleChangeRequestVO cancel(Long platformUserId, Long requestId) {
    TrainScheduleChangeRequestEntity entity = requireRequest(requestId);
    if (!Objects.equals(entity.getApplicantPlatformUserId(), platformUserId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "只能撤销本人的申请");
    }
    if (!STATUS_PENDING.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待审批申请可撤销");
    }
    entity.setStatus(STATUS_CANCELLED);
    entity.setUpdatedBy(platformUserId);
    requestMapper.updateById(entity);
    return toVO(entity);
  }

  /** 管理端：列表 */
  @Transactional(readOnly = true)
  public List<TrainScheduleChangeRequestVO> listAdmin(Long classId, String status) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainScheduleChangeRequestEntity> wrapper =
        new LambdaQueryWrapper<TrainScheduleChangeRequestEntity>()
            .eq(TrainScheduleChangeRequestEntity::getTenantId, tenantId)
            .eq(TrainScheduleChangeRequestEntity::getDeleted, 0)
            .orderByDesc(TrainScheduleChangeRequestEntity::getCreatedAt);
    if (classId != null && classId > 0) {
      wrapper.eq(TrainScheduleChangeRequestEntity::getClassId, classId);
    }
    if (StringUtils.hasText(status)) {
      wrapper.eq(TrainScheduleChangeRequestEntity::getStatus, status.trim());
    }
    return requestMapper.selectList(wrapper).stream().map(this::toVO).toList();
  }

  /** 管理端：详情 */
  @Transactional(readOnly = true)
  public TrainScheduleChangeRequestVO getAdmin(Long requestId) {
    TrainScheduleChangeRequestEntity entity = requireRequest(requestId);
    assertTenant(entity);
    return toVO(entity);
  }

  /** 管理端：通过并应用 */
  @Transactional
  public TrainScheduleChangeRequestVO approve(Long requestId, boolean force) {
    TrainScheduleChangeRequestEntity entity = requireRequest(requestId);
    assertTenant(entity);
    if (!STATUS_PENDING.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待审批申请可通过");
    }
    Long reviewerId = SecurityContext.currentUser().getUserId();
    scheduleService.applyChangeRequest(entity, force, reviewerId);

    entity.setStatus(STATUS_APPROVED);
    entity.setReviewerUserId(reviewerId);
    entity.setReviewedAt(LocalDateTime.now());
    entity.setUpdatedBy(reviewerId);
    requestMapper.updateById(entity);
    return toVO(entity);
  }

  /** 管理端：驳回 */
  @Transactional
  public TrainScheduleChangeRequestVO reject(Long requestId, ScheduleChangeRejectReq req) {
    TrainScheduleChangeRequestEntity entity = requireRequest(requestId);
    assertTenant(entity);
    if (!STATUS_PENDING.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待审批申请可驳回");
    }
    Long reviewerId = SecurityContext.currentUser().getUserId();
    entity.setStatus(STATUS_REJECTED);
    entity.setRejectReason(req != null ? trimToNull(req.getRejectReason()) : null);
    entity.setReviewerUserId(reviewerId);
    entity.setReviewedAt(LocalDateTime.now());
    entity.setUpdatedBy(reviewerId);
    requestMapper.updateById(entity);
    return toVO(entity);
  }

  private void assertNoPendingForSchedule(Long scheduleId) {
    long pending = requestMapper.selectCount(new LambdaQueryWrapper<TrainScheduleChangeRequestEntity>()
        .eq(TrainScheduleChangeRequestEntity::getScheduleId, scheduleId)
        .eq(TrainScheduleChangeRequestEntity::getStatus, STATUS_PENDING)
        .eq(TrainScheduleChangeRequestEntity::getDeleted, 0));
    if (pending > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该课次已有待审批的调课申请，请等待处理或先撤销");
    }
  }

  private Map<String, Object> buildAfterPayload(
      String changeType, ScheduleChangeSubmitReq req, TrainScheduleEntity schedule) {
    Map<String, Object> after = new LinkedHashMap<>();
    if (TYPE_CANCEL.equals(changeType)) {
      after.put("cancelled", true);
      return after;
    }
    if (TYPE_ADD.equals(changeType)) {
      putIfHasText(after, "scheduleDate", req.getScheduleDate());
      putIfHasText(after, "startTime", req.getStartTime());
      putIfHasText(after, "endTime", req.getEndTime());
      putIfHasText(after, "roomName", req.getRoomName());
      putIfHasText(after, "teacherName", req.getTeacherName());
      putIfHasText(after, "courseName", req.getCourseName());
      if (req.getTeacherId() != null) {
        after.put("teacherId", req.getTeacherId());
      }
      if (req.getDayOfWeek() != null) {
        after.put("dayOfWeek", req.getDayOfWeek());
      }
      return after;
    }
    // reschedule / replace_teacher：未传字段沿用原值写入 after，便于审批对照
    if (schedule != null) {
      after.put("scheduleDate", schedule.getScheduleDate());
      after.put("startTime", schedule.getStartTime());
      after.put("endTime", schedule.getEndTime());
      after.put("roomName", schedule.getRoomName());
      after.put("teacherName", schedule.getTeacherName());
      after.put("teacherId", schedule.getTeacherId());
      after.put("courseName", schedule.getCourseName());
      after.put("dayOfWeek", schedule.getDayOfWeek());
      after.put("classHours", schedule.getClassHours());
      after.put("teachForm", schedule.getTeachForm());
    }
    if (TYPE_RESCHEDULE.equals(changeType)) {
      if (StringUtils.hasText(req.getScheduleDate())) {
        after.put("scheduleDate", req.getScheduleDate().trim());
      }
      if (StringUtils.hasText(req.getStartTime())) {
        after.put("startTime", req.getStartTime().trim());
      }
      if (StringUtils.hasText(req.getEndTime())) {
        after.put("endTime", req.getEndTime().trim());
      }
      if (req.getRoomName() != null) {
        after.put("roomName", trimToNull(req.getRoomName()));
      }
      if (req.getDayOfWeek() != null) {
        after.put("dayOfWeek", req.getDayOfWeek());
      }
      if (StringUtils.hasText(req.getCourseName())) {
        after.put("courseName", req.getCourseName().trim());
      }
    }
    if (TYPE_REPLACE_TEACHER.equals(changeType)) {
      if (!StringUtils.hasText(req.getTeacherName()) && req.getTeacherId() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写更换后的老师");
      }
      if (StringUtils.hasText(req.getTeacherName())) {
        after.put("teacherName", req.getTeacherName().trim());
      }
      if (req.getTeacherId() != null) {
        after.put("teacherId", req.getTeacherId());
      }
    }
    return after;
  }

  private void validateAfterPayload(String changeType, Map<String, Object> after) {
    if (TYPE_CANCEL.equals(changeType)) {
      return;
    }
    if (TYPE_RESCHEDULE.equals(changeType) || TYPE_ADD.equals(changeType)) {
      if (!StringUtils.hasText(str(after.get("scheduleDate")))) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写上课日期");
      }
      String start = str(after.get("startTime"));
      String end = str(after.get("endTime"));
      if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写上课起止时间");
      }
      if (start.compareTo(end) >= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间须晚于开始时间");
      }
    }
  }

  private TrainScheduleEntity requireScheduleOfClass(Long scheduleId, Long classId, Long tenantId) {
    TrainScheduleEntity s = scheduleMapper.selectById(scheduleId);
    if (s == null || Objects.equals(s.getDeleted(), 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "课次不存在");
    }
    if (!Objects.equals(s.getClassId(), classId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "课次不属于本班");
    }
    if (tenantId != null && tenantId > 0 && s.getTenantId() != null
        && !Objects.equals(s.getTenantId(), tenantId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作该课次");
    }
    return s;
  }

  private TrainScheduleChangeRequestEntity requireRequest(Long requestId) {
    if (requestId == null || requestId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "申请 ID 无效");
    }
    TrainScheduleChangeRequestEntity entity = requestMapper.selectById(requestId);
    if (entity == null || Objects.equals(entity.getDeleted(), 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "调课申请不存在");
    }
    return entity;
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    if (clazz == null || Objects.equals(clazz.getDeleted(), 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    return clazz;
  }

  private void assertTenant(TrainScheduleChangeRequestEntity entity) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    if (tenantId != null && tenantId > 0
        && entity.getTenantId() != null
        && !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权限查看该申请");
    }
  }

  private Long resolveTenantId(TrainInfoClassEntity clazz) {
    Long fromCtx = TenantContextHolder.getTenantId();
    if (fromCtx != null && fromCtx > 0) {
      return fromCtx;
    }
    try {
      Long fromUser = SecurityContext.currentUser().getTenantId();
      if (fromUser != null && fromUser > 0) {
        return fromUser;
      }
    } catch (Exception ignored) {
      // App principal 可能无租户
    }
    // 回退：取本班已有课次的租户
    if (clazz != null && clazz.getId() != null) {
      TrainScheduleEntity one = scheduleMapper.selectOne(new LambdaQueryWrapper<TrainScheduleEntity>()
          .eq(TrainScheduleEntity::getClassId, clazz.getId())
          .eq(TrainScheduleEntity::getDeleted, 0)
          .last("LIMIT 1"));
      if (one != null && one.getTenantId() != null && one.getTenantId() > 0) {
        return one.getTenantId();
      }
    }
    return 1L;
  }

  private String normalizeType(String changeType) {
    if (!StringUtils.hasText(changeType) || !SUPPORTED_TYPES.contains(changeType.trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的变更类型: " + changeType);
    }
    return changeType.trim();
  }

  private Map<String, Object> snapshotOf(TrainScheduleEntity s) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", s.getId());
    m.put("classId", s.getClassId());
    m.put("courseId", s.getCourseId());
    m.put("courseName", s.getCourseName());
    m.put("classHours", s.getClassHours());
    m.put("teachForm", s.getTeachForm());
    m.put("teacherId", s.getTeacherId());
    m.put("teacherName", s.getTeacherName());
    m.put("roomName", s.getRoomName());
    m.put("scheduleDate", s.getScheduleDate());
    m.put("dayOfWeek", s.getDayOfWeek());
    m.put("startTime", s.getStartTime());
    m.put("endTime", s.getEndTime());
    m.put("status", s.getStatus());
    return m;
  }

  private TrainScheduleChangeRequestVO toVO(TrainScheduleChangeRequestEntity e) {
    return new TrainScheduleChangeRequestVO(
        e.getId() == null ? null : String.valueOf(e.getId()),
        e.getClassId(),
        e.getScheduleId(),
        e.getChangeType(),
        TYPE_NAMES.getOrDefault(e.getChangeType(), e.getChangeType()),
        e.getBeforeSnapshot(),
        e.getAfterPayload(),
        e.getReason(),
        e.getStatus(),
        STATUS_NAMES.getOrDefault(e.getStatus(), e.getStatus()),
        e.getApplicantPlatformUserId(),
        e.getApplicantRoleCode(),
        e.getReviewerUserId(),
        e.getReviewedAt(),
        e.getRejectReason(),
        e.getCreatedAt());
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "序列化失败");
    }
  }

  private static void putIfHasText(Map<String, Object> map, String key, String value) {
    if (StringUtils.hasText(value)) {
      map.put(key, value.trim());
    }
  }

  private static String trimToNull(String s) {
    if (!StringUtils.hasText(s)) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String str(Object o) {
    return o == null ? null : String.valueOf(o);
  }
}
