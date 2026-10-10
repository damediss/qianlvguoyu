package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.FollowWorkLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.FollowWorkLogDraftFromOpsReq;
import com.guoyu.platform.trainingdepartment.dto.FollowWorkLogUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowLogEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowOpLogEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.follow.FollowOpEventCode;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogCommand;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogWriter;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowLogMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowOpLogMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.vo.FollowOpLogVO;
import com.guoyu.platform.trainingdepartment.vo.FollowWorkLogVO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainFollowLogApplicationService {

  private final TrainFollowLogMapper workLogMapper;
  private final TrainFollowOpLogMapper opLogMapper;
  private final TrainInfoClassMapper classMapper;
  private final FollowOpLogWriter opLogWriter;
  private final ObjectMapper objectMapper;

  public TrainFollowLogApplicationService(
      TrainFollowLogMapper workLogMapper,
      TrainFollowOpLogMapper opLogMapper,
      TrainInfoClassMapper classMapper,
      FollowOpLogWriter opLogWriter,
      ObjectMapper objectMapper) {
    this.workLogMapper = workLogMapper;
    this.opLogMapper = opLogMapper;
    this.classMapper = classMapper;
    this.opLogWriter = opLogWriter;
    this.objectMapper = objectMapper;
  }

  public List<FollowOpLogVO> listOpLogs(
      Long classId,
      String bizDate,
      String from,
      String to,
      String category,
      Long actorUserId,
      String minSeverity) {
    requireClass(classId);
    LambdaQueryWrapper<TrainFollowOpLogEntity> q = new LambdaQueryWrapper<TrainFollowOpLogEntity>()
        .eq(TrainFollowOpLogEntity::getTenantId, tenantId())
        .eq(TrainFollowOpLogEntity::getClassId, classId)
        .eq(TrainFollowOpLogEntity::getDeleted, 0)
        .orderByDesc(TrainFollowOpLogEntity::getCreatedAt)
        .orderByDesc(TrainFollowOpLogEntity::getId)
        .last("LIMIT 500");
    if (StringUtils.hasText(bizDate)) {
      q.eq(TrainFollowOpLogEntity::getBizDate, bizDate.trim());
    }
    if (StringUtils.hasText(from)) {
      q.ge(TrainFollowOpLogEntity::getBizDate, from.trim());
    }
    if (StringUtils.hasText(to)) {
      q.le(TrainFollowOpLogEntity::getBizDate, to.trim());
    }
    if (StringUtils.hasText(category)) {
      q.eq(TrainFollowOpLogEntity::getEventCategory, category.trim());
    }
    if (actorUserId != null) {
      q.eq(TrainFollowOpLogEntity::getActorUserId, actorUserId);
    }
    if ("warn".equalsIgnoreCase(minSeverity)) {
      q.in(TrainFollowOpLogEntity::getSeverityLevel, List.of("warn", "critical"));
    } else if ("critical".equalsIgnoreCase(minSeverity)) {
      q.eq(TrainFollowOpLogEntity::getSeverityLevel, "critical");
    }
    return opLogMapper.selectList(q).stream().map(this::toOpVo).toList();
  }

  public List<FollowWorkLogVO> listWorkLogs(Long classId, String bizDate, String from, String to) {
    requireClass(classId);
    LambdaQueryWrapper<TrainFollowLogEntity> q = new LambdaQueryWrapper<TrainFollowLogEntity>()
        .eq(TrainFollowLogEntity::getTenantId, tenantId())
        .eq(TrainFollowLogEntity::getClassId, classId)
        .eq(TrainFollowLogEntity::getDeleted, 0)
        .orderByDesc(TrainFollowLogEntity::getBizDate)
        .orderByDesc(TrainFollowLogEntity::getId);
    if (StringUtils.hasText(bizDate)) {
      q.eq(TrainFollowLogEntity::getBizDate, bizDate.trim());
    }
    if (StringUtils.hasText(from)) {
      q.ge(TrainFollowLogEntity::getBizDate, from.trim());
    }
    if (StringUtils.hasText(to)) {
      q.le(TrainFollowLogEntity::getBizDate, to.trim());
    }
    return workLogMapper.selectList(q).stream().map(this::toWorkVo).toList();
  }

  @Transactional
  public FollowWorkLogVO createWorkLog(Long classId, FollowWorkLogCreateReq req) {
    requireClass(classId);
    assertHasTextBody(req.getContent(), req.getAttendanceSummary(), req.getExceptionNote());
    PlatformUserPrincipal user = SecurityContext.currentUser();
    String status = normalizeStatus(req.getStatus(), "published");

    TrainFollowLogEntity entity = new TrainFollowLogEntity();
    entity.setTenantId(tenantId());
    entity.setClassId(classId);
    entity.setScheduleId(req.getScheduleId());
    entity.setBizDate(req.getBizDate().trim());
    entity.setAuthorUserId(user.getUserId());
    entity.setAuthorName(resolveAuthorName(req.getAuthorName(), user));
    entity.setAttendanceSummary(trimToNull(req.getAttendanceSummary()));
    entity.setExceptionNote(trimToNull(req.getExceptionNote()));
    entity.setContent(trimToNull(req.getContent()));
    entity.setSourceOpIdsJson(trimToNull(req.getSourceOpIdsJson()));
    entity.setStatus(status);
    entity.setCreatedBy(user.getUserId());
    entity.setUpdatedBy(user.getUserId());
    entity.setDeleted(0);
    workLogMapper.insert(entity);

    opLogWriter.write(FollowOpLogCommand.of(classId, FollowOpEventCode.WORK_LOG_CREATE)
        .bizDate(entity.getBizDate())
        .scheduleId(entity.getScheduleId())
        .summary(summarizeWorkLog(entity))
        .target("work_log", entity.getId())
        .actor(user.getUserId(), entity.getAuthorName(), null)
        .detail(Map.of("status", status)));

    return toWorkVo(entity);
  }

  @Transactional
  public FollowWorkLogVO updateWorkLog(Long id, FollowWorkLogUpdateReq req) {
    TrainFollowLogEntity entity = requireWorkLog(id);
    PlatformUserPrincipal user = SecurityContext.currentUser();
    if (req.getBizDate() != null) {
      entity.setBizDate(req.getBizDate().trim());
    }
    if (req.getScheduleId() != null) {
      entity.setScheduleId(req.getScheduleId());
    }
    if (req.getAuthorName() != null) {
      entity.setAuthorName(req.getAuthorName().trim());
    }
    if (req.getAttendanceSummary() != null) {
      entity.setAttendanceSummary(trimToNull(req.getAttendanceSummary()));
    }
    if (req.getExceptionNote() != null) {
      entity.setExceptionNote(trimToNull(req.getExceptionNote()));
    }
    if (req.getContent() != null) {
      entity.setContent(trimToNull(req.getContent()));
    }
    if (req.getSourceOpIdsJson() != null) {
      entity.setSourceOpIdsJson(trimToNull(req.getSourceOpIdsJson()));
    }
    if (req.getStatus() != null) {
      entity.setStatus(normalizeStatus(req.getStatus(), entity.getStatus()));
    }
    assertHasTextBody(entity.getContent(), entity.getAttendanceSummary(), entity.getExceptionNote());
    entity.setUpdatedBy(user.getUserId());
    workLogMapper.updateById(entity);

    opLogWriter.write(FollowOpLogCommand.of(entity.getClassId(), FollowOpEventCode.WORK_LOG_UPDATE)
        .bizDate(entity.getBizDate())
        .scheduleId(entity.getScheduleId())
        .summary(summarizeWorkLog(entity))
        .target("work_log", entity.getId())
        .actor(user.getUserId(), entity.getAuthorName(), null));

    return toWorkVo(entity);
  }

  @Transactional
  public void deleteWorkLog(Long id) {
    TrainFollowLogEntity entity = requireWorkLog(id);
    PlatformUserPrincipal user = SecurityContext.currentUser();
    entity.setDeleted(1);
    entity.setUpdatedBy(user.getUserId());
    workLogMapper.updateById(entity);

    opLogWriter.write(FollowOpLogCommand.of(entity.getClassId(), FollowOpEventCode.WORK_LOG_DELETE)
        .bizDate(entity.getBizDate())
        .scheduleId(entity.getScheduleId())
        .summary("删除工作日志：" + entity.getBizDate())
        .target("work_log", entity.getId())
        .severity("warn")
        .actor(user.getUserId(), resolveAuthorName(null, user), null));
  }

  @Transactional
  public FollowWorkLogVO draftFromOps(Long classId, FollowWorkLogDraftFromOpsReq req) {
    requireClass(classId);
    String day = req.getBizDate().trim();
    List<TrainFollowOpLogEntity> ops = opLogMapper.selectList(
        new LambdaQueryWrapper<TrainFollowOpLogEntity>()
            .eq(TrainFollowOpLogEntity::getTenantId, tenantId())
            .eq(TrainFollowOpLogEntity::getClassId, classId)
            .eq(TrainFollowOpLogEntity::getBizDate, day)
            .eq(TrainFollowOpLogEntity::getDeleted, 0)
            .orderByAsc(TrainFollowOpLogEntity::getCreatedAt)
            .last("LIMIT 200"));

    PlatformUserPrincipal user = SecurityContext.currentUser();
    StringBuilder content = new StringBuilder();
    content.append("【据 ").append(day).append(" 过程动态自动生成草稿】\n");
    if (ops.isEmpty()) {
      content.append("当日暂无过程动态，请手工补充出勤概况与异常处理。");
    } else {
      for (TrainFollowOpLogEntity op : ops) {
        content.append("- ")
            .append(op.getCreatedAt() != null ? op.getCreatedAt().toLocalTime() : "")
            .append(' ')
            .append(StringUtils.hasText(op.getActorName()) ? op.getActorName() : "系统")
            .append(' ')
            .append(op.getTitle());
        if (StringUtils.hasText(op.getSummary())) {
          content.append('：').append(op.getSummary());
        }
        content.append('\n');
      }
    }

    List<Long> ids = ops.stream().map(TrainFollowOpLogEntity::getId).filter(Objects::nonNull).toList();
    String sourceJson;
    try {
      sourceJson = objectMapper.writeValueAsString(ids);
    } catch (Exception ex) {
      sourceJson = null;
    }

    long warnCount = ops.stream()
        .filter(o -> "warn".equals(o.getSeverityLevel()) || "critical".equals(o.getSeverityLevel()))
        .count();
    String attendance = "过程动态共 " + ops.size() + " 条"
        + (warnCount > 0 ? "，其中异常/警示 " + warnCount + " 条" : "");

    TrainFollowLogEntity entity = new TrainFollowLogEntity();
    entity.setTenantId(tenantId());
    entity.setClassId(classId);
    entity.setBizDate(day);
    entity.setAuthorUserId(user.getUserId());
    entity.setAuthorName(resolveAuthorName(null, user));
    entity.setAttendanceSummary(attendance);
    entity.setContent(content.toString().trim());
    entity.setSourceOpIdsJson(sourceJson);
    entity.setStatus("draft");
    entity.setCreatedBy(user.getUserId());
    entity.setUpdatedBy(user.getUserId());
    entity.setDeleted(0);
    workLogMapper.insert(entity);

    // 草稿不单独打 create 业务事件（避免时间线噪音）；正式发布走 create/update
    return toWorkVo(entity);
  }

  private FollowOpLogVO toOpVo(TrainFollowOpLogEntity e) {
    return new FollowOpLogVO(
        e.getId(),
        e.getClassId(),
        e.getScheduleId(),
        e.getBizDate(),
        e.getEventCode(),
        e.getEventCategory(),
        e.getTitle(),
        e.getSummary(),
        e.getDetailJson(),
        e.getActorUserId(),
        e.getActorName(),
        e.getActorRole(),
        e.getTargetType(),
        e.getTargetId(),
        e.getSeverityLevel(),
        e.getClient(),
        e.getRequestId(),
        e.getCreatedAt());
  }

  private FollowWorkLogVO toWorkVo(TrainFollowLogEntity e) {
    return new FollowWorkLogVO(
        e.getId(),
        e.getClassId(),
        e.getScheduleId(),
        e.getBizDate(),
        e.getAuthorUserId(),
        e.getAuthorName(),
        e.getAttendanceSummary(),
        e.getExceptionNote(),
        e.getContent(),
        e.getSourceOpIdsJson(),
        e.getStatus(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private TrainFollowLogEntity requireWorkLog(Long id) {
    TrainFollowLogEntity entity = workLogMapper.selectOne(
        new LambdaQueryWrapper<TrainFollowLogEntity>()
            .eq(TrainFollowLogEntity::getId, id)
            .eq(TrainFollowLogEntity::getTenantId, tenantId())
            .eq(TrainFollowLogEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "工作日志不存在");
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

  private Long tenantId() {
    Long t = TenantContextHolder.getTenantId();
    if (t == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少租户");
    }
    return t;
  }

  private static void assertHasTextBody(String content, String attendance, String exceptionNote) {
    if (!StringUtils.hasText(content)
        && !StringUtils.hasText(attendance)
        && !StringUtils.hasText(exceptionNote)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写内容、出勤概况或异常说明至少一项");
    }
  }

  private static String normalizeStatus(String status, String fallback) {
    if (!StringUtils.hasText(status)) {
      return fallback;
    }
    String s = status.trim();
    if (!"draft".equals(s) && !"published".equals(s)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "status 须为 draft 或 published");
    }
    return s;
  }

  private static String resolveAuthorName(String override, PlatformUserPrincipal user) {
    if (StringUtils.hasText(override)) {
      return override.trim();
    }
    if (StringUtils.hasText(user.getDisplayName())) {
      return user.getDisplayName();
    }
    return user.getUsername();
  }

  private static String summarizeWorkLog(TrainFollowLogEntity e) {
    if (StringUtils.hasText(e.getContent())) {
      String c = e.getContent().trim();
      return c.length() > 80 ? c.substring(0, 80) + "…" : c;
    }
    if (StringUtils.hasText(e.getAttendanceSummary())) {
      return e.getAttendanceSummary();
    }
    return e.getBizDate() + " 工作日志";
  }

  private static String trimToNull(String s) {
    return StringUtils.hasText(s) ? s.trim() : null;
  }
}
