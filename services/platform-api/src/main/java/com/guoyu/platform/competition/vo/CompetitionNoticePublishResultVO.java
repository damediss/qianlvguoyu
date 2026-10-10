package com.guoyu.platform.competition.vo;

public class CompetitionNoticePublishResultVO {

  private Long noticeId;
  private Long eventId;
  private String beforeStatus;
  private String afterStatus;

  public Long getNoticeId() { return noticeId; }
  public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public String getBeforeStatus() { return beforeStatus; }
  public void setBeforeStatus(String beforeStatus) { this.beforeStatus = beforeStatus; }
  public String getAfterStatus() { return afterStatus; }
  public void setAfterStatus(String afterStatus) { this.afterStatus = afterStatus; }
}
