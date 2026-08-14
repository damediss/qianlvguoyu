package com.guoyu.platform.trainingdepartment.follow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.common.web.TraceIdHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowOpLogEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowOpLogMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 跟班过程操作日志写入器。须在业务成功路径、同一事务内调用。
 */
@Component
public class FollowOpLogWriter {

  private static final int MAX_DETAIL_JSON = 8000;
  private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

  private final TrainFollowOpLogMapper opLogMapper;
  private final ObjectMapper objectMapper;

  public FollowOpLogWriter(TrainFollowOpLogMapper opLogMapper, ObjectMapper objectMapper) {
    this.opLogMapper = opLogMapper;
    this.objectMapper = objectMapper;
  }

  public void write(FollowOpLogCommand cmd) {
    if (cmd == null || cmd.getClassId() == null || cmd.getEvent() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "过程日志缺少 classId/event");
    }
    Long tenantId = resolveTenantId(cmd);

    Long actorId = cmd.getActorUserId();
    String actorName = cmd.getActorName();
    String actorRole = cmd.getActorRole();
    if (actorId == null || !StringUtils.hasText(actorName)) {
      try {
        PlatformUserPrincipal p = SecurityContext.currentUser();
        if (actorId == null) {
          actorId = p.getUserId();
        }
        if (!StringUtils.hasText(actorName)) {
          actorName = StringUtils.hasText(p.getDisplayName()) ? p.getDisplayName() : p.getUsername();
        }
      } catch (Exception ignored) {
        // keep nulls
      }
    }

    TrainFollowOpLogEntity row = new TrainFollowOpLogEntity();
    row.setTenantId(tenantId);
    row.setClassId(cmd.getClassId());
    row.setScheduleId(cmd.getScheduleId());
    row.setSessionId(cmd.getSessionId());
    row.setBizDate(StringUtils.hasText(cmd.getBizDate()) ? cmd.getBizDate().trim() : today());
    row.setEventCode(cmd.getEvent().code());
    row.setEventCategory(cmd.getEvent().category());
    row.setTitle(StringUtils.hasText(cmd.getTitle()) ? trim(cmd.getTitle(), 200) : cmd.getEvent().defaultTitle());
    row.setSummary(trim(cmd.getSummary(), 500));
    row.setDetailJson(toDetailJson(cmd.getDetail()));
    row.setActorUserId(actorId);
    row.setActorName(trim(actorName, 64));
    row.setActorRole(trim(actorRole, 32));
    row.setTargetType(trim(cmd.getTargetType(), 64));
    row.setTargetId(cmd.getTargetId());
    row.setSeverityLevel(StringUtils.hasText(cmd.getSeverityLevel()) ? cmd.getSeverityLevel() : "info");
    row.setClient(StringUtils.hasText(cmd.getClient()) ? cmd.getClient() : "admin");
    row.setRequestId(TraceIdHolder.getTraceId());
    row.setCreatedBy(actorId);
    row.setUpdatedBy(actorId);
    row.setDeleted(0);
    opLogMapper.insert(row);
  }

  /** 显式 tenant → 上下文 → 登录用户 → 默认 1（App 跟班常落租户 1） */
  private Long resolveTenantId(FollowOpLogCommand cmd) {
    if (cmd.getTenantId() != null && cmd.getTenantId() > 0) {
      return cmd.getTenantId();
    }
    Long tenantId = TenantContextHolder.getTenantId();
    if (tenantId != null && tenantId > 0) {
      return tenantId;
    }
    try {
      Long tid = SecurityContext.currentUser().getTenantId();
      if (tid != null && tid > 0) {
        return tid;
      }
    } catch (Exception ignored) {
      // leave null
    }
    return 1L;
  }

  private String toDetailJson(Map<String, Object> detail) {
    if (detail == null || detail.isEmpty()) {
      return null;
    }
    try {
      String json = objectMapper.writeValueAsString(detail);
      if (json.length() > MAX_DETAIL_JSON) {
        return json.substring(0, MAX_DETAIL_JSON);
      }
      return json;
    } catch (Exception ex) {
      return null;
    }
  }

  private static String today() {
    return LocalDate.now().format(DAY);
  }

  private static String trim(String s, int max) {
    if (!StringUtils.hasText(s)) {
      return null;
    }
    String t = s.trim();
    return t.length() <= max ? t : t.substring(0, max);
  }
}
