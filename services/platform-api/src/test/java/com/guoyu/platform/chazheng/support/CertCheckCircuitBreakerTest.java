package com.guoyu.platform.chazheng.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 查证批次熔断器：连续接口级失败达阈值熔断、幂等、成功清零、人工重试复位 */
class CertCheckCircuitBreakerTest {

  @Test
  void tripsAtThresholdOnlyOnce() {
    CertCheckCircuitBreaker cb = new CertCheckCircuitBreaker(5);
    long batch = 1001L;
    for (int i = 0; i < 4; i++) {
      cb.recordInterfaceFailure(batch);
      assertFalse(cb.tryTrip(batch), "未达阈值不应熔断");
      assertFalse(cb.isTripped(batch));
    }
    assertEquals(5, cb.recordInterfaceFailure(batch));
    assertTrue(cb.tryTrip(batch), "达阈值应熔断");
    assertTrue(cb.isTripped(batch));
    assertFalse(cb.tryTrip(batch), "重复熔断应幂等返回 false");
    // 其他批次互不影响
    assertFalse(cb.isTripped(2002L));
  }

  @Test
  void successResetsStreak() {
    CertCheckCircuitBreaker cb = new CertCheckCircuitBreaker(3);
    cb.recordInterfaceFailure(1L);
    cb.recordInterfaceFailure(1L);
    cb.resetStreak(1L); // 接口恢复（有用户查证成功）
    cb.recordInterfaceFailure(1L);
    assertFalse(cb.tryTrip(1L), "清零后需重新累计到达阈值");
  }

  @Test
  void resetClearsTrippedForManualRetry() {
    CertCheckCircuitBreaker cb = new CertCheckCircuitBreaker(2);
    cb.recordInterfaceFailure(7L);
    cb.recordInterfaceFailure(7L);
    assertTrue(cb.tryTrip(7L));
    cb.reset(7L);
    assertFalse(cb.isTripped(7L), "人工重试前必须复位熔断标志");
    assertFalse(cb.tryTrip(7L), "复位后计数清零，未达阈值不熔断");
  }

  @Test
  void thresholdClampedToAtLeastOne() {
    CertCheckCircuitBreaker cb = new CertCheckCircuitBreaker(0);
    cb.recordInterfaceFailure(9L);
    assertTrue(cb.tryTrip(9L), "阈值 0 应钳制为 1");
  }
}
