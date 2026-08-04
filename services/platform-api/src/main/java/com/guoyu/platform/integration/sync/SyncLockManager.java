package com.guoyu.platform.integration.sync;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 同步任务防重叠锁（鉴权方案 §3.5.3：Redis {@code SET NX PX=10000}，单实例可降级本地锁）。
 *
 * <p>双层语义：Redis 可用时跨实例互斥；Redis 不可用（AGENTS.md：Redis 非硬依赖）时
 * 降级为本地 {@link AtomicBoolean}（单实例部署足够）。任何 Redis 异常都不阻断同步任务。
 */
@Slf4j
@Component
public class SyncLockManager {

  private static final String LOCK_KEY = "platform:sync:auth-tables:lock";
  private static final Duration LOCK_TTL = Duration.ofSeconds(10);

  private final AtomicBoolean localRunning = new AtomicBoolean(false);
  private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

  public SyncLockManager(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
    this.redisTemplateProvider = redisTemplateProvider;
  }

  /** @return true 获得锁（本周期可执行）；false 已有其他执行中 */
  public boolean tryLock() {
    if (!localRunning.compareAndSet(false, true)) {
      return false;
    }
    StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
    if (redis == null) {
      return true; // 无 Redis：本地锁即全部保障
    }
    try {
      Boolean acquired = redis.opsForValue().setIfAbsent(LOCK_KEY, String.valueOf(System.currentTimeMillis()), LOCK_TTL);
      if (Boolean.FALSE.equals(acquired)) {
        localRunning.set(false);
        return false;
      }
    } catch (Exception e) {
      // Redis 连接失败：降级本地锁，不影响单实例运行
      log.warn("[SYNC] Redis 锁不可用，降级为本地锁: {}", e.getMessage());
    }
    return true;
  }

  public void unlock() {
    try {
      StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
      if (redis != null) {
        redis.delete(LOCK_KEY);
      }
    } catch (Exception e) {
      log.warn("[SYNC] 释放 Redis 锁失败（TTL 10s 自动过期）: {}", e.getMessage());
    } finally {
      localRunning.set(false);
    }
  }
}
