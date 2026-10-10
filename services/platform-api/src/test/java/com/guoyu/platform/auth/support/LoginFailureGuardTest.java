package com.guoyu.platform.auth.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 修订方案 §3.1：登录失败锁定（Redis 计数 5 次锁 15 分钟）。
 */
class LoginFailureGuardTest {

  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  // 测试用短 TTL，避免依赖真实时钟
  private final LoginFailureGuard guard = new LoginFailureGuard(redis, 5, 60);

  @SuppressWarnings("unchecked")
  private ValueOperations<String, String> stubOps() {
    ValueOperations<String, String> ops = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(ops);
    return ops;
  }

  @Test
  @DisplayName("★ 连续 5 次失败触发锁定")
  void locksAfterMaxFailures() {
    ValueOperations<String, String> ops = stubOps();
    when(ops.increment(any())).thenReturn(1L, 2L, 3L, 4L, 5L);

    assertFalse(guard.recordFailure("t1", "u1").justLocked());
    assertFalse(guard.recordFailure("t1", "u1").justLocked());
    assertFalse(guard.recordFailure("t1", "u1").justLocked());
    assertFalse(guard.recordFailure("t1", "u1").justLocked());
    LoginFailureGuard.FailureOutcome fifth = guard.recordFailure("t1", "u1");
    assertEquals(5L, fifth.count(), "第 5 次失败计数应为 5");
    assertTrue(fifth.justLocked(), "第 5 次失败应触发锁定");
  }

  @Test
  @DisplayName("锁定中：剩余秒数 > 0")
  void reportsRemainingLockSeconds() {
    when(redis.getExpire(any())).thenReturn(300L);
    assertEquals(300L, guard.remainingLockSeconds("t1", "u1"));
  }

  @Test
  @DisplayName("未锁定：剩余秒数为 0（含 -1 / -2 等 Redis 特殊返回值）")
  void reportsZeroWhenNotLocked() {
    when(redis.getExpire(any())).thenReturn(-2L);
    assertEquals(0L, guard.remainingLockSeconds("t1", "u1"));
    when(redis.getExpire(any())).thenReturn(null);
    assertEquals(0L, guard.remainingLockSeconds("t1", "u1"));
  }

  @Test
  @DisplayName("登录成功清零计数")
  void clearsOnSuccess() {
    guard.clear("t1", "u1");
    verify(redis).delete(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("Redis 不可用时不阻断登录（fail-open），仅记录告警")
  void failsOpenWhenRedisDown() {
    when(redis.opsForValue()).thenThrow(new IllegalStateException("redis down"));
    LoginFailureGuard.FailureOutcome outcome = guard.recordFailure("t1", "u1");
    assertEquals(0L, outcome.count(), "Redis 故障时计数应为 0");
    assertFalse(outcome.justLocked(), "Redis 故障不应把登录判定为锁定");
  }

  @Test
  @DisplayName("首次失败即设置 TTL，避免计数永久残留")
  void setsTtlOnFirstFailure() {
    ValueOperations<String, String> ops = stubOps();
    when(ops.increment(any())).thenReturn(1L);
    guard.recordFailure("t1", "u1");
    verify(redis).expire(any(), eq(Duration.ofSeconds(60)));
  }

  @Test
  @DisplayName("未达阈值不额外续期")
  void doesNotExtendTtlBeforeThreshold() {
    ValueOperations<String, String> ops = stubOps();
    when(ops.increment(any())).thenReturn(2L);
    guard.recordFailure("t1", "u1");
    verify(redis, never()).expire(any(), any(Duration.class));
  }
}
