package com.guoyu.platform.competition.player.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.competition.player.sms")
public class CompetitionPlayerSmsProperties {

  private int codeTtlSeconds = 300;
  private int resendCooldownSeconds = 60;
  private int dailyLimitPerMobile = 5;
  private int dailyLimitPerPlayer = 5;
  private int maxFailedAttempts = 5;
  private int lockSeconds = 1800;
  /**
   * Local-only activation code used with the fake SMS sender. It is ignored whenever the
   * Aliyun sender is enabled, so a production SMS cannot accidentally become predictable.
   */
  private String developmentTestCode;
  private final Aliyun aliyun = new Aliyun();

  public int getCodeTtlSeconds() { return codeTtlSeconds; }
  public void setCodeTtlSeconds(int codeTtlSeconds) { this.codeTtlSeconds = codeTtlSeconds; }
  public int getResendCooldownSeconds() { return resendCooldownSeconds; }
  public void setResendCooldownSeconds(int resendCooldownSeconds) { this.resendCooldownSeconds = resendCooldownSeconds; }
  public int getDailyLimitPerMobile() { return dailyLimitPerMobile; }
  public void setDailyLimitPerMobile(int dailyLimitPerMobile) { this.dailyLimitPerMobile = dailyLimitPerMobile; }
  public int getDailyLimitPerPlayer() { return dailyLimitPerPlayer; }
  public void setDailyLimitPerPlayer(int dailyLimitPerPlayer) { this.dailyLimitPerPlayer = dailyLimitPerPlayer; }
  public int getMaxFailedAttempts() { return maxFailedAttempts; }
  public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }
  public int getLockSeconds() { return lockSeconds; }
  public void setLockSeconds(int lockSeconds) { this.lockSeconds = lockSeconds; }
  public String getDevelopmentTestCode() { return developmentTestCode; }
  public void setDevelopmentTestCode(String developmentTestCode) {
    this.developmentTestCode = developmentTestCode;
  }
  public Aliyun getAliyun() { return aliyun; }

  public static class Aliyun {
    private boolean enabled;
    private String endpoint = "https://dysmsapi.aliyuncs.com";
    private String regionId = "cn-hangzhou";
    private String signName;
    private String templateCode;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public String getSignName() { return signName; }
    public void setSignName(String signName) { this.signName = signName; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
  }
}
