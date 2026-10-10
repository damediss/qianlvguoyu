package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

/**
 * Isolated competition-player login subject.
 *
 * The database column names are retained for compatibility, but the two legacy ciphertext columns
 * now contain the normalized plain ID card and mobile values by product decision.
 */
@TableName("competition_player_identity")
public class CompetitionPlayerIdentityEntity extends BaseEntity {

  @TableField("id_card_ciphertext")
  private String idCardNo;
  private String idCardLookupHash;
  @TableField("mobile_ciphertext")
  private String mobile;
  private String mobileLookupHash;
  /** Legacy NOT NULL column retained for database compatibility; no encryption version is used. */
  private String cryptoKeyVersion;
  private String passwordHash;
  private LocalDateTime passwordInitializedAt;
  private String status;
  private LocalDateTime activatedAt;
  private LocalDateTime disabledAt;
  private String disabledReason;

  public String getIdCardNo() { return idCardNo; }
  public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
  public String getIdCardLookupHash() { return idCardLookupHash; }
  public void setIdCardLookupHash(String idCardLookupHash) { this.idCardLookupHash = idCardLookupHash; }
  public String getMobile() { return mobile; }
  public void setMobile(String mobile) { this.mobile = mobile; }
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
