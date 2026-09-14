package com.guoyu.platform.chazheng.support;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 查证批次熔断器：按批次统计「连续接口级失败」次数，达到阈值即熔断整批。
 *
 * <p>内存实现（单实例部署够用）：批次是短生命周期任务，重启后计数清零不影响正确性——
 * 熔断中的批次在 DB 里已是 aborted 状态，重启后不会自动重跑，人工重试时才复位。
 */
public class CertCheckCircuitBreaker {

  private final int threshold;
  /** batchId → 连续接口级失败次数 */
  private final Map<Long, AtomicInteger> streaks = new ConcurrentHashMap<>();
  /** 已熔断的批次 */
  private final Set<Long> tripped = ConcurrentHashMap.newKeySet();

  public CertCheckCircuitBreaker(int threshold) {
    this.threshold = Math.max(threshold, 1);
  }

  public boolean isTripped(long batchId) {
    return tripped.contains(batchId);
  }

  /** 记录一次接口级失败，返回当前连续失败次数 */
  public int recordInterfaceFailure(long batchId) {
    return streaks.computeIfAbsent(batchId, k -> new AtomicInteger()).incrementAndGet();
  }

  /** 任一查证成功 → 接口已恢复，清零连续失败计数（已熔断的批次不复位） */
  public void resetStreak(long batchId) {
    streaks.remove(batchId);
  }

  /** 达阈值则熔断；返回是否本次新熔断（幂等，重复触发返回 false） */
  public boolean tryTrip(long batchId) {
    AtomicInteger streak = streaks.get(batchId);
    if (streak == null || streak.get() < threshold) {
      return false;
    }
    return tripped.add(batchId);
  }

  /** 人工重试批次时整体复位（重跑前必须调用，否则新任务会被熔断拦截） */
  public void reset(long batchId) {
    tripped.remove(batchId);
    streaks.remove(batchId);
  }
}
