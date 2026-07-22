package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_player_sms_challenge")
public class CompetitionPlayerSmsChallengeEntity extends BaseEntity {

  private Long playerId;
  private String purpose;
  private String mobileLookupHash;
  private String codeHash;
  private String status;
  private Integer failedAttempts;
  private LocalDateTime lockedUntil;
  private LocalDateTime expiresAt;
  private LocalDateTime sentAt;
  private LocalDateTime consumedAt;
  private String deliveryStatus;
  private String providerMessageId;
  private String requestIpHash;
  private String requestDeviceHash;

  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public String getPurpose() { return purpose; }
  public void setPurpose(String purpose) { this.purpose = purpose; }
  public String getMobileLookupHash() { return mobileLookupHash; }
  public void setMobileLookupHash(String mobileLookupHash) { this.mobileLookupHash = mobileLookupHash; }
  public String getCodeHash() { return codeHash; }
  public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getFailedAttempts() { return failedAttempts; }
  public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }
  public LocalDateTime getLockedUntil() { return lockedUntil; }
  public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
  public LocalDateTime getExpiresAt() { return expiresAt; }
  public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
  public LocalDateTime getSentAt() { return sentAt; }
  public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
  public LocalDateTime getConsumedAt() { return consumedAt; }
  public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
  public String getDeliveryStatus() { return deliveryStatus; }
  public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
  public String getProviderMessageId() { return providerMessageId; }
  public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
  public String getRequestIpHash() { return requestIpHash; }
  public void setRequestIpHash(String requestIpHash) { this.requestIpHash = requestIpHash; }
  public String getRequestDeviceHash() { return requestDeviceHash; }
  public void setRequestDeviceHash(String requestDeviceHash) { this.requestDeviceHash = requestDeviceHash; }
}
