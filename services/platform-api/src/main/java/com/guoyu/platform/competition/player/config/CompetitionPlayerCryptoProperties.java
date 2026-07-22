package com.guoyu.platform.competition.player.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.competition.player.crypto")
public class CompetitionPlayerCryptoProperties {

  private String aesKeyBase64;
  private String lookupPepper;
  private String keyVersion;

  public String getAesKeyBase64() {
    return aesKeyBase64;
  }

  public void setAesKeyBase64(String aesKeyBase64) {
    this.aesKeyBase64 = aesKeyBase64;
  }

  public String getLookupPepper() {
    return lookupPepper;
  }

  public void setLookupPepper(String lookupPepper) {
    this.lookupPepper = lookupPepper;
  }

  public String getKeyVersion() {
    return keyVersion;
  }

  public void setKeyVersion(String keyVersion) {
    this.keyVersion = keyVersion;
  }
}
