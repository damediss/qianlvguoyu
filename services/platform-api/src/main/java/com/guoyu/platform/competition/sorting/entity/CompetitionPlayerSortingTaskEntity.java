package com.guoyu.platform.competition.sorting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_practical_sorting_task")
public class CompetitionPlayerSortingTaskEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private String stageName;
  private Integer groupCount;
  private Integer workstationCount;
  private Integer playersPerGroup;
  private Integer playerCount;
  private String status;
  private String selectedProfileIdsJson;
  private String resultSnapshotJson;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public String getStageName() { return stageName; }
  public void setStageName(String stageName) { this.stageName = stageName; }
  public Integer getGroupCount() { return groupCount; }
  public void setGroupCount(Integer groupCount) { this.groupCount = groupCount; }
  public Integer getWorkstationCount() { return workstationCount; }
  public void setWorkstationCount(Integer workstationCount) {
    this.workstationCount = workstationCount;
  }
  public Integer getPlayersPerGroup() { return playersPerGroup; }
  public void setPlayersPerGroup(Integer playersPerGroup) {
    this.playersPerGroup = playersPerGroup;
  }
  public Integer getPlayerCount() { return playerCount; }
  public void setPlayerCount(Integer playerCount) { this.playerCount = playerCount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getSelectedProfileIdsJson() { return selectedProfileIdsJson; }
  public void setSelectedProfileIdsJson(String selectedProfileIdsJson) {
    this.selectedProfileIdsJson = selectedProfileIdsJson;
  }
  public String getResultSnapshotJson() { return resultSnapshotJson; }
  public void setResultSnapshotJson(String resultSnapshotJson) {
    this.resultSnapshotJson = resultSnapshotJson;
  }
}
