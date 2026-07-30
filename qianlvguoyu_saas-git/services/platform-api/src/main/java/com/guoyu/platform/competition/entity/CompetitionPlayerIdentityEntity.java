package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

/** Isolated competition-player login subject; sensitive values are application-layer ciphertexts. */
@TableName("competition_player_identity")
public class CompetitionPlayerIdentityEntity extends BaseEntity {

  private String idCardCiphertext;
  private String idCardLookupHash;
  private String mobileCiphertext;
  private String mobileLookupHash;
  private String cryptoKeyVersion;
  private String passwordHash;
  private LocalDateTime passwordInitializedAt;
  private String status;
  private LocalDateTime activatedAt;
  private LocalDateTime disabledAt;
  private String disabledReason;

  public String getIdCardCiphertext() { return idCardCiphertext; }
  public void setIdCardCiphertext(String idCardCiphertext) { this.idCardCiphertext = idCardCiphertext; }
  public String getIdCardLookupHash() { return idCardLookupHash; }
  public void setIdCardLookupHash(String idCardLookupHash) { this.idCardLookupHash = idCardLookupHash; }
  public String getMobileCiphertext() { return mobileCiphertext; }
  public void setMobileCiphertext(String mobileCiphertext) { this.mobileCiphertext = mobileCiphertext; }
  public String getMobileLookupHash() { return mobileLookupHash; }
  public void setMobileLookupHash(String mobileLookupHash) { this.mobileLookupHash = mobileLookupHash; }
  public String getCryptoKeyVersion() { return cryptoKeyVersion; }
  public void setCryptoKeyVersion(String cryptoKeyVersion) { this.cryptoKeyVersion = cryptoKeyVersion; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public LocalDateTime getPasswordInitializedAt() { return passwordInitializedAt; }
  public void setPasswordInitializedAt(LocalDateTime passwordInitializedAt) { this.passwordInitializedAt = passwordInitializedAt; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getActivatedAt() { return activatedAt; }
  public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }
  public LocalDateTime getDisabledAt() { return disabledAt; }
  public void setDisabledAt(LocalDateTime disabledAt) { this.disabledAt = disabledAt; }
  public String getDisabledReason() { return disabledReason; }
  public void setDisabledReason(String disabledReason) { this.disabledReason = disabledReason; }
}
