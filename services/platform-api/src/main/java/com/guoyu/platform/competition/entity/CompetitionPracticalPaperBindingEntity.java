package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_practical_paper_binding")
public class CompetitionPracticalPaperBindingEntity extends BaseEntity {
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long paperSnapshotId;

  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long value) { paperSnapshotId = value; }
}
