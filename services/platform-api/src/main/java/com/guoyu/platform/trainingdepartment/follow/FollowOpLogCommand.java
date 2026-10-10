package com.guoyu.platform.trainingdepartment.follow;

import java.util.Map;

/** 写入过程操作日志的命令对象 */
public class FollowOpLogCommand {

  private Long tenantId;
  private Long classId;
  private Long scheduleId;
  private Long sessionId;
  private String bizDate;
  private FollowOpEventCode event;
  private String title;
  private String summary;
  private Map<String, Object> detail;
  private Long actorUserId;
  private String actorName;
  private String actorRole;
  private String targetType;
  private Long targetId;
  private String severityLevel;
  private String client;

  public static FollowOpLogCommand of(Long classId, FollowOpEventCode event) {
    FollowOpLogCommand c = new FollowOpLogCommand();
    c.classId = classId;
    c.event = event;
    c.title = event.defaultTitle();
    c.severityLevel = "info";
    c.client = "admin";
    return c;
  }

  public FollowOpLogCommand tenantId(Long tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  public FollowOpLogCommand bizDate(String bizDate) {
    this.bizDate = bizDate;
    return this;
  }

  public FollowOpLogCommand scheduleId(Long scheduleId) {
    this.scheduleId = scheduleId;
    return this;
  }

  public FollowOpLogCommand sessionId(Long sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  public FollowOpLogCommand title(String title) {
    this.title = title;
    return this;
  }

  public FollowOpLogCommand summary(String summary) {
    this.summary = summary;
    return this;
  }

  public FollowOpLogCommand detail(Map<String, Object> detail) {
    this.detail = detail;
    return this;
  }

  public FollowOpLogCommand actor(Long userId, String name, String role) {
    this.actorUserId = userId;
    this.actorName = name;
    this.actorRole = role;
    return this;
  }

  public FollowOpLogCommand target(String type, Long id) {
    this.targetType = type;
    this.targetId = id;
    return this;
  }

  public FollowOpLogCommand severity(String level) {
    this.severityLevel = level;
    return this;
  }

  public FollowOpLogCommand client(String client) {
    this.client = client;
    return this;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public Long getClassId() {
    return classId;
  }

  public Long getScheduleId() {
    return scheduleId;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public String getBizDate() {
    return bizDate;
  }

  public FollowOpEventCode getEvent() {
    return event;
  }

  public String getTitle() {
    return title;
  }

  public String getSummary() {
    return summary;
  }

  public Map<String, Object> getDetail() {
    return detail;
  }

  public Long getActorUserId() {
    return actorUserId;
  }

  public String getActorName() {
    return actorName;
  }

  public String getActorRole() {
    return actorRole;
  }

  public String getTargetType() {
    return targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public String getSeverityLevel() {
    return severityLevel;
  }

  public String getClient() {
    return client;
  }
}
