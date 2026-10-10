package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** §10.1 错题三级淘汰状态机：全部迁移路径 */
class WrongCardDomainServiceTest {

  private static final LocalDate D1 = LocalDate.of(2026, 8, 1);
  private static final LocalDate D5 = LocalDate.of(2026, 8, 5);
  private static final LocalDate D9 = LocalDate.of(2026, 8, 9);

  @Test
  void firstWrongCreatesRedCard() {
    WrongCardDomainService.CardTransition t = WrongCardDomainService.onWrong(
        WrongCardDomainService.CardState.none(), D1);
    assertThat(t.toLevel()).isEqualTo("red");
    assertThat(t.star()).isFalse();
    assertThat(t.consecutiveCorrect()).isZero();
  }

  @Test
  void redToYellowNeedsThreeConsecutiveCorrect() {
    WrongCardDomainService.CardState red = state("red", false, 0, null);
    var t1 = WrongCardDomainService.onCorrect(red, D1);
    assertThat(t1.toLevel()).isEqualTo("red");
    assertThat(t1.consecutiveCorrect()).isEqualTo(1);

    var t2 = WrongCardDomainService.onCorrect(state("red", false, 1, null), D1.plusDays(1));
    assertThat(t2.consecutiveCorrect()).isEqualTo(2);

    var t3 = WrongCardDomainService.onCorrect(state("red", false, 2, null), D1.plusDays(2));
    assertThat(t3.toLevel()).isEqualTo("yellow");
    assertThat(t3.consecutiveCorrect()).isZero();
    assertThat(t3.lastCorrectDate()).isEqualTo(D1.plusDays(2));
  }

  @Test
  void redReviewWrongAddsStarAndResetsStreak() {
    var t = WrongCardDomainService.onWrong(state("red", false, 2, D1), D5);
    assertThat(t.toLevel()).isEqualTo("red");
    assertThat(t.star()).isTrue();
    assertThat(t.consecutiveCorrect()).isZero();
  }

  @Test
  void yellowToGreenNeedsTwoCorrectAndGapOverThreeDays() {
    // 第 5 天第 1 次对：仅计数
    var t1 = WrongCardDomainService.onCorrect(state("yellow", false, 0, null), D5);
    assertThat(t1.toLevel()).isEqualTo("yellow");
    assertThat(t1.consecutiveCorrect()).isEqualTo(1);

    // 第 9 天（间隔 4 天 > 3）第 2 次对：移除为绿牌
    var t2 = WrongCardDomainService.onCorrect(state("yellow", false, 1, D5), D9);
    assertThat(t2.toLevel()).isEqualTo("green");
    assertThat(t2.removed()).isTrue();

    // 间隔仅 2 天（≤3）：不满足间隔，仍黄牌
    var t3 = WrongCardDomainService.onCorrect(state("yellow", false, 1, D5), D5.plusDays(2));
    assertThat(t3.toLevel()).isEqualTo("yellow");
    assertThat(t3.removed()).isFalse();
  }

  @Test
  void yellowWrongGoesBackToRedWithStar() {
    var t = WrongCardDomainService.onWrong(state("yellow", false, 1, D1), D5);
    assertThat(t.fromLevel()).isEqualTo("yellow");
    assertThat(t.toLevel()).isEqualTo("red");
    assertThat(t.star()).isTrue();
    assertThat(t.consecutiveCorrect()).isZero();
  }

  @Test
  void interruptWashPullsRecentGreensBackToYellow() {
    // 10 天前移除的绿牌（≤14 天）：回黄牌
    var t1 = WrongCardDomainService.onInterruptWash(
        new WrongCardDomainService.CardState(true, "green", false, 2, D1, true), D5.plusDays(10), 10);
    assertThat(t1.toLevel()).isEqualTo("yellow");
    assertThat(t1.consecutiveCorrect()).isZero();

    // 20 天前移除的绿牌（>14 天）：不动
    var t2 = WrongCardDomainService.onInterruptWash(
        new WrongCardDomainService.CardState(true, "green", false, 2, D1, true), D5.plusDays(20), 20);
    assertThat(t2.toLevel()).isEqualTo("green");

    // 在册红牌：计数清零、牌级不动
    var t3 = WrongCardDomainService.onInterruptWash(state("red", true, 2, D1), D5, -1);
    assertThat(t3.toLevel()).isEqualTo("red");
    assertThat(t3.consecutiveCorrect()).isZero();
  }

  private static WrongCardDomainService.CardState state(String level, boolean star, int streak,
      LocalDate lastCorrect) {
    return new WrongCardDomainService.CardState(true, level, star, streak, lastCorrect, false);
  }
}
