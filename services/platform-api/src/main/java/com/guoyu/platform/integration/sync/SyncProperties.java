package com.guoyu.platform.integration.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 三表同步任务配置（platform.sync.*）。
 *
 * <p>开关语义：{@code platform.sync.enabled=false} 时调度器不加载（测试环境默认关闭），
 * 但数据源/服务 Bean 仍在，便于手动触发与排障。
 */
@Component
@ConfigurationProperties(prefix = "platform.sync")
public class SyncProperties {

  /** 同步任务总开关 */
  private boolean enabled = true;

  /** 快周期间隔（毫秒），默认 3 秒 */
  private long intervalMs = 3000;

  /** 启动后首次执行延迟（毫秒），避开应用启动窗口 */
  private long initialDelayMs = 10_000;

  /** 水位线快路径单批拉取上限 */
  private int batchSize = 500;

  /** 连续失败告警阈值（次），默认 20 周期 ≈ 1 分钟 */
  private int failAlertThreshold = 20;

  /** 试运行：只读 PHP 库并记录将发生的变更，不写 SaaS 镜像、不推进水位线 */
  private boolean dryRun = false;

  /** 每日全量对账 cron，默认 03:30 */
  private String dailyReconcileCron = "0 30 3 * * *";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getIntervalMs() {
    return intervalMs;
  }

  public void setIntervalMs(long intervalMs) {
    this.intervalMs = intervalMs;
  }

  public long getInitialDelayMs() {
    return initialDelayMs;
  }

  public void setInitialDelayMs(long initialDelayMs) {
    this.initialDelayMs = initialDelayMs;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public int getFailAlertThreshold() {
    return failAlertThreshold;
  }

  public void setFailAlertThreshold(int failAlertThreshold) {
    this.failAlertThreshold = failAlertThreshold;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  public String getDailyReconcileCron() {
    return dailyReconcileCron;
  }

  public void setDailyReconcileCron(String dailyReconcileCron) {
    this.dailyReconcileCron = dailyReconcileCron;
  }
}
