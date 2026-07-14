package com.guoyu.platform.chazheng.support;

/**
 * 简单 QPS 限流器，按固定间隔放行请求（替代 Guava RateLimiter）。
 */
public final class FixedRateLimiter {

  private final long intervalNanos;
  private long nextPermitNanos;
  private final Object lock = new Object();

  private FixedRateLimiter(double permitsPerSecond) {
    if (permitsPerSecond <= 0) {
      throw new IllegalArgumentException("permitsPerSecond must be positive");
    }
    this.intervalNanos = (long) (1_000_000_000.0 / permitsPerSecond);
    this.nextPermitNanos = System.nanoTime();
  }

  public static FixedRateLimiter create(double permitsPerSecond) {
    return new FixedRateLimiter(permitsPerSecond);
  }

  public void acquire() {
    synchronized (lock) {
      long now = System.nanoTime();
      if (now < nextPermitNanos) {
        long sleepNanos = nextPermitNanos - now;
        try {
          long sleepMs = sleepNanos / 1_000_000;
          int sleepNs = (int) (sleepNanos % 1_000_000);
          if (sleepMs > 0 || sleepNs > 0) {
            Thread.sleep(sleepMs, sleepNs);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        nextPermitNanos += intervalNanos;
      } else {
        nextPermitNanos = now + intervalNanos;
      }
    }
  }
}
