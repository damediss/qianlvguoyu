package com.guoyu.platform.integration.sync;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 三表同步调度器（鉴权方案 §3.5.3：轮询周期 3s；§3.5.5：每日全量对账）。
 *
 * <p>开关：{@code platform.sync.enabled=false} 时整个调度器不装配（测试环境 src/test/resources 已关闭），
 * 生产默认开启。{@code @EnableScheduling} 放在本条件装配内，不影响其他模块的调度语义
 * （与 CompetitionMessagingConfiguration 同一模式）。
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "platform.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformSyncScheduler {

  private final PlatformSyncService syncService;

  public PlatformSyncScheduler(PlatformSyncService syncService) {
    this.syncService = syncService;
  }

  /** 快周期：token 全量对账（踢人敏感）+ user/detail 水位线与指纹比对 */
  @Scheduled(
      fixedDelayString = "${platform.sync.interval-ms:3000}",
      initialDelayString = "${platform.sync.initial-delay-ms:10000}")
  public void runCycle() {
    syncService.runCycleSafely();
  }

  /** 每日全量对账（凌晨 03:30）：物理删除修复 + 漂移大闸 */
  @Scheduled(cron = "${platform.sync.daily-reconcile-cron:0 30 3 * * *}")
  public void reconcileDaily() {
    syncService.reconcileDailySafely();
  }
}
