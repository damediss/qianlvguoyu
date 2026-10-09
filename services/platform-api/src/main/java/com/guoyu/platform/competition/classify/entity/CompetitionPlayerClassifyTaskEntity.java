package com.guoyu.platform.competition.classify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_player_classify_task")
public class CompetitionPlayerClassifyTaskEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private String stageName;
  private Integer typeCount;
  private Integer playerCount;
  private String status;
  private String typeNamesJson;
  private String selectedProfileIdsJson;
  private String resultSnapshotJson;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public String getStageName() { return stageName; }
  public void setStageName(String stageName) { this.stageName = stageName; }
  public Integer getTypeCount() { return typeCount; }
  public void setTypeCount(Integer typeCount) { this.typeCount = typeCount; }
  public Integer getPlayerCount() { return playerCount; }
  public void setPlayerCount(Integer playerCount) { this.playerCount = playerCount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getTypeNamesJson() { return typeNamesJson; }
  public void setTypeNamesJson(String typeNamesJson) { this.typeNamesJson = typeNamesJson; }
  public String getSelectedProfileIdsJson() { return selectedProfileIdsJson; }
  public void setSelectedProfileIdsJson(String selectedProfileIdsJson) {
    this.selectedProfileIdsJson = selectedProfileIdsJson;
  }
  public String getResultSnapshotJson() { return resultSnapshotJson; }
  public void setResultSnapshotJson(String resultSnapshotJson) {
    this.resultSnapshotJson = resultSnapshotJson;
  }
}
