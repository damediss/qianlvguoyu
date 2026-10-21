package com.guoyu.platform.competition.draw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_player_draw_assignment")
public class CompetitionPlayerDrawAssignmentEntity extends BaseEntity {

  private Long taskId;
  private Long eventId;
  private Long stageId;
  private Long contestantProfileId;
  private Long playerId;
  private String contestantNo;
  private String playerName;
  private String orgName;
  private String drawStatus;
  private Integer appearanceOrder;
  private LocalDateTime drawnAt;

  public Long getTaskId() { return taskId; }
  public void setTaskId(Long taskId) { this.taskId = taskId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getContestantProfileId() { return contestantProfileId; }
  public void setContestantProfileId(Long contestantProfileId) {
    this.contestantProfileId = contestantProfileId;
  }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public String getContestantNo() { return contestantNo; }
  public void setContestantNo(String contestantNo) { this.contestantNo = contestantNo; }
  public String getPlayerName() { return playerName; }
  public void setPlayerName(String playerName) { this.playerName = playerName; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public String getDrawStatus() { return drawStatus; }
  public void setDrawStatus(String drawStatus) { this.drawStatus = drawStatus; }
  public Integer getAppearanceOrder() { return appearanceOrder; }
  public void setAppearanceOrder(Integer appearanceOrder) { this.appearanceOrder = appearanceOrder; }
  public LocalDateTime getDrawnAt() { return drawnAt; }
  public void setDrawnAt(LocalDateTime drawnAt) { this.drawnAt = drawnAt; }
}
