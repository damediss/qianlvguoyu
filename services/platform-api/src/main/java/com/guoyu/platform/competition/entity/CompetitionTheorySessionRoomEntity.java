package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_theory_session_room")
public class CompetitionTheorySessionRoomEntity extends BaseEntity {

  private Long scheduleVersionId;
  private Long sessionId;
  private Long roomId;
  private String roomNameSnapshot;
  private String addressSnapshot;
  private Integer capacitySnapshot;
  private Integer sortOrder;

  public Long getScheduleVersionId() { return scheduleVersionId; }
  public void setScheduleVersionId(Long scheduleVersionId) { this.scheduleVersionId = scheduleVersionId; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public Long getRoomId() { return roomId; }
  public void setRoomId(Long roomId) { this.roomId = roomId; }
  public String getRoomNameSnapshot() { return roomNameSnapshot; }
  public void setRoomNameSnapshot(String roomNameSnapshot) { this.roomNameSnapshot = roomNameSnapshot; }
  public String getAddressSnapshot() { return addressSnapshot; }
  public void setAddressSnapshot(String addressSnapshot) { this.addressSnapshot = addressSnapshot; }
  public Integer getCapacitySnapshot() { return capacitySnapshot; }
  public void setCapacitySnapshot(Integer capacitySnapshot) { this.capacitySnapshot = capacitySnapshot; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
