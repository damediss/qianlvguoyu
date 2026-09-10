package com.guoyu.platform.competition.mapper;

import java.time.LocalDateTime;

public class CompetitionPlayerSessionConflictRow {

  private String source;
  private String eventName;
  private String workTypeName;
  private String sessionName;
  private LocalDateTime startAt;
  private LocalDateTime endAt;

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public String getWorkTypeName() {
    return workTypeName;
  }

  public void setWorkTypeName(String workTypeName) {
    this.workTypeName = workTypeName;
  }

  public String getSessionName() {
    return sessionName;
  }

  public void setSessionName(String sessionName) {
    this.sessionName = sessionName;
  }

  public LocalDateTime getStartAt() {
    return startAt;
  }

  public void setStartAt(LocalDateTime startAt) {
    this.startAt = startAt;
  }

  public LocalDateTime getEndAt() {
    return endAt;
  }

  public void setEndAt(LocalDateTime endAt) {
    this.endAt = endAt;
  }
}
