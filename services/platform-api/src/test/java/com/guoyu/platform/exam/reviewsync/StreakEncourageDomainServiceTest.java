package com.guoyu.platform.exam.reviewsync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.reviewsync.service.StreakEncourageDomainService;
import com.guoyu.platform.exam.reviewsync.service.StreakEncourageDomainService.Streak;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 连对/连错判定与档位（D9，方案 §15.3） */
class StreakEncourageDomainServiceTest {

  @Test
  void emptySequenceYieldsZeroCount() {
    Streak streak = StreakEncourageDomainService.computeStreak(List.of());
    assertEquals(0, streak.count());
    assertEquals(0, StreakEncourageDomainService.tierOf(streak));
  }

  @Test
  void trailingCorrectRunIsCountedFromTail() {
    // 错对错对对对 → 尾部连对 3
    Streak streak = StreakEncourageDomainService.computeStreak(List.of(0, 1, 0, 1, 1, 1));
    assertEquals("correct", streak.type());
    assertEquals(3, streak.count());
  }

  @Test
  void trailingWrongRunIsCountedFromTail() {
    Streak streak = StreakEncourageDomainService.computeStreak(List.of(1, 1, 0, 0));
    assertEquals("wrong", streak.type());
    assertEquals(2, streak.count());
  }

  @Test
  void singleAnswerIsStreakOfOne() {
    assertEquals(1, StreakEncourageDomainService.computeStreak(List.of(1)).count());
    assertEquals(1, StreakEncourageDomainService.computeStreak(List.of(0)).count());
  }

  @Test
  void correctTiersPickLargestTierBelowCount() {
    assertEquals(0, StreakEncourageDomainService.tierOf(new Streak("correct", 2)));
    assertEquals(3, StreakEncourageDomainService.tierOf(new Streak("correct", 3)));
    assertEquals(3, StreakEncourageDomainService.tierOf(new Streak("correct", 4)));
    assertEquals(5, StreakEncourageDomainService.tierOf(new Streak("correct", 7)));
    assertEquals(10, StreakEncourageDomainService.tierOf(new Streak("correct", 14)));
    assertEquals(15, StreakEncourageDomainService.tierOf(new Streak("correct", 19)));
    assertEquals(20, StreakEncourageDomainService.tierOf(new Streak("correct", 25)));
  }

  @Test
  void wrongTiersCapAtFour() {
    // W1.5：连错 4 档兜底一次后沉默（5+ 不再打扰）
    assertEquals(0, StreakEncourageDomainService.tierOf(new Streak("wrong", 1)));
    assertEquals(2, StreakEncourageDomainService.tierOf(new Streak("wrong", 2)));
    assertEquals(3, StreakEncourageDomainService.tierOf(new Streak("wrong", 3)));
    assertEquals(4, StreakEncourageDomainService.tierOf(new Streak("wrong", 4)));
    assertEquals(4, StreakEncourageDomainService.tierOf(new Streak("wrong", 6)));
    // 恰好等于档位才触发：连错 4 触发一次，5+ 不重发
    assertTrue(StreakEncourageDomainService.shouldFire(new Streak("wrong", 4), 4));
    assertFalse(StreakEncourageDomainService.shouldFire(new Streak("wrong", 5), 4));
    assertFalse(StreakEncourageDomainService.shouldFire(new Streak("wrong", 3), 4));
  }

  @Test
  void shouldFireOnlyWhenCountExactlyEqualsTier() {
    // 连对 7 时档 5 已发过，不再重发；连对 5 恰好触发
    assertTrue(StreakEncourageDomainService.shouldFire(new Streak("correct", 5), 5));
    assertFalse(StreakEncourageDomainService.shouldFire(new Streak("correct", 7), 5));
    assertTrue(StreakEncourageDomainService.shouldFire(new Streak("wrong", 3), 3));
    assertFalse(StreakEncourageDomainService.shouldFire(new Streak("wrong", 4), 3));
    assertFalse(StreakEncourageDomainService.shouldFire(new Streak("correct", 2), 0));
  }
}
