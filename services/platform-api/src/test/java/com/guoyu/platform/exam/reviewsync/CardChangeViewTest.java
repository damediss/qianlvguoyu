package com.guoyu.platform.exam.reviewsync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.guoyu.platform.exam.reviewsync.service.ReviewPlanApplicationService;
import com.guoyu.platform.exam.reviewsync.service.WrongCardDomainService;
import com.guoyu.platform.exam.reviewsync.service.WrongCardDomainService.CardTransition;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * cardChange 视图组装回归（2026-08-11 缺陷：已加星红牌清算区答对，旧 star 状态被当成
 * 新变化下发，前端误报"加星标记（顽固盲区）"）。
 *
 * <p>口径：starNew=本次作答新打上星；答错恒回 cardChange（次数刷新/AI 讲解入口），
 * 答对仅牌级变化才回。
 */
class CardChangeViewTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 11);

  @Test
  void correctOnStarredRedNoLevelChangeReturnsNull() {
    // 加星红牌答对（连对计数 1/3，牌级不变）：不得下发 cardChange —— 否则误报"加星标记"
    WrongCardDomainService.CardState starredRed =
        new WrongCardDomainService.CardState(true, "red", true, 1, null, false);
    CardTransition transition = WrongCardDomainService.onCorrect(starredRed, TODAY);
    boolean starNew = transition.star() && !starredRed.star();
    assertEquals(false, starNew, "已加星红牌答对不是新加星");
    assertNull(ReviewPlanApplicationService.buildCardChange(transition, starNew, true));
  }

  @Test
  void correctWithLevelChangeReturnsChangeWithoutStarNew() {
    // 红牌连续 3 对升黄牌：回 cardChange 展示"红牌 → 黄牌"，starNew=0
    WrongCardDomainService.CardState red =
        new WrongCardDomainService.CardState(true, "red", false, 2, null, false);
    CardTransition transition = WrongCardDomainService.onCorrect(red, TODAY);
    boolean starNew = transition.star() && !red.star();
    Map<String, Object> change = ReviewPlanApplicationService.buildCardChange(transition, starNew, true);
    assertEquals("red", change.get("from"));
    assertEquals("yellow", change.get("to"));
    assertEquals(0, change.get("starNew"));
  }

  @Test
  void wrongOnUnstarredRedSetsStarNew() {
    // 无星红牌复盘再错：本次新加星 → starNew=1 + AI 讲解入口
    WrongCardDomainService.CardState red =
        new WrongCardDomainService.CardState(true, "red", false, 0, null, false);
    CardTransition transition = WrongCardDomainService.onWrong(red, TODAY);
    boolean starNew = transition.star() && !red.star();
    Map<String, Object> change = ReviewPlanApplicationService.buildCardChange(transition, starNew, false);
    assertEquals(1, change.get("starNew"));
    assertEquals(Boolean.TRUE, change.get("explainAvailable"));
  }

  @Test
  void wrongOnStarredRedKeepsCardChangeButNotStarNew() {
    // 已加星红牌复盘再错：仍回 cardChange（已错次数/AI 讲解），但 starNew=0 不再重复提示加星
    WrongCardDomainService.CardState starredRed =
        new WrongCardDomainService.CardState(true, "red", true, 0, null, false);
    CardTransition transition = WrongCardDomainService.onWrong(starredRed, TODAY);
    boolean starNew = transition.star() && !starredRed.star();
    Map<String, Object> change = ReviewPlanApplicationService.buildCardChange(transition, starNew, false);
    assertEquals(0, change.get("starNew"));
    assertEquals(Boolean.TRUE, change.get("explainAvailable"));
  }

  @Test
  void firstWrongNewCardReturnsChange() {
    // 首次答错新入册：from=null → red，starNew=0（无星）
    CardTransition transition = WrongCardDomainService.onWrong(
        WrongCardDomainService.CardState.none(), TODAY);
    Map<String, Object> change = ReviewPlanApplicationService.buildCardChange(transition, false, false);
    assertNull(change.get("from"));
    assertEquals("red", change.get("to"));
    assertEquals(0, change.get("starNew"));
  }
}
