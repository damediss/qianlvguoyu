package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_simulation_sync_row")
public class CompetitionSimulationSyncRowEntity extends BaseEntity {
  private Long batchId;
  private Integer rowNo;
  private String externalParticipantId;
  private String externalSessionId;
  private String playerName;
  private String idCardMasked;
  private Long contestantMemberId;
  private BigDecimal rawScore;
  private String moduleTitle;
  private String appDisplayName;
  private Integer durationSeconds;
  private LocalDateTime achievedAt;
  private String validationStatus;
  private String errorMessage;
  private Long scoreSourceId;
  private String detailJson;
  private String payloadHash;

  public Long getBatchId() { return batchId; }
  public void setBatchId(Long value) { batchId = value; }
  public Integer getRowNo() { return rowNo; }
  public void setRowNo(Integer value) { rowNo = value; }
  public String getExternalParticipantId() { return externalParticipantId; }
  public void setExternalParticipantId(String value) { externalParticipantId = value; }
  public String getExternalSessionId() { return externalSessionId; }
  public void setExternalSessionId(String value) { externalSessionId = value; }
  public String getPlayerName() { return playerName; }
  public void setPlayerName(String value) { playerName = value; }
  public String getIdCardMasked() { return idCardMasked; }
  public void setIdCardMasked(String value) { idCardMasked = value; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long value) { contestantMemberId = value; }
  public BigDecimal getRawScore() { return rawScore; }
  public void setRawScore(BigDecimal value) { rawScore = value; }
  public String getModuleTitle() { return moduleTitle; }
  public void setModuleTitle(String value) { moduleTitle = value; }
  public String getAppDisplayName() { return appDisplayName; }
  public void setAppDisplayName(String value) { appDisplayName = value; }
  public Integer getDurationSeconds() { return durationSeconds; }
  public void setDurationSeconds(Integer value) { durationSeconds = value; }
  public LocalDateTime getAchievedAt() { return achievedAt; }
  public void setAchievedAt(LocalDateTime value) { achievedAt = value; }
  public String getValidationStatus() { return validationStatus; }
  public void setValidationStatus(String value) { validationStatus = value; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String value) { errorMessage = value; }
  public Long getScoreSourceId() { return scoreSourceId; }
  public void setScoreSourceId(Long value) { scoreSourceId = value; }
  public String getDetailJson() { return detailJson; }
  public void setDetailJson(String value) { detailJson = value; }
  public String getPayloadHash() { return payloadHash; }
  public void setPayloadHash(String value) { payloadHash = value; }
}
