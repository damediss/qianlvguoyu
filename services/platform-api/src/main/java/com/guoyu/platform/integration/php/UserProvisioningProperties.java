package com.guoyu.platform.integration.php;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 学员开户双写配置（platform.user-provisioning.*）。
 *
 * <p>生产必须 {@code php-write-enabled=true}，否则新建账号仅落 SaaS，App 无法登录。
 */
@Component
@ConfigurationProperties(prefix = "platform.user-provisioning")
public class UserProvisioningProperties {

  /** PHP 来源 id 上限：与 PlatformSyncService.LOCAL_ID_BOUNDARY 一致 */
  public static final long LOCAL_ID_BOUNDARY = 1_000_000_000L;

  /** 是否写入 PHP platform_user（App 登录权威库） */
  private boolean phpWriteEnabled = false;

  /** PHP 单人失败时记 SKIPPED 而不抛错拖垮整班 */
  private boolean failSoft = true;

  public boolean isPhpWriteEnabled() {
    return phpWriteEnabled;
  }

  public void setPhpWriteEnabled(boolean phpWriteEnabled) {
    this.phpWriteEnabled = phpWriteEnabled;
  }

  public boolean isFailSoft() {
    return failSoft;
  }

  public void setFailSoft(boolean failSoft) {
    this.failSoft = failSoft;
  }
}
